package com.ec.application.service;

import static java.util.stream.Collectors.counting;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import com.ec.application.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.data.BatchOverrideEntry;
import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.OutwardInventoryExportDAO2;
import com.ec.application.data.ProductGroupedDAO;
import com.ec.application.data.ProductWithQuantity;
import com.ec.application.data.ReturnOutwardInventoryData;
import com.ec.application.data.ReturnRejectInwardOutwardData;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.InwardOutwardListRepo;
import com.ec.application.repository.InventoryBatchRepository;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.OutwardBatchConsumptionRepository;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.UsageAreaRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.OutwardInventorySpecification;
import com.ec.application.ReusableClasses.ActivityLogDescription;

@Service
@Transactional
public class OutwardInventoryService {
    @Autowired
    InwardOutwardListRepo iolRepo;

    @Autowired
    ProductRepo productRepo;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @Autowired
    StockService stockService;

    @Autowired
    OutwardInventoryRepo outwardInventoryRepo;

    @Autowired
    LocationRepo locationRepo;

    @Autowired
    StockRepo stockRepo;

    @Autowired
    ContractorRepo contractorRepo;

    @Autowired
    WarehouseRepo warehouseRepo;

    @Autowired
    UsageAreaRepo usageAreaRepo;

    @Autowired
    InventoryNotificationService inventoryNotificationService;

    @Autowired
    GroupBySpecification groupBySpecification;

    @Autowired
    UserDetailsService userDetailService;

    @Autowired
    private AsyncService asyncService;

    @Autowired
    AsyncServiceInventory asyncServiceInventory;

    @Autowired
    ProjectConstantsService projectConstantsService;

    @Autowired
    BOQService boqService;

    @Autowired
    InventoryBatchRepository inventoryBatchRepository;

    @Autowired
    OutwardBatchConsumptionRepository outwardBatchConsumptionRepository;

    @Autowired
    ActivityLogService activityLogService;

    Logger log = LoggerFactory.getLogger(OutwardInventoryService.class);

    private String resolveCurrentUser() {
        try { return userDetailService.getCurrentUser().getUsername(); }
        catch (Exception e) { return "System"; }
    }

    /** Composite key for matching a line item by (productId, warehouseId) — warehouseId may be null
     *  for legacy callers, in which case the key degrades to product-only matching. */
    private static String lineKey(Long productId, Long warehouseId) {
        return productId + "_" + (warehouseId != null ? warehouseId : "");
    }

    /**
     * Collapses multiple lines of the same product (now possible across different warehouses)
     * into one combined ProductWithQuantity per product, summing quantities. BOQ is enforced
     * per product+location, not per warehouse, so the BOQ check must see the combined demand —
     * otherwise two 60-unit lines of a product with 100 BOQ remaining would each independently
     * pass a "≤100" check (60 ≤ 100) while jointly exceeding it (120 > 100).
     */
    private List<ProductWithQuantity> aggregateByProduct(List<ProductWithQuantity> items) {
        Map<Long, Double> totals = new LinkedHashMap<>();
        for (ProductWithQuantity item : items) {
            totals.merge(item.getProductId(), item.getQuantity(), Double::sum);
        }
        List<ProductWithQuantity> aggregated = new ArrayList<>();
        for (Map.Entry<Long, Double> e : totals.entrySet()) {
            aggregated.add(new ProductWithQuantity(e.getKey(), e.getValue()));
        }
        return aggregated;
    }

    /**
     * Finds the single InwardOutwardList line matching productId (and warehouseId, when provided).
     * Throws a clear error if the match is ambiguous (same product on multiple warehouse lines,
     * but the caller didn't say which one) rather than silently picking one.
     */
    private InwardOutwardList findLineForProductAndWarehouse(Set<InwardOutwardList> lines, Long productId, Long warehouseId) throws Exception {
        List<InwardOutwardList> matchesByProduct = lines.stream()
                .filter(l -> l.getProduct().getProductId().equals(productId))
                .collect(Collectors.toList());
        if (matchesByProduct.isEmpty()) return null;
        if (matchesByProduct.size() == 1) return matchesByProduct.get(0);

        // More than one line has this product (different warehouses) — caller must disambiguate.
        if (warehouseId == null) {
            throw new Exception("Product '" + matchesByProduct.get(0).getProduct().getProductName()
                    + "' was outwarded from multiple warehouses on this entry. Please specify which warehouse this applies to.");
        }
        List<InwardOutwardList> matchesByWarehouse = matchesByProduct.stream()
                .filter(l -> l.getWarehouse().getWarehouseId().equals(warehouseId))
                .collect(Collectors.toList());
        if (matchesByWarehouse.isEmpty()) {
            throw new Exception("Product '" + matchesByProduct.get(0).getProduct().getProductName()
                    + "' was not outwarded from the specified warehouse on this entry.");
        }
        return matchesByWarehouse.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"boqStatusRows", "boqOutwardQty"}, allEntries = true)
    public OutwardInventory createOutwardnventory(OutwardInventoryData oiData) throws Exception {
        log.info("Invoked createOutwardnventory with payload -" + oiData.toString());
        OutwardInventory outwardInventory = new OutwardInventory();
        boolean allHaveBOQ = validateInputs(oiData);
        exitIfNotAuthorized(outwardInventory, oiData, APICallTypeForAuthorization.Create);
        setFields(outwardInventory, oiData);
        outwardInventory.setHasBOQ(allHaveBOQ ? true : null);
        updateStockForCreateOutwardInventory(outwardInventory);
        outwardInventoryRepo.save(outwardInventory);
        consumeBatchesForOutward(outwardInventory, oiData);
        String createUser = resolveCurrentUser();
        boolean multiWarehouse = outwardInventory.getInwardOutwardList().stream()
                .map(l -> l.getWarehouse().getWarehouseId()).distinct().count() > 1;
        List<Map<String, Object>> createItems = new ArrayList<>();
        for (InwardOutwardList io : outwardInventory.getInwardOutwardList())
            createItems.add(multiWarehouse
                    ? ActivityLogDescription.item(io.getProduct().getProductName(), io.getQuantity(), io.getWarehouse().getWarehouseName())
                    : ActivityLogDescription.item(io.getProduct().getProductName(), io.getQuantity()));
        activityLogService.record("CREATED", "OUTWARD", String.valueOf(outwardInventory.getOutwardid()),
                ActivityLogDescription.withItems("Outward " + outwardInventory.getOutwardid() + " created by " + createUser, createItems),
                createUser);
        return outwardInventory;
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockForCreateOutwardInventory(OutwardInventory outwardInventory) throws Exception {
        log.info("Invoked updateStockForCreateOutwardInventory");
        Set<InwardOutwardList> productsWithQuantities = outwardInventory.getInwardOutwardList();

        for (InwardOutwardList oiList : productsWithQuantities) {
            Long productId = oiList.getProduct().getProductId();
            Long warehouseId = oiList.getWarehouse().getWarehouseId();
            Double quantity = oiList.getQuantity();
            Double closingStock = stockService.updateStock(productId, warehouseId, quantity, "outward");
            oiList.setClosingStock(closingStock);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public OutwardInventory addReturnEntry(ReturnRejectInwardOutwardData rd, Long outwardId, String type)
            throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!outwardInventoryRepo.existsById(outwardId))
            throw new Exception("Outward inventory with ID not found");

        if (rd.getProductWithQuantities().size() == 0)
            throw new Exception("Minimum of one product is required to save data.");

        if (type.equals("reject")) {
            for (ProductWithQuantity pwq : rd.getProductWithQuantities()) {
                if (pwq.getRemarks() == null)
                    throw new Exception(
                            "Remarks is a mandatory field. Please provide remarks for all products before saving data");

                if (pwq.getRemarks().trim().equals(""))
                    throw new Exception("Remarks is a mandatory field. Please provide remarks before saving data");
            }
        }
        // Dedupe by (productId, warehouseId) — a return/reject payload may legitimately target
        // the same product across two different lines if the original outward drew it from
        // multiple warehouses, as long as each (product, warehouse) pair appears once.
        Long duplicateProductIdCount = rd.getProductWithQuantities().stream()
                .collect(Collectors.groupingBy(pwq -> lineKey(pwq.getProductId(), pwq.getWarehouseId()), counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Inventory List should be Unique. Same product/warehouse combination added multiple times. Please correct.");

        for (ProductWithQuantity productWithQuantity : rd.getProductWithQuantities()) {
            if (productWithQuantity.getQuantity() == null || productWithQuantity.getProductId() == null
                    || !productRepo.existsById(productWithQuantity.getProductId()))
                throw new Exception("Error fetching product details");
            if (type.equals("return"))
                addReturnForOutward(outwardId, productWithQuantity);
            else
                addRejectForOutward(outwardId, productWithQuantity.getProductId(), productWithQuantity.getQuantity(),
                        productWithQuantity.getRemarks(), productWithQuantity.getWarehouseId(),
                        productWithQuantity.getRejectBatches());
        }

        String actionUser = resolveCurrentUser();
        String actionType = type.equals("return") ? "RETURNED" : "REJECTED";
        List<Map<String, Object>> actionItems = rd.getProductWithQuantities().stream()
                .map(pwq -> {
                    String name = productRepo.findById(pwq.getProductId())
                            .map(p -> p.getProductName()).orElse("ID:" + pwq.getProductId());
                    return ActivityLogDescription.item(name, pwq.getQuantity());
                })
                .collect(Collectors.toList());
        activityLogService.record(actionType, "OUTWARD", String.valueOf(outwardId),
                ActivityLogDescription.withItems("Outward " + outwardId + " " + type + " by " + actionUser, actionItems),
                actionUser);

        return outwardInventoryRepo.findById(outwardId).get();
    }

    @Transactional(rollbackFor = Exception.class)
    private void addReturnForOutward(Long outwardId, ProductWithQuantity pwq) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        Long productId = pwq.getProductId();
        Double quantity = pwq.getQuantity();
        List<BatchOverrideEntry> returnBatches = pwq.getReturnBatches();

        OutwardInventory oi = outwardInventoryRepo.findById(outwardId).get();
        exitIfNotAuthorized(oi, null, APICallTypeForAuthorization.Reject);

        Set<ReturnOutwardList> returnOutwardList = oi.getReturnOutwardList();
        Set<InwardOutwardList> inwardOutwardListSet = oi.getInwardOutwardList();
        InwardOutwardList inwardOutwardList = findLineForProductAndWarehouse(inwardOutwardListSet, productId, pwq.getWarehouseId());
        if (inwardOutwardList != null) {
            Double currentQuantity = inwardOutwardList.getQuantity();
            if (quantity > currentQuantity)
                throw new Exception(
                        "Return quantity cannot be greater than existing quantity for product -"
                                + inwardOutwardList.getProduct().getProductName());

            Double diffInQuantity = currentQuantity - quantity;
            Double closingStock = stockService.updateStock(productId, inwardOutwardList.getWarehouse().getWarehouseId(),
                    quantity, "inward");

            // Restore batch quantities, capturing a one-time snapshot of which batch(es) the
            // returned qty came from for display (see batchEntriesJson on ReturnOutwardList).
            List<BatchOverrideEntry> batchSnapshot = restoreBatchesForReturn(outwardId, productId, quantity, returnBatches);

            ReturnOutwardList returnEntry = new ReturnOutwardList(new Date(), inwardOutwardList.getProduct(), currentQuantity,
                    quantity, closingStock);
            if (batchSnapshot != null && !batchSnapshot.isEmpty()) {
                try {
                    returnEntry.setBatchEntriesJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(batchSnapshot));
                } catch (Exception e) {
                    log.warn("Failed to serialize batchEntriesJson for return: " + e.getMessage());
                }
            }
            returnOutwardList.add(returnEntry);
            inwardOutwardList.setQuantity(diffInQuantity);
            inwardOutwardList.setClosingStock(closingStock);
        }
        oi.setReturnOutwardList(returnOutwardList);
        oi.setInwardOutwardList(inwardOutwardListSet);
        outwardInventoryRepo.save(oi);
    }

    /**
     * Restore batches after a return.
     * If returnBatches is provided (multi-batch scenario), restore each specified batch.
     * Otherwise, auto-restore from OutwardBatchConsumption records (single-batch scenario).
     * Returns an enriched snapshot (batchId, qty, brand, lotNumber, expiryDate) of what was
     * restored, for display — never re-derived after this call.
     */
    private List<BatchOverrideEntry> restoreBatchesForReturn(Long outwardId, Long productId, Double quantity,
                                         List<BatchOverrideEntry> returnBatches) {
        List<BatchOverrideEntry> snapshot = new ArrayList<>();
        try {
            if (returnBatches != null && !returnBatches.isEmpty()) {
                // ----- PRE-VALIDATION (no saves yet) -----

                // Filter valid entries only
                List<BatchOverrideEntry> validEntries = returnBatches.stream()
                        .filter(e -> e.getBatchId() != null && e.getQty() != null && e.getQty() > 0)
                        .collect(java.util.stream.Collectors.toList());
                if (validEntries.isEmpty()) {
                    throw new IllegalArgumentException("No valid batch entries provided for return.");
                }
                // Duplicate batchId check
                Set<Long> seenIds = new HashSet<>();
                for (BatchOverrideEntry e : validEntries) {
                    if (!seenIds.add(e.getBatchId())) {
                        throw new IllegalArgumentException("Duplicate batch ID " + e.getBatchId() + " in return batches.");
                    }
                }
                // Sum must equal return quantity
                double totalReturn = validEntries.stream().mapToDouble(BatchOverrideEntry::getQty).sum();
                if (Math.abs(totalReturn - quantity) > 0.001) {
                    throw new IllegalArgumentException(
                            "Return batch quantities (" + totalReturn
                            + ") must equal the return quantity (" + quantity + ").");
                }
                for (BatchOverrideEntry entry : validEntries) {
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Batch not found: " + entry.getBatchId()));
                    // Product ownership
                    if (!batch.getProduct().getProductId().equals(productId)) {
                        throw new IllegalArgumentException(
                                "Batch #" + entry.getBatchId() + " belongs to product '"
                                + batch.getProduct().getProductName() + "', not the returned product.");
                    }
                    // Outward ownership — batch must have been consumed by this outward
                    List<OutwardBatchConsumption> cons = outwardBatchConsumptionRepository
                            .findByOutwardIdAndBatch_BatchId(outwardId, entry.getBatchId());
                    if (cons.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Batch #" + entry.getBatchId() + " was not consumed by outward #" + outwardId + ".");
                    }
                    // Upper-bound: cannot return more than was consumed
                    double totalConsumed = cons.stream().mapToDouble(OutwardBatchConsumption::getQtyConsumed).sum();
                    if (entry.getQty() > totalConsumed + 0.001) {
                        throw new IllegalArgumentException(
                                "Cannot return " + entry.getQty() + " from batch #" + entry.getBatchId()
                                + " — only " + String.format("%.3f", totalConsumed) + " units were consumed.");
                    }
                }
                // ----- APPLY (all validations passed) -----
                for (BatchOverrideEntry entry : validEntries) {
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId()).orElse(null);
                    if (batch != null) {
                        batch.setQtyRemaining(batch.getQtyRemaining() + entry.getQty());
                        inventoryBatchRepository.save(batch);
                        snapshot.add(enrichBatchEntry(batch, entry.getQty()));
                        List<OutwardBatchConsumption> cons = outwardBatchConsumptionRepository
                                .findByOutwardIdAndBatch_BatchId(outwardId, entry.getBatchId());
                        for (OutwardBatchConsumption c : cons) {
                            double newQty = c.getQtyConsumed() - entry.getQty();
                            if (newQty <= 0.001) {
                                outwardBatchConsumptionRepository.delete(c);
                            } else {
                                c.setQtyConsumed(newQty);
                                outwardBatchConsumptionRepository.save(c);
                            }
                        }
                    }
                }
            } else {
                // Auto-restore only safe for single-batch outwards.
                // If multiple distinct batches were consumed, require explicit allocation.
                List<OutwardBatchConsumption> consumptions =
                        outwardBatchConsumptionRepository.findByOutwardIdAndProductIdOrderByIdAsc(outwardId, productId);
                if (consumptions.isEmpty()) return snapshot;

                long distinctBatches = consumptions.stream()
                        .filter(c -> c.getBatch() != null)
                        .map(c -> c.getBatch().getBatchId())
                        .distinct().count();
                if (distinctBatches > 1) {
                    throw new IllegalArgumentException(
                            "This outward consumed from " + distinctBatches + " batches. "
                            + "Please specify which batch(es) to return from.");
                }

                double totalConsumed = consumptions.stream().mapToDouble(OutwardBatchConsumption::getQtyConsumed).sum();
                if (totalConsumed <= 0) return snapshot;

                double remaining = quantity;
                // Restore from most-recently-consumed first (LIFO restore)
                List<OutwardBatchConsumption> reversed = new ArrayList<>(consumptions);
                Collections.reverse(reversed);
                for (OutwardBatchConsumption c : reversed) {
                    if (remaining <= 0) break;
                    double restoreQty = Math.min(remaining, c.getQtyConsumed());
                    InventoryBatch batch = c.getBatch();
                    if (batch != null) {
                        batch.setQtyRemaining(batch.getQtyRemaining() + restoreQty);
                        inventoryBatchRepository.save(batch);
                        snapshot.add(enrichBatchEntry(batch, restoreQty));
                    }
                    double newQty = c.getQtyConsumed() - restoreQty;
                    if (newQty <= 0.001) {
                        outwardBatchConsumptionRepository.delete(c);
                    } else {
                        c.setQtyConsumed(newQty);
                        outwardBatchConsumptionRepository.save(c);
                    }
                    remaining -= restoreQty;
                }
                // Guard: stock was already increased by full quantity — if batches couldn't absorb it all,
                // we have a divergence. This should not happen in normal flow but fail loud if it does.
                if (remaining > 0.001) {
                    throw new IllegalStateException(
                            "Batch restore incomplete: " + String.format("%.3f", remaining)
                            + " units could not be returned to any batch. Stock/batch totals may be inconsistent.");
                }
            }
            return snapshot;
        } catch (Exception e) {
            // #8: Rethrow — don't silently swallow; stock/batch divergence would corrupt future outwards
            log.error("Batch restore failed for return outwardId={}, productId={}: {}", outwardId, productId, e.getMessage(), e);
            throw new RuntimeException("Failed to restore batch quantities after return: " + e.getMessage(), e);
        }
    }

    private BatchOverrideEntry enrichBatchEntry(InventoryBatch batch, Double qty) {
        BatchOverrideEntry enriched = new BatchOverrideEntry();
        enriched.setBatchId(batch.getBatchId());
        enriched.setQty(qty);
        enriched.setBrand(batch.getBrand());
        enriched.setLotNumber(batch.getLotNumber());
        if (batch.getExpiryDate() != null) {
            enriched.setExpiryDate(new java.text.SimpleDateFormat("dd-MM-yyyy").format(batch.getExpiryDate()));
        }
        return enriched;
    }

    // Reject means the contractor lost/damaged the material at site — it never comes back to the
    // warehouse. The original outward already deducted it from Stock/batches at consumption time,
    // and that deduction must stand. This only reclassifies the qty into RejectOutwardList for
    // reporting; it must NOT call stockService.updateStock or restore batch consumption.
    //
    // `quantity` on the line is deliberately left untouched — it feeds the stock ledger view
    // (all_inventory_view) and must keep representing "stock that left and hasn't been returned."
    // Rejected-so-far is tracked separately on InwardOutwardList.rejectedQuantity so the same qty
    // can't be rejected twice, without shrinking the ledger total reject doesn't actually undo.
    @Transactional(rollbackFor = Exception.class)
    private void addRejectForOutward(Long outwardId, Long productId, Double quantity, String remarks, Long warehouseId,
                                      List<BatchOverrideEntry> rejectBatches) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        OutwardInventory oi = outwardInventoryRepo.findById(outwardId).get();
        exitIfNotAuthorized(oi, null, APICallTypeForAuthorization.Reject);
        Set<RejectOutwardList> rejectOutwardList = oi.getRejectOutwardList();
        Set<InwardOutwardList> inwardOutwardListSet = oi.getInwardOutwardList();
        InwardOutwardList inwardOutwardList = findLineForProductAndWarehouse(inwardOutwardListSet, productId, warehouseId);
        if (inwardOutwardList != null) {
            Double currentQuantity = inwardOutwardList.getQuantity();
            Double alreadyRejected = inwardOutwardList.getRejectedQuantity() != null ? inwardOutwardList.getRejectedQuantity() : 0.0;
            Double remainingRejectable = currentQuantity - alreadyRejected;
            if (quantity > remainingRejectable)
                throw new Exception(
                        "Reject quantity cannot be greater than existing quantity for product -"
                                + inwardOutwardList.getProduct().getProductName());

            RejectOutwardList rejectEntry = new RejectOutwardList(new Date(), inwardOutwardList.getProduct(), currentQuantity,
                    quantity, remarks);
            List<BatchOverrideEntry> batchSnapshot = buildRejectBatchSnapshot(outwardId, productId, quantity, rejectBatches);
            if (batchSnapshot != null && !batchSnapshot.isEmpty()) {
                try {
                    rejectEntry.setBatchEntriesJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(batchSnapshot));
                } catch (Exception e) {
                    log.warn("Failed to serialize batchEntriesJson for reject: " + e.getMessage());
                }
            }
            rejectOutwardList.add(rejectEntry);

            inwardOutwardList.setRejectedQuantity(alreadyRejected + quantity);
        }
        oi.setRejectOutwardList(rejectOutwardList);
        oi.setInwardOutwardList(inwardOutwardListSet);
        outwardInventoryRepo.save(oi);
    }

    /**
     * Read-only batch attribution for a reject — does NOT mutate batch.qtyRemaining or
     * OutwardBatchConsumption (stock/batch state was already deducted at outward-create time
     * and must stand). Only records which batch(es) the rejected qty is attributed to, for display.
     * Validated the same way as a return (sum/ownership/upper-bound against what's still
     * consumption-attributed to this outward), but never applies any mutation.
     */
    private List<BatchOverrideEntry> buildRejectBatchSnapshot(Long outwardId, Long productId, Double quantity,
                                                                List<BatchOverrideEntry> rejectBatches) {
        List<BatchOverrideEntry> snapshot = new ArrayList<>();
        try {
            if (rejectBatches != null && !rejectBatches.isEmpty()) {
                List<BatchOverrideEntry> validEntries = rejectBatches.stream()
                        .filter(e -> e.getBatchId() != null && e.getQty() != null && e.getQty() > 0)
                        .collect(java.util.stream.Collectors.toList());
                if (validEntries.isEmpty()) {
                    throw new IllegalArgumentException("No valid batch entries provided for reject.");
                }
                Set<Long> seenIds = new HashSet<>();
                for (BatchOverrideEntry e : validEntries) {
                    if (!seenIds.add(e.getBatchId())) {
                        throw new IllegalArgumentException("Duplicate batch ID " + e.getBatchId() + " in reject batches.");
                    }
                }
                double total = validEntries.stream().mapToDouble(BatchOverrideEntry::getQty).sum();
                if (Math.abs(total - quantity) > 0.001) {
                    throw new IllegalArgumentException(
                            "Reject batch quantities (" + total + ") must equal the reject quantity (" + quantity + ").");
                }
                for (BatchOverrideEntry entry : validEntries) {
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                    if (!batch.getProduct().getProductId().equals(productId)) {
                        throw new IllegalArgumentException(
                                "Batch #" + entry.getBatchId() + " belongs to product '"
                                + batch.getProduct().getProductName() + "', not the rejected product.");
                    }
                    List<OutwardBatchConsumption> cons = outwardBatchConsumptionRepository
                            .findByOutwardIdAndBatch_BatchId(outwardId, entry.getBatchId());
                    if (cons.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Batch #" + entry.getBatchId() + " was not consumed by outward #" + outwardId + ".");
                    }
                    double totalConsumed = cons.stream().mapToDouble(OutwardBatchConsumption::getQtyConsumed).sum();
                    if (entry.getQty() > totalConsumed + 0.001) {
                        throw new IllegalArgumentException(
                                "Cannot reject " + entry.getQty() + " from batch #" + entry.getBatchId()
                                + " — only " + String.format("%.3f", totalConsumed) + " units were consumed.");
                    }
                    snapshot.add(enrichBatchEntry(batch, entry.getQty()));
                }
            } else {
                List<OutwardBatchConsumption> consumptions =
                        outwardBatchConsumptionRepository.findByOutwardIdAndProductIdOrderByIdAsc(outwardId, productId);
                if (consumptions.isEmpty()) return snapshot;

                long distinctBatches = consumptions.stream()
                        .filter(c -> c.getBatch() != null)
                        .map(c -> c.getBatch().getBatchId())
                        .distinct().count();
                if (distinctBatches > 1) {
                    throw new IllegalArgumentException(
                            "This outward consumed from " + distinctBatches + " batches. "
                            + "Please specify which batch(es) the rejected qty came from.");
                }
                InventoryBatch batch = consumptions.get(0).getBatch();
                if (batch != null) {
                    snapshot.add(enrichBatchEntry(batch, quantity));
                }
            }
            return snapshot;
        } catch (Exception e) {
            log.error("Reject batch attribution failed for outwardId={}, productId={}: {}", outwardId, productId, e.getMessage(), e);
            throw new RuntimeException("Failed to record batch attribution for reject: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"boqStatusRows", "boqOutwardQty"}, allEntries = true)
    public OutwardInventory updateOutwardnventory(OutwardInventoryData iiData, Long id) throws Exception {
        log.info("Invoked updateOutwardnventory");
        Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
        if (!outwardInventoryOpt.isPresent())
            throw new Exception("Inventory Entry with ID not found");
        OutwardInventory outwardInventory = outwardInventoryOpt.get();

        // Adding or removing product lines during an outward edit is not allowed. modifyStockBeforeUpdate
        // reconciles stock by delta over the payload lines only, so a removed line's outward deduction
        // would never be restored (silent stock leak) while removeOrphans still drops the row. To reduce
        // an outward, use Return. Mirrors the same guard on inward edits (updateInwardInventory).
        Set<String> existingLineKeys = outwardInventory.getInwardOutwardList().stream()
                .map(io -> lineKey(io.getProduct().getProductId(), io.getWarehouse().getWarehouseId()))
                .collect(Collectors.toSet());
        Set<String> payloadLineKeys = iiData.getProductWithQuantities().stream()
                .map(pwq -> lineKey(pwq.getProductId(), pwq.getWarehouseId()))
                .collect(Collectors.toSet());
        if (!existingLineKeys.equals(payloadLineKeys)) {
            throw new Exception("Adding or removing product lines is not allowed during an outward update. "
                    + "Use Return to reduce an outward.");
        }

        // Validate non-BOQ fields
        boolean allHaveBOQ = validateInputs(iiData, true);

        // BOQ enforcement for update: use delta quantities (newQty - oldQty) to avoid
        // double-counting existing outward quantities that are still in the DB at validation time.
        // Only products with a net increase need to be checked. Aggregated by product (not by
        // product+warehouse) since BOQ is enforced per product+location — a product with two lines
        // (different warehouses) must be compared as one combined old-vs-new total.
        Map<Long, Double> oldQtyMap = new HashMap<>();
        for (InwardOutwardList io : outwardInventory.getInwardOutwardList()) {
            oldQtyMap.merge(io.getProduct().getProductId(), io.getQuantity(), Double::sum);
        }
        Map<Long, Double> newQtyMap = new HashMap<>();
        for (ProductWithQuantity item : iiData.getProductWithQuantities()) {
            newQtyMap.merge(item.getProductId(), item.getQuantity(), Double::sum);
        }
        List<ProductWithQuantity> deltaItems = new ArrayList<>();
        for (Map.Entry<Long, Double> e : newQtyMap.entrySet()) {
            double oldQty = oldQtyMap.getOrDefault(e.getKey(), 0.0);
            double delta  = e.getValue() - oldQty;
            if (delta > 0) {
                deltaItems.add(new ProductWithQuantity(e.getKey(), delta));
            }
        }
        if (!deltaItems.isEmpty()) {
            allHaveBOQ = boqService.enforceBOQLimits(iiData.getUsageLocationId(), deltaItems, iiData.getUsageAreaId());
        }
        exitIfNotAuthorized(outwardInventory, iiData, APICallTypeForAuthorization.Update);
        exitIfReturnExists(outwardInventory, iiData);
        OutwardInventory oldOutwardInventory = (OutwardInventory) outwardInventory.clone();
        reverseBatchConsumptions(id);
        setFields(outwardInventory, iiData);
        outwardInventory.setHasBOQ(allHaveBOQ ? true : null);
        outwardInventory.setHasFifoOverride(false);   // reset — consumeBatchesForOutward re-sets to true if needed
        modifyStockBeforeUpdate(oldOutwardInventory, outwardInventory);
        removeOrphans(oldOutwardInventory);
        outwardInventoryRepo.save(outwardInventory);
        consumeBatchesForOutward(outwardInventory, iiData);
        String updateUser = resolveCurrentUser();
        // Keyed by (productId, warehouseId) — a product can have two lines (different warehouses),
        // each with its own old quantity, so a productId-only map would conflate them.
        Map<String, Double> savedOldQtyMap = oldOutwardInventory.getInwardOutwardList().stream()
                .collect(Collectors.toMap(
                        io -> lineKey(io.getProduct().getProductId(), io.getWarehouse().getWarehouseId()),
                        InwardOutwardList::getQuantity));
        boolean multiWarehouse = outwardInventory.getInwardOutwardList().stream()
                .map(l -> l.getWarehouse().getWarehouseId()).distinct().count() > 1;
        List<Map<String, Object>> changedItems = ActivityLogDescription.list();
        for (InwardOutwardList io : outwardInventory.getInwardOutwardList()) {
            String key = lineKey(io.getProduct().getProductId(), io.getWarehouse().getWarehouseId());
            Double oldQty = savedOldQtyMap.get(key);
            if (oldQty == null || Double.compare(oldQty, io.getQuantity()) != 0) {
                String productName = io.getProduct() != null ? io.getProduct().getProductName() : String.valueOf(io.getProduct().getProductId());
                changedItems.add(multiWarehouse
                        ? ActivityLogDescription.itemChanged(productName, oldQty != null ? oldQty : 0, io.getQuantity(), io.getWarehouse().getWarehouseName())
                        : ActivityLogDescription.itemChanged(productName, oldQty != null ? oldQty : 0, io.getQuantity()));
            }
        }
        if (!changedItems.isEmpty()) {
            activityLogService.record("UPDATED", "OUTWARD", String.valueOf(id),
                    ActivityLogDescription.withItems("Outward " + id + " updated by " + updateUser, changedItems),
                    updateUser);
        } else {
            activityLogService.record("UPDATED", "OUTWARD", String.valueOf(id),
                    ActivityLogDescription.of("Outward " + id + " updated by " + updateUser),
                    updateUser);
        }
        return outwardInventory;

    }

    private void exitIfReturnExists(OutwardInventory outwardInventory, OutwardInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        boolean flag = false;

        if (outwardInventory.getReturnOutwardList().size() > 0) {
            if (outwardInventory.getInwardOutwardList().size() != iiData.getProductWithQuantities().size())
                throw new Exception("Outward inventory entry cannot be edited after return entry is added.");

            // Match by (productId, warehouseId), not productId alone — a product can have two
            // lines (different warehouses), and matching by productId only would let either line
            // compare against the wrong one's quantity.
            Map<String, Double> payloadByLineKey = iiData.getProductWithQuantities().stream()
                    .collect(Collectors.toMap(p -> lineKey(p.getProductId(), p.getWarehouseId()), ProductWithQuantity::getQuantity));

            for (InwardOutwardList ioList : outwardInventory.getInwardOutwardList()) {
                String key = lineKey(ioList.getProduct().getProductId(), ioList.getWarehouse().getWarehouseId());
                Double payloadQty = payloadByLineKey.get(key);
                if (payloadQty == null || !ioList.getQuantity().equals(payloadQty)) {
                    flag = true;
                }
            }

            if (flag)
                throw new Exception("Outward inventory entry cannot be edited after return entry is added.");
        }
    }

    /**
     * Applies stock deltas for an outward edit. Each line now carries its own warehouse, and
     * {@code exitIfNotAuthorized} (Update path) already enforces that the set of (product, warehouse)
     * lines cannot change on edit — only quantities can. That means every line in the new payload has
     * exactly one matching line in the old payload at the same (product, warehouse) key, so this is
     * always a simple per-line quantity delta — no more old/new/common-set reconciliation needed.
     */
    @Transactional(rollbackFor = Exception.class)
    private void modifyStockBeforeUpdate(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory)
            throws Exception {
        log.info("Invoked modifyStockBeforeUpdate");
        Map<String, Double> oldQtyByLineKey = oldOutwardInventory.getInwardOutwardList().stream()
                .collect(Collectors.toMap(
                        l -> lineKey(l.getProduct().getProductId(), l.getWarehouse().getWarehouseId()),
                        InwardOutwardList::getQuantity));

        for (InwardOutwardList newLine : outwardInventory.getInwardOutwardList()) {
            String key = lineKey(newLine.getProduct().getProductId(), newLine.getWarehouse().getWarehouseId());
            Double oldQty = oldQtyByLineKey.getOrDefault(key, 0.0);
            Double newQty = newLine.getQuantity();
            Double delta = newQty - oldQty;
            if (Math.abs(delta) < 0.0001) {
                // No quantity change — still refresh closingStock for display consistency
                Double closingStock = stockService.findStockForProductWarehouse(
                        newLine.getProduct().getProductId(), newLine.getWarehouse().getWarehouseId());
                newLine.setClosingStock(closingStock != null ? closingStock : 0.0);
                continue;
            }
            Double closingStock = stockService.updateStock(newLine.getProduct().getProductId(),
                    newLine.getWarehouse().getWarehouseId(), Math.abs(delta), delta > 0 ? "outward" : "inward");
            inventoryNotificationService.pushQuantityEditedNotification(newLine.getProduct(),
                    newLine.getWarehouse().getWarehouseName(), "outward", closingStock);
            newLine.setClosingStock(closingStock);
        }
        log.info("Exiting modifyStockBeforeUpdate");
    }

    private void setFields(OutwardInventory outwardInventory, OutwardInventoryData oiData) {
        log.info("Invoked setFields");
        outwardInventory.setAdditionalInfo(oiData.getAdditionalInfo());
        outwardInventory.setContractor(contractorRepo.findById(oiData.getContractorId()).get());
        outwardInventory.setUsageLocation(locationRepo.findById(oiData.getUsageLocationId()).get());
        outwardInventory.setUsageArea(usageAreaRepo.findById(oiData.getUsageAreaId()).get());
        outwardInventory.setDate(oiData.getDate());
        outwardInventory.setPurpose(oiData.getPurpose());
        outwardInventory.setSlipNo(oiData.getSlipNo());
        outwardInventory.setRequestedBy(oiData.getRequestedBy());
        outwardInventory.setIssuedBy(oiData.getIssuedBy());
        // Mutate the existing Hibernate-managed collection in place rather than replacing it with a
        // new HashSet. Replacing the field outright causes Hibernate to redundantly re-flush the
        // outwardinventory_entry join-table insert if a second auto-flush happens later in the same
        // transaction (e.g. triggered by the low-stock-check queries below), producing a duplicate-key
        // error on a row it already inserted moments earlier in the same uncommitted transaction.
        outwardInventory.getInwardOutwardList().clear();
        outwardInventory.getInwardOutwardList().addAll(fetchInwardOutwardList(oiData.getProductWithQuantities()));
        outwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(oiData.getFileInformations()));
        log.info("Exited setFields");
    }

    private boolean validateInputs(OutwardInventoryData oiData) throws Exception {
        return validateInputs(oiData, false);
    }

    /**
     * @param skipBoq when true, skips BOQ enforcement (caller handles it separately,
     *                e.g. update path uses delta quantities to avoid double-counting)
     */
    private boolean validateInputs(OutwardInventoryData oiData, boolean skipBoq) throws Exception {
        log.info("Invoked validateInputs");
        if (!locationRepo.existsById(oiData.getUsageLocationId()))
            throw new Exception("Structure not found.");
        if (!contractorRepo.existsById(oiData.getContractorId()))
            throw new Exception("Contractor not found.");
        if (!usageAreaRepo.existsById(oiData.getUsageAreaId()))
            throw new Exception("Work Area not found.");

        if (oiData.getProductWithQuantities() == null || oiData.getProductWithQuantities().isEmpty())
            throw new Exception("Minimum of one product is required to save data.");

        // Each line now carries its own warehouse — validate per line instead of a single header warehouse.
        for (ProductWithQuantity productWithQuantity : oiData.getProductWithQuantities()) {
            if (!productRepo.existsById(productWithQuantity.getProductId()))
                throw new Exception("Product not found.");
            if (productWithQuantity.getWarehouseId() == null)
                throw new Exception("Warehouse is required for every product line.");
            Warehouse lineWarehouse = warehouseRepo.findById(productWithQuantity.getWarehouseId())
                    .orElseThrow(() -> new Exception("Warehouse not found."));
            if (com.ec.application.constants.ProjectConstants.deadStockWarehouseName
                    .equalsIgnoreCase(lineWarehouse.getWarehouseName())) {
                throw new Exception("Outward cannot be created from Dead Stock Warehouse for product '"
                        + lineWarehouse.getWarehouseName() + "'. " +
                        "Use 'Move from Dead Stock' on the Stock page to transfer stock to another warehouse first.");
            }
        }

        // Dedupe by (productId, warehouseId) — same product from two different warehouses in one
        // outward is allowed; the same product/warehouse combination twice is not (combine into one line).
        Long duplicateProductIdCount = oiData.getProductWithQuantities().stream()
                .collect(Collectors.groupingBy(pwq -> lineKey(pwq.getProductId(), pwq.getWarehouseId()), counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Inventory List should be Unique. Same product/warehouse combination added multiple times. Please correct.");

        if (skipBoq) return false;

        // BOQ enforcement: block save if any product exceeds 100% BOQ consumption.
        // Aggregate by product first — a product can now have two lines (different warehouses)
        // whose combined quantity is what matters for BOQ, not each line in isolation.
        // returns true only if ALL products have BOQ configured
        return boqService.enforceBOQLimits(oiData.getUsageLocationId(), aggregateByProduct(oiData.getProductWithQuantities()), oiData.getUsageAreaId());
    }

    public OutwardInventory findOutwardnventory(Long id) throws Exception {
        log.info("Invoked findOutwardnventory");
        Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
        if (outwardInventoryOpt.isPresent() == false)
            throw new Exception("Outward inventory with ID not found");
        OutwardInventory outwardInventory = outwardInventoryOpt.get();
        log.info("Exited findOutwardnventory");
        return outwardInventory;
    }

    public ReturnOutwardInventoryData fetchOutwardnventory(FilterDataList filterDataList, Pageable pageable)
            throws ParseException {
        log.info("Invoked fetchOutwardnventory");
        ReturnOutwardInventoryData returnOutwardInventoryData = new ReturnOutwardInventoryData();

        // Feed data list
        Specification<OutwardInventory> spec = OutwardInventorySpecification.getSpecification(filterDataList);
        if (spec != null)
            returnOutwardInventoryData.setOutwardInventory(outwardInventoryRepo.findAll(spec, pageable));
        else
            returnOutwardInventoryData.setOutwardInventory(outwardInventoryRepo.findAll(pageable));

        // Feed dropdown values
        returnOutwardInventoryData.setIiDropdown(populateDropdownService.fetchData("outward"));

        log.info("Exited fetchOutwardnventory");
        return returnOutwardInventoryData;
    }

    public List<ProductGroupedDAO> getTotalsForOutward(FilterDataList filterDataList) throws ParseException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Specification<OutwardInventory> spec = OutwardInventorySpecification.getSpecification(filterDataList);
        if (spec != null)
            return fetchGroupingForFilteredData(spec);
        else
            return fetchOutwardnventoryGroupBy();
    }

    public List<ProductGroupedDAO> fetchOutwardnventoryGroupBy() throws ParseException {
        log.info("Invoked fetchOutwardnventoryGroupBy");
        List<ProductGroupedDAO> groupedData = outwardInventoryRepo.findGroupByInfo();
        log.info("Exiing fetchOutwardnventoryGroupBy");
        return groupedData;
    }

    private List<ProductGroupedDAO> fetchGroupingForFilteredData(Specification<OutwardInventory> spec) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Map<Pair<String, String>, Double> map = outwardInventoryRepo.findAll(spec).stream()
                .flatMap(i -> i.getInwardOutwardList().stream())
                .collect(Collectors.toMap(l -> Pair.of(l.getProduct().getProductName(), l.getProduct().getMeasurementUnit()),
                        InwardOutwardList::getQuantity,
                        Double::sum));

        List<ProductGroupedDAO> returnData = new ArrayList<>();
        for (Map.Entry<Pair<String, String>, Double> e : map.entrySet()) {
            ProductGroupedDAO rd = new ProductGroupedDAO(
                    e.getKey().getFirst(),
                    e.getKey().getSecond(),
                    e.getValue()
            );
            returnData.add(rd);
        }
        return returnData;
    }

    public List<OutwardInventoryExportDAO2> fetchInwardnventoryForExport2(FilterDataList filterDataList)
            throws Exception {
        log.info("Invoked fetchInwardnventoryForExport2");
        Specification<OutwardInventory> spec = OutwardInventorySpecification.getSpecification(filterDataList);
        long size = spec != null ? outwardInventoryRepo.count(spec) : outwardInventoryRepo.count();
        System.out.println("Size of inward inventory after filter -" + size);
        if (size > 5000)
            throw new Exception("Too many rows to export. Please apply filters to reduce results below 5000 and try again.");
        System.out.println("Fetching data from db");
        List<OutwardInventory> iiData = spec != null ? outwardInventoryRepo.findAll(spec)
                : outwardInventoryRepo.findAll();
        List<OutwardInventoryExportDAO2> clonedData = transformDataForExport(iiData);
        System.out.println("Completed - returning to controller");
        log.info("Exited fetchInwardnventoryForExport2");
        return clonedData;
    }

    private List<OutwardInventoryExportDAO2> transformDataForExport(List<OutwardInventory> iiData) {
        log.info("Invoked transformDataForExport");
        List<OutwardInventoryExportDAO2> transformedData = new ArrayList<OutwardInventoryExportDAO2>();
        for (OutwardInventory ii : iiData) {
            for (InwardOutwardList ioList : ii.getInwardOutwardList()) {
                OutwardInventoryExportDAO2 ied = new OutwardInventoryExportDAO2(ii, ioList);
                transformedData.add(ied);
            }
        }
        log.info("Exiting transformDataForExport");
        return transformedData;
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"boqStatusRows", "boqOutwardQty"}, allEntries = true)
    public void deleteOutwardInventoryById(Long id) throws Exception {
        log.info("Invoked deleteOutwardInventoryById");
        Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
        if (!outwardInventoryOpt.isPresent())
            throw new Exception("Outward Inventory with ID not found");
        OutwardInventory outwardInventory = outwardInventoryOpt.get();
        exitIfNotAuthorized(outwardInventory, null, APICallTypeForAuthorization.Delete);
        reverseBatchConsumptions(id);
        updateStockBeforeDelete(outwardInventory);
        removeOrphans(outwardInventory);
        outwardInventoryRepo.softDeleteById(id);
        String deleteUser = resolveCurrentUser();
        activityLogService.record("DELETED", "OUTWARD", String.valueOf(id),
                ActivityLogDescription.of("Outward " + id + " deleted by " + deleteUser),
                deleteUser);
    }

    @Transactional(rollbackFor = Exception.class)
    private void removeOrphans(OutwardInventory outwardInventory) {
        Set<InwardOutwardList> iolList = outwardInventory.getInwardOutwardList();
        for (InwardOutwardList iol : iolList) {
            iol.setDeleted(true);
            iolRepo.save(iol);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockBeforeDelete(OutwardInventory outwardInventory) throws Exception {
        log.info("Invoked updateStockBeforeDelete");
        for (InwardOutwardList ioList : outwardInventory.getInwardOutwardList()) {
            Long warehouseId = ioList.getWarehouse().getWarehouseId();
            Double stock = ioList.getQuantity();
            stockService.updateStock(ioList.getProduct().getProductId(), warehouseId, stock, "inward");
        }
        log.info("Exiting updateStockBeforeDelete");
    }

    @Transactional(rollbackFor = Exception.class)
    private void exitIfNotAuthorized(OutwardInventory outwardInventory, OutwardInventoryData oiData,
                                     APICallTypeForAuthorization action) throws Exception {

        if (action.equals(APICallTypeForAuthorization.Update)) {
            Long daysDifference = ReusableMethods.daysBetweenTwoDates(outwardInventory.getDate(), new Date());
            Long daysEditAllowed = projectConstantsService.getInventoryEditDaysForCurrentUser();

            if (daysDifference > daysEditAllowed)
                throw new Exception("Cannot edit inventory record with date older than " + daysDifference + " days.");


            if (!oiData.getDate().equals(outwardInventory.getDate()))
                throw new Exception("Date should not be modified while updating outward inventory record");

            // Each line's (product, warehouse) pairing is fixed at creation — only quantities can
            // change on edit. Adding/removing lines or moving a line to a different warehouse is not
            // allowed (mirrors the previous single-warehouse rule, applied per line instead of header).
            if (oiData.getProductWithQuantities().stream().anyMatch(p -> p.getWarehouseId() == null))
                throw new Exception("Warehouse is required for every product line.");

            Set<String> lineKeysInPayload = oiData.getProductWithQuantities().stream()
                    .map(p -> lineKey(p.getProductId(), p.getWarehouseId())).collect(Collectors.toSet());

            Set<String> lineKeysInExistingRecord = outwardInventory.getInwardOutwardList().stream()
                    .map(l -> lineKey(l.getProduct().getProductId(), l.getWarehouse().getWarehouseId()))
                    .collect(Collectors.toSet());

            if (!lineKeysInPayload.equals(lineKeysInExistingRecord))
                throw new Exception("Inventory list and warehouse assignment should not be modified while updating an outward inventory record — only quantities can be changed.");
        }

        if (action.equals(APICallTypeForAuthorization.Create)) {
            Long daysDifference = ReusableMethods.daysBetweenTwoDates(oiData.getDate(), new Date());
            Long daysEditAllowed = projectConstantsService.getInventoryEditDaysForCurrentUser();

            if (daysDifference > daysEditAllowed)
                throw new Exception("Cannot edit inventory record with date older than " + daysEditAllowed + " days.");
        }
        if (action.equals(APICallTypeForAuthorization.Delete)) {
            Long daysDifference = ReusableMethods.daysBetweenTwoDates(outwardInventory.getDate(), new Date());
            Long daysEditAllowed = projectConstantsService.getInventoryEditDaysForCurrentUser();

            if (daysDifference > daysEditAllowed)
                throw new Exception("Cannot DELETE inventory record with date older than " + daysEditAllowed + " days.");
        }
        if (action.equals(APICallTypeForAuthorization.Reject)) {
            Long daysDifference = ReusableMethods.daysBetweenTwoDates(outwardInventory.getDate(), new Date());
            Long daysAllowed = projectConstantsService.getRejectReturnDaysForCurrentUser();

            if (daysDifference > daysAllowed)
                throw new Exception("Cannot add reject/return for record older than " + daysAllowed + " days.");
        }
    }

    public Pageable modifyPageable(Pageable pageable) {
        Sort sort = pageable.getSort();
        Sort newSort = sort;

        for (Sort.Order order : sort) {
            String property = order.getProperty();
            Sort.Direction direction = order.getDirection();
            if (property.equalsIgnoreCase("date")) {
                if (direction == Sort.Direction.ASC) {
                    newSort = Sort.by(Sort.Order.asc("date"), Sort.Order.desc("outwardid"));
                } else if (direction == Sort.Direction.DESC) {
                    newSort = Sort.by(Sort.Order.desc("date"), Sort.Order.asc("outwardid"));
                }
                pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
                break;
            }
        }

        return pageable;
    }

    public Set<InwardOutwardList> fetchInwardOutwardList(List<ProductWithQuantity> productWithQuantities) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();
        for (ProductWithQuantity productWithQuantity : productWithQuantities) {
            InwardOutwardList inwardOutwardList = new InwardOutwardList();
            Product product = productRepo.findById(productWithQuantity.getProductId()).get();
            Warehouse warehouse = warehouseRepo.findById(productWithQuantity.getWarehouseId()).get();
            inwardOutwardList.setProduct(product);
            inwardOutwardList.setQuantity(productWithQuantity.getQuantity());
            inwardOutwardList.setWarehouse(warehouse);
            inwardOutwardListSet.add(inwardOutwardList);
        }
        return inwardOutwardListSet;
    }

    private void consumeBatchesForOutward(OutwardInventory outwardInventory, OutwardInventoryData oiData) {
        boolean anyOverride = false;

        // Keyed by (productId, warehouseId) — a product may appear on two lines if it was
        // outwarded from two different warehouses in the same transaction.
        Map<String, ProductWithQuantity> pwqByLineKey = oiData.getProductWithQuantities().stream()
                .collect(Collectors.toMap(p -> lineKey(p.getProductId(), p.getWarehouseId()), p -> p));

        for (InwardOutwardList iol : outwardInventory.getInwardOutwardList()) {
            Long productId = iol.getProduct().getProductId();
            Long warehouseId = iol.getWarehouse().getWarehouseId();
            Double qtyToConsume = iol.getQuantity();
            ProductWithQuantity pwq = pwqByLineKey.get(lineKey(productId, warehouseId));
            Long overrideBatchId = pwq != null ? pwq.getOverrideBatchId() : null;
            String overrideComment = pwq != null ? pwq.getOverrideComment() : null;

            List<BatchOverrideEntry> overrideBatches = pwq != null ? pwq.getOverrideBatches() : null;
            boolean hasMultiOverride = overrideBatches != null && !overrideBatches.isEmpty();
            boolean hasSingleOverride = overrideBatchId != null && !hasMultiOverride;

            // Untracked stock check — runs for ALL paths (FIFO and override)
            if (iol.getProduct().isBatchTracked()) {
                List<InventoryBatch> availableBatches = iol.getProduct().requiresExpiry()
                        ? inventoryBatchRepository.findAvailableBatchesFifoOrderLocked(productId, warehouseId)
                        : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceivedLocked(productId, warehouseId);
                double totalBatchQty = availableBatches.stream()
                        .mapToDouble(InventoryBatch::getQtyRemaining).sum();
                Double currentStock = stockService.findStockForProductWarehouse(productId, warehouseId);
                double originalStock = (currentStock != null ? currentStock : 0.0) + qtyToConsume;
                double untrackedStock = originalStock - totalBatchQty;
                if (untrackedStock > 0.001) {
                    throw new IllegalArgumentException(
                        "Product '" + iol.getProduct().getProductName() + "' has " +
                        String.format("%.3f", untrackedStock) +
                        " units of untracked stock. Go to Stock → Batches tab and split existing stock into batches first.");
                }
            }

            if (hasMultiOverride || hasSingleOverride) {
                // #10: Skip batch processing entirely for non-batch-tracked products
                if (!iol.getProduct().isBatchTracked()) {
                    log.warn("Override batches ignored for non-batch-tracked product: {}", iol.getProduct().getProductName());
                    continue;
                }

                // Validate reason
                if (overrideComment == null || overrideComment.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "A reason is required when overriding FIFO batch selection for product: " +
                            iol.getProduct().getProductName());
                }

                // Normalise to a list for uniform processing
                List<BatchOverrideEntry> entriesToProcess = new ArrayList<>();
                if (hasMultiOverride) {
                    // #4: Filter out null/zero-qty entries (placeholders not yet filled in)
                    List<BatchOverrideEntry> validEntries = overrideBatches.stream()
                            .filter(e -> e.getQty() != null && e.getQty() > 0)
                            .collect(Collectors.toList());
                    // #3: Validate no duplicate batch IDs
                    Set<Long> seenBatchIds = new HashSet<>();
                    for (BatchOverrideEntry e : validEntries) {
                        if (!seenBatchIds.add(e.getBatchId())) {
                            throw new IllegalArgumentException(
                                "Duplicate batch ID " + e.getBatchId() + " in override list for product: " +
                                iol.getProduct().getProductName());
                        }
                    }
                    double total = validEntries.stream().mapToDouble(BatchOverrideEntry::getQty).sum();
                    if (Math.abs(total - qtyToConsume) > 0.001) {
                        throw new IllegalArgumentException(
                                "Override batch quantities (" + total + ") must equal outward quantity (" +
                                qtyToConsume + ") for product: " + iol.getProduct().getProductName());
                    }
                    entriesToProcess = validEntries;
                } else {
                    BatchOverrideEntry single = new BatchOverrideEntry();
                    single.setBatchId(overrideBatchId);
                    single.setQty(qtyToConsume);
                    entriesToProcess.add(single);
                }

                List<InventoryBatch> fifoBatches = iol.getProduct().requiresExpiry()
                        ? inventoryBatchRepository.findAvailableBatchesFifoOrderLocked(productId, warehouseId)
                        : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceivedLocked(productId, warehouseId);
                List<Long> fifoBatchIds = fifoBatches.stream()
                        .map(InventoryBatch::getBatchId).collect(java.util.stream.Collectors.toList());

                // Pre-compute what pure FIFO would assign to each batch (for quantity-level override detection)
                Map<Long, Double> fifoExpectedQty = new java.util.LinkedHashMap<>();
                double fifoRemaining = qtyToConsume;
                for (InventoryBatch fb : fifoBatches) {
                    if (fifoRemaining <= 0) break;
                    double fifoConsume = Math.min(fifoRemaining, fb.getQtyRemaining());
                    fifoExpectedQty.put(fb.getBatchId(), fifoConsume);
                    fifoRemaining -= fifoConsume;
                }

                int fifoPtr = 0;
                for (BatchOverrideEntry entry : entriesToProcess) {
                    InventoryBatch batch = inventoryBatchRepository.findByIdLocked(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Batch not found with ID: " + entry.getBatchId()));
                    // Validate batch belongs to the correct product
                    if (!batch.getProduct().getProductId().equals(productId)) {
                        throw new IllegalArgumentException(
                            "Batch " + entry.getBatchId() + " belongs to product '"
                            + batch.getProduct().getProductName()
                            + "' but outward line is for a different product.");
                    }
                    // Validate batch belongs to the outward's warehouse
                    if (!batch.getWarehouse().getWarehouseId().equals(warehouseId)) {
                        throw new IllegalArgumentException(
                            "Batch " + entry.getBatchId() + " belongs to warehouse '" +
                            batch.getWarehouse().getWarehouseName() +
                            "' but outward is for a different warehouse.");
                    }
                    if (batch.getQtyRemaining() < entry.getQty()) {
                        throw new IllegalArgumentException(
                                "Batch " + entry.getBatchId() + " does not have enough quantity. " +
                                "Available: " + batch.getQtyRemaining() + ", Requested: " + entry.getQty());
                    }
                    boolean isFifoOrder = fifoPtr < fifoBatchIds.size()
                            && fifoBatchIds.get(fifoPtr).equals(entry.getBatchId());
                    Double expectedQty = fifoExpectedQty.get(entry.getBatchId());
                    boolean isFifoQty = expectedQty != null && Math.abs(expectedQty - entry.getQty()) <= 0.001;
                    boolean isFifoOverride = !isFifoOrder || !isFifoQty;

                    batch.setQtyRemaining(batch.getQtyRemaining() - entry.getQty());
                    inventoryBatchRepository.save(batch);

                    OutwardBatchConsumption consumption = new OutwardBatchConsumption();
                    consumption.setOutwardId(outwardInventory.getOutwardid());
                    consumption.setBatch(batch);
                    consumption.setProductId(productId);
                    consumption.setWarehouseId(warehouseId);
                    consumption.setQtyConsumed(entry.getQty());
                    consumption.setFifoOverridden(isFifoOverride);
                    consumption.setOverrideComment(isFifoOverride ? overrideComment : null);
                    outwardBatchConsumptionRepository.save(consumption);

                    if (isFifoOverride) anyOverride = true;
                    fifoPtr++;
                }
            } else {
                // True FIFO/FEFO: auto-consume oldest batches first
                List<InventoryBatch> fifoBatches = iol.getProduct().requiresExpiry()
                        ? inventoryBatchRepository.findAvailableBatchesFifoOrderLocked(productId, warehouseId)
                        : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceivedLocked(productId, warehouseId);

                double totalBatchQty = fifoBatches.stream()
                        .mapToDouble(InventoryBatch::getQtyRemaining).sum();

                double preFeatureStock = Math.max(qtyToConsume - totalBatchQty, 0.0);
                double remaining = qtyToConsume - preFeatureStock;

                for (InventoryBatch batch : fifoBatches) {
                    if (remaining <= 0) break;
                    double consume = Math.min(remaining, batch.getQtyRemaining());
                    batch.setQtyRemaining(batch.getQtyRemaining() - consume);
                    inventoryBatchRepository.save(batch);

                    OutwardBatchConsumption consumption = new OutwardBatchConsumption();
                    consumption.setOutwardId(outwardInventory.getOutwardid());
                    consumption.setBatch(batch);
                    consumption.setProductId(productId);
                    consumption.setWarehouseId(warehouseId);
                    consumption.setQtyConsumed(consume);
                    consumption.setFifoOverridden(false);
                    outwardBatchConsumptionRepository.save(consumption);

                    remaining -= consume;
                }
                // Any pre-feature stock is silently consumed (no batch record) — only for non-expirable products
            }
        }

        if (anyOverride) {
            outwardInventory.setHasFifoOverride(true);
            outwardInventoryRepo.save(outwardInventory);
        }
    }

    // Reverse all batch consumptions for an outward — restores qtyRemaining on each batch (#4)
    private void reverseBatchConsumptions(Long outwardId) {
        List<OutwardBatchConsumption> consumptions =
                outwardBatchConsumptionRepository.findByOutwardIdOrderByIdAsc(outwardId);
        for (OutwardBatchConsumption c : consumptions) {
            InventoryBatch batch = c.getBatch();
            batch.setQtyRemaining(batch.getQtyRemaining() + c.getQtyConsumed());
            inventoryBatchRepository.save(batch);
        }
        outwardBatchConsumptionRepository.deleteByOutwardId(outwardId);
    }

    public com.ec.application.data.OutwardTilesDTO getTiles() {
        com.ec.application.data.OutwardTilesDTO dto = new com.ec.application.data.OutwardTilesDTO();
        dto.setNoBoqCount(outwardInventoryRepo.countNoBOQ());
        dto.setFifoOverrideCount(outwardInventoryRepo.countFifoOverride());
        dto.setRejectCount(outwardInventoryRepo.countWithReject());
        dto.setReturnCount(outwardInventoryRepo.countWithReturn());

        Calendar weekCal = Calendar.getInstance();
        weekCal.setFirstDayOfWeek(Calendar.MONDAY);
        weekCal.set(Calendar.DAY_OF_WEEK, weekCal.getFirstDayOfWeek());
        weekCal.set(Calendar.HOUR_OF_DAY, 0);
        weekCal.set(Calendar.MINUTE, 0);
        weekCal.set(Calendar.SECOND, 0);
        weekCal.set(Calendar.MILLISECOND, 0);
        dto.setThisWeekCount(outwardInventoryRepo.countSince(weekCal.getTime()));

        Calendar monthCal = Calendar.getInstance();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        monthCal.set(Calendar.HOUR_OF_DAY, 0);
        monthCal.set(Calendar.MINUTE, 0);
        monthCal.set(Calendar.SECOND, 0);
        monthCal.set(Calendar.MILLISECOND, 0);
        dto.setThisMonthCount(outwardInventoryRepo.countSince(monthCal.getTime()));

        return dto;
    }
}
