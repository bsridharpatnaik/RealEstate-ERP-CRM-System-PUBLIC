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
     * Recompute priority for ALL open purchase orders.
     * Called by the hourly scheduler and the manual /po-prioritize endpoint.
     *
     * <p>Flow:
     * <ol>
     *   <li>Fetch every non-closed PO.</li>
     *   <li>Fetch all indent refs for those POs (in one query).</li>
     *   <li>Bulk-load the referenced indent line items.</li>
     *   <li>For each open PO — compute priority from the earliest open line-item
     *       needByDate, or null-out all fields when no date is available.</li>
     * </ol>
     */
    public void recomputeAllPriorities() {
        log.info("PriorityComputeService: starting priority computation");

        // 1. All open (non-closed) POs — these are the authoritative set
        List<PurchaseOrder> openPOs = purchaseOrderRepo.findAllOpenPurchaseOrders(CLOSED_STATUSES);
        if (openPOs.isEmpty()) {
            log.info("PriorityComputeService: no open POs found, nothing to compute");
            return;
        }

        List<String> openPoIds = openPOs.stream()
                .map(PurchaseOrder::getPurchaseOrderId)
                .collect(Collectors.toList());

        // 2. Fetch indent refs for those POs (single bulk query)
        List<PurchaseOrderIndentRef> allRefs = poIndentRefRepo.findByPurchaseOrderIdIn(openPoIds);

        // 3. Group refs by PO id → list of indent line item codes
        Map<String, List<String>> poToLineItemCodes = new HashMap<>();
        for (PurchaseOrderIndentRef ref : allRefs) {
            String poId = ref.getPoLine().getPurchaseOrder().getPurchaseOrderId();
            poToLineItemCodes.computeIfAbsent(poId, k -> new ArrayList<>())
                    .add(ref.getIndentLineItemCode());
        }

        // 4. Bulk-load all referenced indent line items
        Set<String> allLineItemCodes = poToLineItemCodes.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Map<String, IndentInventoryList> lineItemMap = Collections.emptyMap();
        if (!allLineItemCodes.isEmpty()) {
            lineItemMap = indentInventoryListRepo.findByLineItemCodeIn(allLineItemCodes)
                    .stream()
                    // Only open line items with a date contribute to priority
                    .filter(li -> li.getNeedByDate() != null)
                    .filter(li -> li.getLineItemStatus() == null ||
                                  OPEN_LINE_ITEM_STATUSES.stream()
                                      .anyMatch(s -> s.equalsIgnoreCase(li.getLineItemStatus())))
                    .collect(Collectors.toMap(
                            IndentInventoryList::getLineItemCode,
                            li -> li,
                            (a, b) -> a
                    ));
        }

        // 5. Recompute and persist priority for EVERY open PO
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        final Map<String, IndentInventoryList> lineItemMapFinal = lineItemMap;

        for (PurchaseOrder po : openPOs) {
            String poId = po.getPurchaseOrderId();
            List<String> codes = poToLineItemCodes.getOrDefault(poId, Collections.emptyList());

            Date earliestNeedByDate = codes.stream()
                    .map(lineItemMapFinal::get)
                    .filter(Objects::nonNull)
                    .map(IndentInventoryList::getNeedByDate)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            if (earliestNeedByDate == null) {
                // No date available — clear all priority fields
                po.setPriority(null);
                po.setDaysToDeadline(null);
                po.setNeedByDate(null);
            } else {
                LocalDate deadline = earliestNeedByDate.toInstant()
                        .atZone(ZoneId.of("Asia/Kolkata"))
                        .toLocalDate();
                int daysInt = (int) java.time.temporal.ChronoUnit.DAYS.between(today, deadline);

                po.setNeedByDate(earliestNeedByDate);
                po.setDaysToDeadline(daysInt);
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

        log.info("PriorityComputeService: completed priority computation for {} open POs", openPOs.size());
    }

    /**
     * Recompute priority for a single PO (called after an inward is completed for that PO).
     */
    public void recomputePriorityForPo(String purchaseOrderId) {
        log.info("PriorityComputeService: recomputing priority for PO {}", purchaseOrderId);
        recomputeAllPriorities();   // simple strategy: recompute all (acceptable for current scale)
    }
}
