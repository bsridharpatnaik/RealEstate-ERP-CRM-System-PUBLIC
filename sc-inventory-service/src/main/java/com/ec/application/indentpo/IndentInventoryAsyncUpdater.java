package com.ec.application.indentpo;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.InwardActionType;
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
@EnableAsync
@UseDefaultTenant
public class IndentInventoryAsyncUpdater {

    private static final Logger log = LoggerFactory.getLogger(IndentInventoryAsyncUpdater.class);
    private final IndentInventoryListRepo indentInventoryListRepo;
    private final IndentInventoryRepo indentInventoryRepo;
    private final IndentCompletionEvaluator indentCompletionEvaluator;
    private final PurchaseOrderCompletionEvaluator purchaseOrderCompletionEvaluator;
    private final PurchaseOrderRepo purchaseOrderRepo;
    private final InwardSyncFailureService inwardSyncFailureService;

    // ============================================================
    // CREATE / UPDATE FLOW  (ENTITY IS SAFE TO USE)
    // ============================================================
    @Async
    public void updateIndentAfterInwardAsync(
            String tenantSchema,
            InwardActionType actionType,
            Long inwardId,
            Set<String> lineItemCodes
    ) {
        try {
            ThreadLocalStorage.setTenantName(tenantSchema);
            doSyncIndentAndPO(actionType, inwardId, lineItemCodes);
        } catch (Exception ex) {
            inwardSyncFailureService.recordFailure(
                    tenantSchema,
                    inwardId,
                    actionType,
                    lineItemCodes,
                    ex
            );
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void doSyncIndentAndPO(
            InwardActionType actionType,
            Long inwardId,
            Set<String> lineItemCodes
    ) {

        Set<String> affectedIndentIds = new HashSet<>();
        Set<String> affectedPoNumbers = new HashSet<>();

        List<IndentInventoryList> indentLines =
                indentInventoryListRepo.findByLineItemCodeIn(lineItemCodes);

        for (IndentInventoryList indentLine : indentLines) {

            if (actionType == InwardActionType.DELETE) {
                removeInwardEntry(indentLine, inwardId);
            } else {
                upsertInwardEntry(indentLine, inwardId);
            }

            recalculateIndentLine(indentLine);
            indentInventoryListRepo.save(indentLine);

            affectedIndentIds.add(indentLine.getIndentInventory().getIndentId());

            if (indentLine.getPurchaseOrderId() != null) {
                affectedPoNumbers.add(indentLine.getPurchaseOrderId());
            }
        }

        reevaluateIndentsAndPOs(affectedIndentIds, affectedPoNumbers);
    }


    // ============================================================
    // HELPER METHODS
    // ============================================================

    private void upsertInwardEntry(IndentInventoryList indentLine, Long inwardId, InwardOutwardList io) {
        IndentInwardEntry existing = null;
        for (IndentInwardEntry entry : indentLine.getInwardEntries()) {
            if (inwardId.equals(entry.getInwardId())) {
                existing = entry;
                break;
            }
        }

        if (existing != null) {
            existing.setQuantity(io.getQuantity());
            existing.setInwardDate(inwardInventory.getDate());
        } else {
            indentLine.getInwardEntries().add(
                    new IndentInwardEntry(inwardInventory.getInwardId(), inwardInventory.getDate(), io.getQuantity()));
        }
    }

    private void removeInwardEntry(IndentInventoryList indentLine, Long inwardId) {

        Iterator<IndentInwardEntry> it = indentLine.getInwardEntries().iterator();

        while (it.hasNext()) {
            IndentInwardEntry entry = it.next();
            if (inwardId.equals(entry.getInwardId())) {
                it.remove();
                break;
            }
        }
    }

    private void recalculateIndentLine(IndentInventoryList indentLine) {

        double totalReceived = 0.0;
        for (IndentInwardEntry entry : indentLine.getInwardEntries()) {
            if (entry.getQuantity() != null) {
                totalReceived += entry.getQuantity();
            }
        }

        indentLine.setQuantityReceived(totalReceived);

        double orderedQty = indentLine.getQuantity() == null ? 0.0 : indentLine.getQuantity();

        double pendingQty = orderedQty - totalReceived;
        if (pendingQty < 0) {
            pendingQty = 0.0;
        }

        indentLine.setQuantityPending(pendingQty);
        indentLine.setLineItemStatus(
                pendingQty == 0.0
                        ? IndentLineItemStatusConstants.STATUS_INWARD_COMPLETE
                        : IndentLineItemStatusConstants.STATUS_INWARD_PARTIAL
        );
    }

    private void reevaluateIndentsAndPOs(Set<String> indentIds, Set<String> poNumbers) {

        for (String indentId : indentIds) {
            Optional<IndentInventory> indentOpt = indentInventoryRepo.findByIdWithDetails(indentId);
            if (!indentOpt.isPresent()) {
                continue;
            }
            indentCompletionEvaluator.evaluate(indentOpt.get());
        }

        // -----------------------------
        // Re-evaluate PURCHASE ORDERS
        // -----------------------------
        for (String poNumber : poNumbers) {
            Optional<PurchaseOrder> poOpt = purchaseOrderRepo.findByIdWithDetails(poNumber);
            if (!poOpt.isPresent()) {
                continue;
            }
            log.info("Re-evaluating PO [{}] after inward update", poNumber);
            purchaseOrderCompletionEvaluator.evaluate(poOpt.get());
        }
    }
}