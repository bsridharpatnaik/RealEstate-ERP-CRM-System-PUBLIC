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
import com.ec.application.repository.PurchaseOrderRepo;
import com.ec.application.service.CategoryService;
import com.ec.application.service.InwardInventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    // ============================================================
    // CREATE / UPDATE FLOW  (ENTITY IS SAFE TO USE)
    // ============================================================
    @Async
    public void updateIndentAfterInwardAsync(String tenantSchema, InwardInventory inwardInventory, InwardActionType actionType) {
        ThreadLocalStorage.setTenantName(tenantSchema);
        try {
            doUpdateIndentAfterInward(inwardInventory, actionType);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }


    @Async
    public void updateIndentAfterInwardAsync(String tenantSchema, InwardSnapshot inwardInventory, InwardActionType actionType) {
        ThreadLocalStorage.setTenantName(tenantSchema);
        try {
            doUpdateIndentAfterDelete(inwardInventory, actionType);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doUpdateIndentAfterInward(InwardInventory inwardInventory, InwardActionType actionType) {

        try {
            log.info("Starting async inward {} processing | inwardId={}", actionType, inwardInventory.getInwardId());
            Set<IndentInventory> affectedIndents = new HashSet<IndentInventory>();
            Set<String> affectedPoNumbers = new HashSet<String>();

            for (InwardOutwardList io : inwardInventory.getInwardOutwardList()) {

                List<IndentInventoryList> rows = indentInventoryListRepo.findByLineItemCode(io.getLineItemCode());

                if (rows.isEmpty()) {
                    continue;
                }

                IndentInventoryList indentLine = rows.get(0);

                // UPSERT inward entry
                upsertInwardEntry(indentLine, inwardInventory, io);

                // Recalculate derived values
                recalculateIndentLine(indentLine);
                indentInventoryListRepo.save(indentLine);
                affectedIndents.add(indentLine.getIndentInventory());
                if (indentLine.getPurchaseOrderId() != null) {
                    affectedPoNumbers.add(indentLine.getPurchaseOrderId());
                }
            }
            reevaluateIndentsAndPOs(affectedIndents, affectedPoNumbers);
        } catch (Exception ex) {
            log.error("Async inward {} failed | inwardId={}", actionType, inwardInventory.getInwardId(), ex);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }


    // ============================================================
    // DELETE FLOW (ENTITY IS GONE → USE SNAPSHOT)
    // ============================================================
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doUpdateIndentAfterDelete(InwardSnapshot snapshot, InwardActionType actionType) {

        try {

            if (!actionType.equals(InwardActionType.DELETE)) {
                log.error("Invalid action type for DELETE flow: {}", actionType);
                return;
            }

            Set<IndentInventory> affectedIndents = new HashSet<IndentInventory>();
            Set<String> affectedPoNumbers = new HashSet<String>();

            for (InwardLineSnapshot line : snapshot.getLines()) {
                List<IndentInventoryList> rows = indentInventoryListRepo.findByLineItemCode(line.getLineItemCode());

                if (rows.isEmpty()) {
                    continue;
                }
                IndentInventoryList indentLine = rows.get(0);
                // REMOVE inward entry
                removeInwardEntry(indentLine, snapshot.getInwardId());

                // Recalculate
                recalculateIndentLine(indentLine);
                indentInventoryListRepo.save(indentLine);
                affectedIndents.add(indentLine.getIndentInventory());
                if (indentLine.getPurchaseOrderId() != null) {
                    affectedPoNumbers.add(indentLine.getPurchaseOrderId());
                }
            }
            reevaluateIndentsAndPOs(affectedIndents, affectedPoNumbers);
        } catch (Exception ex) {
            log.error("Async inward DELETE failed | tenant={} | inwardId={}", snapshot.getInwardId(), ex);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private void upsertInwardEntry(IndentInventoryList indentLine, InwardInventory inwardInventory, InwardOutwardList io) {

        IndentInwardEntry existing = null;

        for (IndentInwardEntry entry : indentLine.getInwardEntries()) {
            if (inwardInventory.getInwardId().equals(entry.getInwardId())) {
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

    private void reevaluateIndentsAndPOs(Set<IndentInventory> indents, Set<String> poNumbers) {

        for (IndentInventory indent : indents) {
            indentCompletionEvaluator.evaluate(indent);
            indentInventoryRepo.save(indent);
        }

        for (String poNumber : poNumbers) {
            PurchaseOrder po = purchaseOrderRepo.findByIdWithDetails(poNumber).get();
            purchaseOrderCompletionEvaluator.evaluate(po);
            purchaseOrderRepo.save(po);
        }
    }
}