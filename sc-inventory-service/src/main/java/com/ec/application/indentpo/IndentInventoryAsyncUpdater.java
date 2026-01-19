package com.ec.application.indentpo;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.InwardActionType;
import com.ec.application.data.IndentInwardDeltaDTO;
import com.ec.application.data.IndentInwardSyncDTO;
import com.ec.application.data.InwardLineSnapshot;
import com.ec.application.data.InwardSnapshot;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.InwardSyncFailureRepo;
import com.ec.application.repository.PurchaseOrderRepo;
import com.ec.application.service.CategoryService;
import com.ec.application.service.InwardInventoryService;
import com.ec.application.service.InwardSyncFailureService;
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
public class IndentInventoryAsyncUpdater {

    private final IndentInventoryListRepo indentInventoryListRepo;
    private final IndentCompletionEvaluator indentCompletionEvaluator;
    private final PurchaseOrderCompletionEvaluator purchaseOrderCompletionEvaluator;
    private final PurchaseOrderRepo purchaseOrderRepo;
    private final IndentInventoryRepo indentInventoryRepo;
    private final InwardSyncFailureService inwardSyncFailureService;

    private static final Logger log =
            LoggerFactory.getLogger(IndentInventoryAsyncUpdater.class);

    // ============================================================
    // ASYNC ENTRY POINT (DTO-DRIVEN)
    // ============================================================
    @Async
    public void updateIndentAfterInwardAsync(IndentInwardSyncDTO dto) throws JsonProcessingException {

        String tenantSchema = dto.getTenantSchema();
        Long inwardId = dto.getInwardId();
        InwardActionType actionType = dto.getActionType();

        try {
            ThreadLocalStorage.setTenantName(tenantSchema);

            log.info(
                    "[ASYNC-START] tenant={} inwardId={} action={} deltaCount={}",
                    tenantSchema,
                    inwardId,
                    actionType,
                    dto.getDeltas().size()
            );

            doSyncIndentAndPO(dto);

            log.info(
                    "[ASYNC-SUCCESS] tenant={} inwardId={} action={}",
                    tenantSchema,
                    inwardId,
                    actionType
            );
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
    protected void doSyncIndentAndPO(IndentInwardSyncDTO dto) {

        if (ThreadLocalStorage.getTenantName() == null) {
            throw new IllegalStateException("Tenant not set in async worker");
        }

        Set<String> affectedIndentIds = new HashSet<String>();
        Set<String> affectedPoNumbers = new HashSet<String>();

        for (IndentInwardDeltaDTO delta : dto.getDeltas()) {

            String lineItemCode = delta.getLineItemCode();
            Double qtyDelta = delta.getQuantityDelta();

            log.info(
                    "[ASYNC-LINE] lineItemCode={} delta={}",
                    lineItemCode,
                    qtyDelta
            );

            List<IndentInventoryList> rows =
                    indentInventoryListRepo.findByLineItemCode(lineItemCode);

            if (rows.isEmpty()) {
                log.warn(
                        "[ASYNC-SKIP] No indent line found for lineItemCode={}",
                        lineItemCode
                );
                continue;
            }

            IndentInventoryList indentLine = rows.get(0);

            // ------------------------------------------------
            // APPLY DELTA TO INWARD ENTRIES
            // ------------------------------------------------
            applyDelta(indentLine, dto.getInwardId(), qtyDelta);

            // ------------------------------------------------
            // RECALCULATE DERIVED FIELDS
            // ------------------------------------------------
            recalculateIndentLine(indentLine);

            indentInventoryListRepo.save(indentLine);

            // Track affected parents
            affectedIndentIds.add(
                    indentLine.getIndentInventory().getIndentId()
            );

            if (indentLine.getPurchaseOrderId() != null) {
                affectedPoNumbers.add(indentLine.getPurchaseOrderId());
            }
        }

        // ------------------------------------------------
        // RE-EVALUATE INDENTS
        // ------------------------------------------------
        for (String indentId : affectedIndentIds) {

            Optional<IndentInventory> indentOpt =
                    indentInventoryRepo.findByIdWithDetails(indentId);

            if (!indentOpt.isPresent()) {
                continue;
            }

            indentCompletionEvaluator.evaluate(indentOpt.get());
        }

        // ------------------------------------------------
        // RE-EVALUATE PURCHASE ORDERS
        // ------------------------------------------------
        for (String poNumber : affectedPoNumbers) {

            Optional<PurchaseOrder> poOpt =
                    purchaseOrderRepo.findByIdWithDetails(poNumber);

            if (!poOpt.isPresent()) {
                continue;
            }

            purchaseOrderCompletionEvaluator.evaluate(poOpt.get());
        }
    }

    // ============================================================
    // APPLY DELTA LOGIC (IDEMPOTENT)
    // ============================================================
    private void applyDelta(
            IndentInventoryList indentLine,
            Long inwardId,
            Double qtyDelta
    ) {

        if (qtyDelta == null || qtyDelta == 0.0) {
            return;
        }

        IndentInwardEntry target = null;

        for (IndentInwardEntry entry : indentLine.getInwardEntries()) {
            if (inwardId.equals(entry.getInwardId())) {
                target = entry;
                break;
            }
        }

        // DELETE or net-zero case
        if (qtyDelta < 0) {
            if (target != null) {
                target.setQuantity(
                        target.getQuantity() + qtyDelta
                );

                if (target.getQuantity() <= 0.0) {
                    indentLine.getInwardEntries().remove(target);
                }
            }
            return;
        }

        // CREATE / UPDATE (positive delta)
        if (target == null) {
            indentLine.getInwardEntries().add(
                    new IndentInwardEntry(
                            inwardId,
                            new Date(),     // date is informational here
                            qtyDelta
                    )
            );
        } else {
            target.setQuantity(target.getQuantity() + qtyDelta);
        }
    }

    // ============================================================
    // DERIVED FIELD RECALCULATION
    // ============================================================
    private void recalculateIndentLine(IndentInventoryList indentLine) {

        double orderedQty =
                indentLine.getQuantity() == null ? 0.0 : indentLine.getQuantity();

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
