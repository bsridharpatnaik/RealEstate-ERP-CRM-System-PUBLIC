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

    // ============================================================
    // CREATE / UPDATE FLOW  (ENTITY IS SAFE TO USE)
    // ============================================================
    @Async
    @UseDefaultTenant
    public void updateIndentAfterInwardAsync(String tenantSchema, InwardInventory inwardInventory, InwardActionType actionType) {
        try {
            doUpdateIndentAfterInward(inwardInventory, actionType);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }


    @Async
    @UseDefaultTenant
    public void updateIndentAfterInwardAsync(String tenantSchema, InwardSnapshot inwardInventory, InwardActionType actionType) {
        try {
            doUpdateIndentAfterDelete(inwardInventory, actionType);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doUpdateIndentAfterInward(
            InwardInventory inwardInventory,
            InwardActionType actionType
    ) {

        final String threadName = Thread.currentThread().getName();

        log.info(
                "[TX-START] thread={} action={} inwardId={} tenant={}",
                threadName,
                actionType,
                inwardInventory != null ? inwardInventory.getInwardId() : null,
                ThreadLocalStorage.getTenantName()
        );

        try {

            if (ThreadLocalStorage.getTenantName() == null) {
                log.error(
                        "[TX-FATAL] thread={} tenant is NULL at TX start",
                        threadName
                );
                return;
            }

            Set<String> affectedIndentIds = new HashSet<>();
            Set<String> affectedPoNumbers = new HashSet<String>();

            int index = 0;

            for (InwardOutwardList io : inwardInventory.getInwardOutwardList()) {

                index++;

                log.info(
                        "[TX-LINE-START] thread={} idx={} lineItemCode={} tenant={}",
                        threadName,
                        index,
                        io.getLineItemCode(),
                        ThreadLocalStorage.getTenantName()
                );

                // -------------------------------
                // Before repository query
                // -------------------------------
                log.info(
                        "[TX-BEFORE-QUERY] thread={} idx={} tenant={}",
                        threadName,
                        index,
                        ThreadLocalStorage.getTenantName()
                );

                List<IndentInventoryList> rows =
                        indentInventoryListRepo.findByLineItemCode(
                                io.getLineItemCode()
                        );

                // -------------------------------
                // After repository query
                // -------------------------------
                log.info(
                        "[TX-AFTER-QUERY] thread={} idx={} resultSize={}",
                        threadName,
                        index,
                        rows != null ? rows.size() : null
                );

                if (rows == null || rows.isEmpty()) {
                    log.warn(
                            "[TX-SKIP] thread={} idx={} No indent line found for lineItemCode={}",
                            threadName,
                            index,
                            io.getLineItemCode()
                    );
                    continue;
                }

                IndentInventoryList indentLine = rows.get(0);

                log.info(
                        "[TX-FOUND] thread={} idx={} indentLineId={} indentId={} poId={}",
                        threadName,
                        index,
                        indentLine.getEntryid(),
                        indentLine.getIndentInventory() != null
                                ? indentLine.getIndentInventory().getIndentId()
                                : null,
                        indentLine.getPurchaseOrderId()
                );

                // -------------------------------
                // UPSERT inward entry
                // -------------------------------
                log.info(
                        "[TX-UPSERT] thread={} idx={} inwardId={}",
                        threadName,
                        index,
                        inwardInventory.getInwardId()
                );

                upsertInwardEntry(indentLine, inwardInventory, io);

                // -------------------------------
                // Recalculate
                // -------------------------------
                recalculateIndentLine(indentLine);

                // -------------------------------
                // Save indent line
                // -------------------------------
                log.info(
                        "[TX-BEFORE-SAVE] thread={} idx={} indentLineId={}",
                        threadName,
                        index,
                        indentLine.getEntryid()
                );

                indentInventoryListRepo.save(indentLine);

                log.info(
                        "[TX-AFTER-SAVE] thread={} idx={} indentLineId={}",
                        threadName,
                        index,
                        indentLine.getEntryid()
                );

                affectedIndentIds.add(indentLine.getIndentInventory().getIndentId());

                if (indentLine.getPurchaseOrderId() != null) {
                    affectedPoNumbers.add(indentLine.getPurchaseOrderId());
                }
            }

            // -------------------------------
            // Re-evaluate statuses
            // -------------------------------
            log.info(
                    "[TX-REEVAL-START] thread={} indents={} pos={}",
                    threadName,
                    affectedIndentIds.size(),
                    affectedPoNumbers.size()
            );

            reevaluateIndentsAndPOs(affectedIndentIds, affectedPoNumbers);

            log.info(
                    "[TX-SUCCESS] thread={} action={} inwardId={}",
                    threadName,
                    actionType,
                    inwardInventory.getInwardId()
            );

        } catch (Exception ex) {

            log.error(
                    "[TX-EXCEPTION] thread={} tenant={} action={} inwardId={}",
                    threadName,
                    ThreadLocalStorage.getTenantName(),
                    actionType,
                    inwardInventory != null ? inwardInventory.getInwardId() : null,
                    ex
            );

        } finally {

            log.info(
                    "[TX-END] thread={} clearing tenant (was={})",
                    threadName,
                    ThreadLocalStorage.getTenantName()
            );

            // ⚠️ Do NOT clear tenant here if caller manages it
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

            Set<String> affectedIndents = new HashSet<>();
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
                affectedIndents.add(indentLine.getIndentInventory().getIndentId());
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
            log.info("Re-evaluating PO [{}] after inward update",  poNumber);
            purchaseOrderCompletionEvaluator.evaluate(poOpt.get());
        }
    }
}