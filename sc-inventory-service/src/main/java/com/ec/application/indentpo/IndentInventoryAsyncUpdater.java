package com.ec.application.indentpo;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.HistoryRelationType;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.InwardActionType;
import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.InwardSyncFailureRepo;
import com.ec.application.repository.PurchaseOrderRepo;
import com.ec.application.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class IndentInventoryAsyncUpdater {

    private final IndentInventoryListRepo indentInventoryListRepo;
    private final IndentCompletionEvaluator indentCompletionEvaluator;
    private final PurchaseOrderCompletionEvaluator purchaseOrderCompletionEvaluator;
    private final PurchaseOrderRepo purchaseOrderRepo;
    private final IndentInventoryRepo indentInventoryRepo;
    private final InwardSyncFailureService inwardSyncFailureService;
    private final IndentStatusHistoryService indentStatusHistoryService;
    private final PurchaseOrderStatusHistoryService purchaseOrderStatusHistoryService;

    private static final Logger log = LoggerFactory.getLogger(IndentInventoryAsyncUpdater.class);

    // ============================================================
    // ASYNC ENTRY POINT (DTO-DRIVEN)
    // ============================================================
    @Async
    @UseDefaultTenant
    public void updateIndentAfterInwardAsync(IndentInwardSyncDTO dto, String action) throws JsonProcessingException {

        String tenantSchema = dto.getTenantSchema();
        Long inwardId = dto.getInwardId();
        InwardActionType actionType = dto.getActionType();

        try {
            log.info("[ASYNC-START] tenant={} inwardId={} action={} deltaCount={}", tenantSchema, inwardId, actionType, dto.getDeltas().size());
            doSyncIndentAndPO(dto);
            log.info("[ASYNC-SUCCESS] tenant={} inwardId={} action={}", tenantSchema, inwardId, actionType);
        } catch (Exception ex) {
            log.error("[ASYNC-FAILURE] tenant={} inwardId={} action={}", tenantSchema, inwardId, actionType, ex);
            inwardSyncFailureService.recordFailure(dto, ex);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    private Set<String> extractLineItemCodes(IndentInwardSyncDTO dto) {
        Set<String> codes = new HashSet<String>();
        for (IndentInwardDeltaDTO d : dto.getDeltas()) {
            codes.add(d.getLineItemCode());
        }
        return codes;
    }

    // ============================================================
    // TENANT-SCOPED TRANSACTIONAL WORKER
    // ============================================================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @UseDefaultTenant
    protected void doSyncIndentAndPO(IndentInwardSyncDTO dto) {

        if (ThreadLocalStorage.getTenantName() == null) {
            throw new IllegalStateException("Tenant not set in async worker");
        }

        Set<String> affectedIndentIds = new HashSet<String>();
        Set<String> affectedPoNumbers = new HashSet<String>();

        for (IndentInwardDeltaDTO delta : dto.getDeltas()) {

            String lineItemCode = delta.getLineItemCode();
            Double qtyDelta = delta.getFinalQuantity();

            log.info(
                    "[ASYNC-LINE] lineItemCode={} delta={}",
                    lineItemCode,
                    qtyDelta
            );

            List<IndentInventoryList> rows = indentInventoryListRepo.findByLineItemCode(lineItemCode);

            if (rows.isEmpty()) {
                log.warn("[ASYNC-SKIP] No indent line found for lineItemCode={}", lineItemCode);
                continue;
            }

            IndentInventoryList indentLine = rows.get(0);
            String oldStatus = indentLine.getLineItemStatus();
            // ------------------------------------------------
            // APPLY DELTA TO INWARD ENTRIES
            // ------------------------------------------------
            upsertAbsoluteQuantity(indentLine, dto.getInwardId(), delta.getFinalQuantity(), dto.getInwardDate());

            // ------------------------------------------------
            // RECALCULATE DERIVED FIELDS
            // ------------------------------------------------
            recalculateIndentLine(indentLine);
            String newStatus = indentLine.getLineItemStatus();
            if (!oldStatus.equals(newStatus)) {
                log.info("[ASYNC-STATUS-CHANGE] lineItemCode={} oldStatus={} newStatus={}", lineItemCode, oldStatus, newStatus);
                indentStatusHistoryService.logStatusChange(
                        indentLine.getIndentInventory(), null, null,
                        "System", "Indent line item " + indentLine.getLineItemCode() + " status changed to " +
                                newStatus + " due to inward sync for " + dto.getTenantSchema() + " inward ID " + dto.getInwardId() + "."
                        , null
                );
            }
            indentInventoryListRepo.save(indentLine);

            // Track affected parents
            affectedIndentIds.add(indentLine.getIndentInventory().getIndentId());

            if (indentLine.getPurchaseOrderId() != null) {
                affectedPoNumbers.add(indentLine.getPurchaseOrderId());
            }
        }

        // ------------------------------------------------
        // RE-EVALUATE INDENTS
        // ------------------------------------------------
        for (String indentId : affectedIndentIds) {
            Optional<IndentInventory> indentOpt = indentInventoryRepo.findByIdWithDetails(indentId);
            if (!indentOpt.isPresent()) {
                continue;
            }
            if (dto.getActionType().equals(InwardActionType.CREATE)) {
                indentStatusHistoryService.logStatusChange(indentOpt.get(), null, null, "System", "New inward entry for " + dto.getTenantSchema() + " inward ID " + dto.getInwardId() + ".", Collections.singletonList(new HistoryRelationInput(HistoryRelationType.INWARD, dto.getTenantSchema(), String.valueOf(dto.getInwardId()))));
            } else if (dto.getActionType().equals(InwardActionType.UPDATE)) {
                indentStatusHistoryService.logStatusChange(indentOpt.get(), null, null, "System", "Inward entry updated for " + dto.getTenantSchema() + " inward ID " + dto.getInwardId() + ".", Collections.singletonList(new HistoryRelationInput(HistoryRelationType.INWARD, dto.getTenantSchema(), String.valueOf(dto.getInwardId()))));
            } else if (dto.getActionType().equals(InwardActionType.DELETE)) {
                indentStatusHistoryService.logStatusChange(indentOpt.get(), null, null, "System", "Inward entry deleted for " + dto.getTenantSchema() + " inward ID " + dto.getInwardId() + ".", null);
            }
            indentCompletionEvaluator.evaluate(indentOpt.get());
        }

        // ------------------------------------------------
        // RE-EVALUATE PURCHASE ORDERS
        // ------------------------------------------------
        for (String poNumber : affectedPoNumbers) {
            Optional<PurchaseOrder> poOpt = purchaseOrderRepo.findByIdWithDetails(poNumber);
            if (!poOpt.isPresent()) {
                continue;
            }
            if (dto.getActionType().equals(InwardActionType.CREATE)) {
                purchaseOrderStatusHistoryService.logStatusChange(poOpt.get(), null, null, "System", "Inward entry CREATED for " + dto.getTenantSchema() + ". Inward ID - " + dto.getInwardId() + ".", Collections.singletonList(new HistoryRelationInput(HistoryRelationType.INWARD, dto.getTenantSchema(), String.valueOf(dto.getInwardId()))));
            } else if (dto.getActionType().equals(InwardActionType.UPDATE)) {
                purchaseOrderStatusHistoryService.logStatusChange(poOpt.get(), null, null, "System", "Inward entry UPDATED for " + dto.getTenantSchema() + ". Inward ID - " + dto.getInwardId() + ".", Collections.singletonList(new HistoryRelationInput(HistoryRelationType.INWARD, dto.getTenantSchema(), String.valueOf(dto.getInwardId()))));
            } else if (dto.getActionType().equals(InwardActionType.DELETE)) {
                purchaseOrderStatusHistoryService.logStatusChange(poOpt.get(), null, null, "System", "Inward entry DELETED for " + dto.getTenantSchema() + ". Inward ID - " + dto.getInwardId() + ".", null);
            }
            purchaseOrderCompletionEvaluator.evaluate(poOpt.get());
        }
    }

    // ============================================================
    // APPLY DELTA LOGIC (IDEMPOTENT)
    // ============================================================
    private void upsertAbsoluteQuantity(
            IndentInventoryList indentLine,
            Long inwardId,
            Double finalQty,
            Date inwardDate
    ) {

        IndentInwardEntry existing = null;

        for (IndentInwardEntry entry : indentLine.getInwardEntries()) {
            if (inwardId.equals(entry.getInwardId())) {
                existing = entry;
                break;
            }
        }

        // DELETE case
        if (finalQty == null || finalQty <= 0.0) {
            if (existing != null) {
                indentLine.getInwardEntries().remove(existing);
            }
            return;
        }

        // CREATE / UPDATE
        if (existing != null) {
            existing.setQuantity(finalQty);
            existing.setInwardDate(inwardDate);
        } else {
            indentLine.getInwardEntries().add(
                    new IndentInwardEntry(inwardId, inwardDate, finalQty)
            );
        }
    }

    // ============================================================
    // DERIVED FIELD RECALCULATION
    // ============================================================
    private void recalculateIndentLine(IndentInventoryList indentLine) {

        double orderedQty = indentLine.getQuantity() == null ? 0.0 : indentLine.getQuantity();
        double received = 0.0;

        for (IndentInwardEntry entry : indentLine.getInwardEntries()) {
            if (entry.getQuantity() != null) {
                received += entry.getQuantity();
            }
        }

        double pending = orderedQty - received;
        if (pending < 0) {
            pending = 0.0;
        }

        indentLine.setQuantityReceived(received);
        indentLine.setQuantityPending(pending);

        if (received == 0.0) {
            indentLine.setLineItemStatus(
                    IndentLineItemStatusConstants.STATUS_NEW
            );
        } else if (pending == 0.0) {
            indentLine.setLineItemStatus(
                    IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE
            );
        } else {
            indentLine.setLineItemStatus(
                    IndentLineItemStatusConstants.STATUS_INWARD_PARTIAL
            );
        }
    }
}
