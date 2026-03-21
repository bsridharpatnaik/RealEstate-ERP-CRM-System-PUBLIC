package com.ec.application.service;

import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderIndentRef;
import com.ec.application.model.PurchaseOrderLine;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.PurchaseOrderIndentRefRepository;
import com.ec.application.repository.PurchaseOrderRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes and persists priority + daysToDeadline on every open PurchaseOrder.
 *
 * <p>Priority rules (based on the earliest open-indent needByDate linked to the PO):
 * <ul>
 *   <li>CRITICAL  — ≤ 3 days remaining (or already overdue)</li>
 *   <li>HIGH      — 4–7 days remaining</li>
 *   <li>MEDIUM    — 8–14 days remaining</li>
 *   <li>NORMAL    — &gt; 14 days remaining</li>
 *   <li>null      — no needByDate set on any linked open indent line item</li>
 * </ul>
 */
@Service
@Transactional
public class PriorityComputeService {

    private static final Logger log = LoggerFactory.getLogger(PriorityComputeService.class);

    /** PO statuses considered "closed" — excluded from priority computation. */
    private static final List<String> CLOSED_STATUSES =
            Arrays.asList("CANCELLED", "SHORT CLOSE", "SHORT CLOSED", "COMPLETE INWARD");

    /** Indent line item statuses that are NOT yet fully received. */
    private static final List<String> OPEN_LINE_ITEM_STATUSES =
            Arrays.asList("NEW", "PO PARTIAL", "PARTIAL");

    @Autowired
    private PurchaseOrderRepo purchaseOrderRepo;

    @Autowired
    private PurchaseOrderIndentRefRepository poIndentRefRepo;

    @Autowired
    private IndentInventoryListRepo indentInventoryListRepo;

    /**
     * Recompute priority for ALL open purchase orders in the current tenant schema.
     * Called by the nightly scheduler and after inward completion.
     */
    public void recomputeAllPriorities() {
        log.info("PriorityComputeService: starting priority computation");

        // 1. Load all PO→indent-line-item references for open POs
        List<PurchaseOrderIndentRef> allRefs = poIndentRefRepo.findAllForOpenPurchaseOrders(CLOSED_STATUSES);
        if (allRefs.isEmpty()) {
            log.info("PriorityComputeService: no open PO refs found, nothing to compute");
            return;
        }

        // 2. Group refs by PO id
        Map<String, List<String>> poToLineItemCodes = new HashMap<>();
        for (PurchaseOrderIndentRef ref : allRefs) {
            String poId = ref.getPoLine().getPurchaseOrder().getPurchaseOrderId();
            poToLineItemCodes.computeIfAbsent(poId, k -> new ArrayList<>())
                    .add(ref.getIndentLineItemCode());
        }

        // 3. Bulk-load all referenced indent line items
        Set<String> allLineItemCodes = poToLineItemCodes.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        List<IndentInventoryList> lineItems =
                indentInventoryListRepo.findByLineItemCodeIn(allLineItemCodes);

        // 4. Keep only open (not fully received) items that have a needByDate
        Map<String, IndentInventoryList> lineItemMap = lineItems.stream()
                .filter(li -> li.getNeedByDate() != null)
                .filter(li -> li.getLineItemStatus() == null ||
                              OPEN_LINE_ITEM_STATUSES.stream()
                                  .anyMatch(s -> s.equalsIgnoreCase(li.getLineItemStatus())))
                .collect(Collectors.toMap(
                        IndentInventoryList::getLineItemCode,
                        li -> li,
                        (a, b) -> a   // keep first if duplicate
                ));

        // 5. For each PO, find earliest needByDate and persist priority
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        Map<String, PurchaseOrder> poCache = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : poToLineItemCodes.entrySet()) {
            String poId = entry.getKey();
            List<String> codes = entry.getValue();

            Date earliestNeedByDate = codes.stream()
                    .map(lineItemMap::get)
                    .filter(Objects::nonNull)
                    .map(IndentInventoryList::getNeedByDate)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            PurchaseOrder po = poCache.computeIfAbsent(poId, id ->
                    purchaseOrderRepo.findById(id).orElse(null));
            if (po == null) continue;

            if (earliestNeedByDate == null) {
                po.setPriority(null);
                po.setDaysToDeadline(null);
                po.setNeedByDate(null);
            } else {
                LocalDate deadline = earliestNeedByDate.toInstant()
                        .atZone(ZoneId.of("Asia/Kolkata"))
                        .toLocalDate();
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, deadline);
                int daysInt = (int) days;

                po.setDaysToDeadline(daysInt);
                po.setNeedByDate(earliestNeedByDate);
                if (daysInt <= 3) {
                    po.setPriority("CRITICAL");
                } else if (daysInt <= 7) {
                    po.setPriority("HIGH");
                } else if (daysInt <= 14) {
                    po.setPriority("MEDIUM");
                } else {
                    po.setPriority("NORMAL");
                }
            }
            purchaseOrderRepo.save(po);
        }

        log.info("PriorityComputeService: completed priority computation for {} POs",
                poToLineItemCodes.size());
    }

    /**
     * Recompute priority for a single PO (called after an inward is completed for that PO).
     */
    public void recomputePriorityForPo(String purchaseOrderId) {
        log.info("PriorityComputeService: recomputing priority for PO {}", purchaseOrderId);
        recomputeAllPriorities();   // simple strategy: recompute all (acceptable for current scale)
    }
}
