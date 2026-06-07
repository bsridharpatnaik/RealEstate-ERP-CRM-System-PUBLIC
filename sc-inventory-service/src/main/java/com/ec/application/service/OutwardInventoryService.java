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
        List<Map<String, Object>> createItems = new ArrayList<>();
        for (InwardOutwardList io : outwardInventory.getInwardOutwardList())
            createItems.add(ActivityLogDescription.item(io.getProduct().getProductName(), io.getQuantity()));
        activityLogService.record("CREATED", "OUTWARD", String.valueOf(outwardInventory.getOutwardid()),
                ActivityLogDescription.withItems("Outward " + outwardInventory.getOutwardid() + " created by " + createUser, createItems),
                createUser);
        return outwardInventory;
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockForCreateOutwardInventory(OutwardInventory outwardInventory) throws Exception {
        log.info("Invoked updateStockForCreateOutwardInventory");
        Set<InwardOutwardList> productsWithQuantities = outwardInventory.getInwardOutwardList();
        Long warehouseId = outwardInventory.getWarehouse().getWarehouseId();

        for (InwardOutwardList oiList : productsWithQuantities) {
            Long productId = oiList.getProduct().getProductId();
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
        Long duplicateProductIdCount = rd.getProductWithQuantities().stream()
                .collect(Collectors.groupingBy(ProductWithQuantity::getProductId, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Inventory List should be Unique. Same product added multiple times. Please correct.");

        for (ProductWithQuantity productWithQuantity : rd.getProductWithQuantities()) {
            if (productWithQuantity.getQuantity() == null || productWithQuantity.getProductId() == null
                    || !productRepo.existsById(productWithQuantity.getProductId()))
                throw new Exception("Error fetching product details");
            if (type.equals("return"))
                addReturnForOutward(outwardId, productWithQuantity);
            else
                addRejectForOutward(outwardId, productWithQuantity.getProductId(), productWithQuantity.getQuantity(),
                        productWithQuantity.getRemarks());
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
        for (InwardOutwardList inwardOutwardList : inwardOutwardListSet) {
            if (inwardOutwardList.getProduct().getProductId().equals(productId)) {
                Double currentQuantity = inwardOutwardList.getQuantity();
                if (quantity > currentQuantity)
                    throw new Exception(
                            "Return quantity cannot be greater than existing quantity for product -"
                                    + inwardOutwardList.getProduct().getProductName());

                Double diffInQuantity = currentQuantity - quantity;
                Double closingStock = stockService.updateStock(productId, oi.getWarehouse().getWarehouseId(),
                        quantity, "inward");
                returnOutwardList.add(new ReturnOutwardList(new Date(), inwardOutwardList.getProduct(), currentQuantity,
                        quantity, closingStock));
                inwardOutwardList.setQuantity(diffInQuantity);
                inwardOutwardList.setClosingStock(closingStock);
            }
        }
        oi.setReturnOutwardList(returnOutwardList);
        oi.setInwardOutwardList(inwardOutwardListSet);
        outwardInventoryRepo.save(oi);

        // Restore batch quantities
        restoreBatchesForReturn(outwardId, productId, quantity, returnBatches);
    }

    /**
     * Restore batches after a return.
     * If returnBatches is provided (multi-batch scenario), restore each specified batch.
     * Otherwise, auto-restore from OutwardBatchConsumption records (single-batch scenario).
     */
    private void restoreBatchesForReturn(Long outwardId, Long productId, Double quantity,
                                         List<BatchOverrideEntry> returnBatches) {
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
                if (consumptions.isEmpty()) return;

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
                if (totalConsumed <= 0) return;

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
        } catch (Exception e) {
            // #8: Rethrow — don't silently swallow; stock/batch divergence would corrupt future outwards
            log.error("Batch restore failed for return outwardId={}, productId={}: {}", outwardId, productId, e.getMessage(), e);
            throw new RuntimeException("Failed to restore batch quantities after return: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void addRejectForOutward(Long outwardId, Long productId, Double quantity, String remarks) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        OutwardInventory oi = outwardInventoryRepo.findById(outwardId).get();
        exitIfNotAuthorized(oi, null, APICallTypeForAuthorization.Reject);
        Set<RejectOutwardList> rejectOutwardList = oi.getRejectOutwardList();
        Set<InwardOutwardList> inwardOutwardListSet = oi.getInwardOutwardList();
        for (InwardOutwardList inwardOutwardList : inwardOutwardListSet) {
            if (inwardOutwardList.getProduct().getProductId().equals(productId)) {
                Double currentQuantity = inwardOutwardList.getQuantity();
                if (quantity > currentQuantity)
                    throw new Exception(
                            "Reject quantity cannot be greater than existing quantity for product -"
                                    + inwardOutwardList.getProduct().getProductName());

                rejectOutwardList.add(new RejectOutwardList(new Date(), inwardOutwardList.getProduct(), currentQuantity,
                        quantity, remarks));

                // Decrement line qty so the same qty cannot be rejected again on a subsequent call
                inwardOutwardList.setQuantity(currentQuantity - quantity);
            }
        }
        oi.setRejectOutwardList(rejectOutwardList);
        oi.setInwardOutwardList(inwardOutwardListSet);
        outwardInventoryRepo.save(oi);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"boqStatusRows", "boqOutwardQty"}, allEntries = true)
    public OutwardInventory updateOutwardnventory(OutwardInventoryData iiData, Long id) throws Exception {
        log.info("Invoked updateOutwardnventory");
        Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
        if (!outwardInventoryOpt.isPresent())
            throw new Exception("Inventory Entry with ID not found");
        OutwardInventory outwardInventory = outwardInventoryOpt.get();

        // Validate non-BOQ fields
        boolean allHaveBOQ = validateInputs(iiData, true);

        // BOQ enforcement for update: use delta quantities (newQty - oldQty) to avoid
        // double-counting existing outward quantities that are still in the DB at validation time.
        // Only products with a net increase need to be checked.
        Map<Long, Double> oldQtyMap = new HashMap<>();
        for (InwardOutwardList io : outwardInventory.getInwardOutwardList()) {
            oldQtyMap.put(io.getProduct().getProductId(), io.getQuantity());
        }
        List<ProductWithQuantity> deltaItems = new ArrayList<>();
        for (ProductWithQuantity item : iiData.getProductWithQuantities()) {
            double oldQty   = oldQtyMap.getOrDefault(item.getProductId(), 0.0);
            double delta    = item.getQuantity() - oldQty;
            if (delta > 0) {
                deltaItems.add(new ProductWithQuantity(item.getProductId(), delta));
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
        Map<Long, Double> savedOldQtyMap = oldOutwardInventory.getInwardOutwardList().stream()
                .collect(Collectors.toMap(io -> io.getProduct().getProductId(), InwardOutwardList::getQuantity, (a, b) -> a));
        List<Map<String, Object>> changedItems = ActivityLogDescription.list();
        for (InwardOutwardList io : outwardInventory.getInwardOutwardList()) {
            Double oldQty = savedOldQtyMap.get(io.getProduct().getProductId());
            if (oldQty == null || Double.compare(oldQty, io.getQuantity()) != 0) {
                String productName = io.getProduct() != null ? io.getProduct().getProductName() : String.valueOf(io.getProduct().getProductId());
                changedItems.add(ActivityLogDescription.itemChanged(productName, oldQty != null ? oldQty : 0, io.getQuantity()));
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

            for (InwardOutwardList ioList : outwardInventory.getInwardOutwardList()) {
                boolean isFound = false;
                for (ProductWithQuantity pwq : iiData.getProductWithQuantities()) {
                    if (ioList.getProduct().getProductId().equals(pwq.getProductId())) {
                        isFound = true;
                        if (!ioList.getQuantity().equals(pwq.getQuantity())) {
                            flag = true;
                        }
                    }
                }
                if (!isFound)
                    flag = true;
            }

            if (flag)
                throw new Exception("Outward inventory entry cannot be edited after return entry is added.");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateWhenWarehouseSame(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory)
            throws Exception {
        log.info("Invoked updateWhenWarehouseSame");
        // Fetch product only in old and only in new and common
        Set<Long> oldProductSet = new HashSet<>(oldOutwardInventory.getInwardOutwardList().size());
        Set<Long> newProductSet = new HashSet<>(outwardInventory.getInwardOutwardList().size());
        oldOutwardInventory.getInwardOutwardList().stream()
                .filter(p -> oldProductSet.add(p.getProduct().getProductId())).collect(Collectors.toList());
        outwardInventory.getInwardOutwardList().stream().filter(p -> newProductSet.add(p.getProduct().getProductId()))
                .collect(Collectors.toList());
        Set<Long> onlyInOld = ReusableMethods.differenceBetweenSets(oldProductSet, newProductSet);
        Set<Long> onlyInNew = ReusableMethods.differenceBetweenSets(newProductSet, oldProductSet);
        Set<Long> commonInBoth = ReusableMethods.commonBetweenSets(oldProductSet, newProductSet);
        updateStockForOnlyInOld(onlyInOld, oldOutwardInventory);
        updateStockForOnlyInNew(onlyInNew, outwardInventory);
        updateStockForCommonInBoth(commonInBoth, oldOutwardInventory, outwardInventory);
        log.info("Exiting updateWhenWarehouseSame");
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockForCommonInBoth(Set<Long> commonInBoth, OutwardInventory oldOutwardInventory,
                                            OutwardInventory outwardInventory) throws Exception {
        log.info("Invoked updateWhenWarehouseSame");
        Set<InwardOutwardList> oldIOListSet = oldOutwardInventory.getInwardOutwardList();
        Set<InwardOutwardList> newIOListSet = outwardInventory.getInwardOutwardList();
        for (Long id : commonInBoth) {
            Double oldQuantity = findQuantityForProductInIOList(id, oldIOListSet);
            Double newQuantity = findQuantityForProductInIOList(id, newIOListSet);
            Double quantityForUpdate = newQuantity - oldQuantity;
            for (InwardOutwardList ioList : newIOListSet) {
                if (id.equals(ioList.getProduct().getProductId())) {
                    Double closingStock = stockService.updateStock(id,
                            outwardInventory.getWarehouse().getWarehouseId(), quantityForUpdate, "outward");
                    System.out.println("Closing stock - " + closingStock);
                    inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
                            outwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
                    ioList.setClosingStock(closingStock);
                }
            }
            outwardInventory.setInwardOutwardList(newIOListSet);
        }
        log.info("Exiting updateWhenWarehouseSame");
    }

    @Transactional(rollbackFor = Exception.class)
    private Double findQuantityForProductInIOList(Long productId, Set<InwardOutwardList> ioListSet) {
        log.info("Invoked findQuantityForProductInIOList");
        for (InwardOutwardList ioList : ioListSet) {
            if (productId.equals(ioList.getProduct().getProductId())) {
                log.info("Old Quantity in findQuantityForProductInIOList - " + ioList.getQuantity());
                Double oldQuantity = ioList.getQuantity();
                log.info("Exiting findQuantityForProductInIOList");
                return oldQuantity;
            }
        }
        log.info("Exiting findQuantityForProductInIOList with return as null");
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockForOnlyInNew(Set<Long> onlyInNew, OutwardInventory outwardInventory) throws Exception {
        log.info("Invoked updateStockForOnlyInNew");
        for (Long id : onlyInNew) {
            Set<InwardOutwardList> ioListSet = outwardInventory.getInwardOutwardList();
            for (InwardOutwardList ioList : ioListSet) {
                if (id.equals(ioList.getProduct().getProductId())) {
                    Double quantity = ioList.getQuantity();
                    Double closingStock = stockService.updateStock(id,
                            outwardInventory.getWarehouse().getWarehouseId(), quantity, "outward");
                    inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
                            outwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
                    ioList.setClosingStock(closingStock);
                }
            }
            outwardInventory.setInwardOutwardList(ioListSet);

        }
        log.info("Exiting updateStockForOnlyInNew");
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockForOnlyInOld(Set<Long> onlyInOld, OutwardInventory oldOutwardInventory) throws Exception {
        log.info("Invoked updateStockForOnlyInOld");
        // Delete stock received as part of old inventory
        for (Long id : onlyInOld) {
            Set<InwardOutwardList> ioListSet = oldOutwardInventory.getInwardOutwardList();
            for (InwardOutwardList ioList : ioListSet) {
                if (id.equals(ioList.getProduct().getProductId())) {
                    Double quantity = ioList.getQuantity();
                    Double closingStock = stockService.updateStock(id,
                            oldOutwardInventory.getWarehouse().getWarehouseId(), quantity, "inward");
                    inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
                            oldOutwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
                }
            }
        }
        log.info("Exiting updateStockForOnlyInOld");
    }

    @Transactional(rollbackFor = Exception.class)
    private void modifyStockBeforeUpdate(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory)
            throws Exception {
        log.info("Invoked modifyStockBeforeUpdate");
        if (!oldOutwardInventory.getWarehouse().getWarehouseId()
                .equals(outwardInventory.getWarehouse().getWarehouseId()))
            updateWhenWarehouseChanged(oldOutwardInventory, outwardInventory);
        else
            updateWhenWarehouseSame(oldOutwardInventory, outwardInventory);
        log.info("Exiting modifyStockBeforeUpdate");
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateWhenWarehouseChanged(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory)
            throws Exception {
        log.info("Invoked updateWhenWarehouseChanged");
        // Delete all stock added as part of old warehouse
        traverseListAndUpdateStock(oldOutwardInventory.getInwardOutwardList(), "inward",
                oldOutwardInventory.getWarehouse());

        // Add new stock to new warehouse
        Set<InwardOutwardList> newLIOList = traverseListAndUpdateStock(outwardInventory.getInwardOutwardList(),
                "outward", outwardInventory.getWarehouse());
        outwardInventory.setInwardOutwardList(newLIOList);
        log.info("Existing updateWhenWarehouseChanged");
    }

    @Transactional(rollbackFor = Exception.class)
    private Set<InwardOutwardList> traverseListAndUpdateStock(Set<InwardOutwardList> ioListset, String type,
                                                              Warehouse warehouse) throws Exception {
        log.info("Invoked traverseListAndUpdateStock");
        for (InwardOutwardList oiList : ioListset) {
            Double closingStock = stockService.updateStock(oiList.getProduct().getProductId(),
                    warehouse.getWarehouseId(), oiList.getQuantity(), type);
            inventoryNotificationService.pushQuantityEditedNotification(oiList.getProduct(),
                    warehouse.getWarehouseName(), "outward", closingStock);
            oiList.setClosingStock(closingStock);

        }
        log.info("Exiting traverseListAndUpdateStock");
        return ioListset;
    }

    private void setFields(OutwardInventory outwardInventory, OutwardInventoryData oiData) {
        log.info("Invoked setFields");
        Warehouse warehouse = warehouseRepo.findById(oiData.getWarehouseId()).get();
        outwardInventory.setAdditionalInfo(oiData.getAdditionalInfo());
        outwardInventory.setContractor(contractorRepo.findById(oiData.getContractorId()).get());
        outwardInventory.setUsageLocation(locationRepo.findById(oiData.getUsageLocationId()).get());
        outwardInventory.setUsageArea(usageAreaRepo.findById(oiData.getUsageAreaId()).get());
        outwardInventory.setWarehouse(warehouse);
        ;
        outwardInventory.setDate(oiData.getDate());
        outwardInventory.setPurpose(oiData.getPurpose());
        outwardInventory.setSlipNo(oiData.getSlipNo());
        outwardInventory.setInwardOutwardList(fetchInwardOutwardList(oiData.getProductWithQuantities(), warehouse));
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
        if (!warehouseRepo.existsById(oiData.getWarehouseId()))
            throw new Exception("Warehouse not found.");
        if (!usageAreaRepo.existsById(oiData.getUsageAreaId()))
            throw new Exception("Work Area not found.");

        Long duplicateProductIdCount = oiData.getProductWithQuantities().stream()
                .collect(Collectors.groupingBy(ProductWithQuantity::getProductId, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Inventory List should be Unique. Same product added multiple times. Please correct.");
        for (ProductWithQuantity productWithQuantity : oiData.getProductWithQuantities()) {
            if (!productRepo.existsById(productWithQuantity.getProductId()))
                throw new Exception("Product not found.");
        }

        if (skipBoq) return false;

        // BOQ enforcement: block save if any product exceeds 100% BOQ consumption
        // returns true only if ALL products have BOQ configured
        return boqService.enforceBOQLimits(oiData.getUsageLocationId(), oiData.getProductWithQuantities(), oiData.getUsageAreaId());
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

    /*
     * public List<OutwardInventoryExportDAO>
     * fetchOutwardnventoryForExport(FilterDataList filterDataList) throws Exception
     * { log.info("Invoked fetchOutwardnventoryForExport");
     * Specification<OutwardInventory> spec =
     * OutwardInventorySpecification.getSpecification(filterDataList); long size =
     * spec != null ? outwardInventoryRepo.count(spec) :
     * outwardInventoryRepo.count(); if (size > 2000) throw new
     * Exception("Too many rows to export. Apply some more filters and try again");
     * List<OutwardInventory> iiData = spec != null ?
     * outwardInventoryRepo.findAll(spec) : outwardInventoryRepo.findAll();
     * List<OutwardInventoryExportDAO> clonedData =
     * iiData.parallelStream().map(OutwardInventoryExportDAO::new)
     * .collect(Collectors.toList());
     * log.info("Exited fetchOutwardnventoryForExport"); return clonedData; }
     */

    public List<OutwardInventoryExportDAO2> fetchInwardnventoryForExport2(FilterDataList filterDataList)
            throws Exception {
        log.info("Invoked fetchInwardnventoryForExport2");
        Specification<OutwardInventory> spec = OutwardInventorySpecification.getSpecification(filterDataList);
        long size = spec != null ? outwardInventoryRepo.count(spec) : outwardInventoryRepo.count();
        System.out.println("Size of inward inventory after filter -" + size);
        if (size > 2000)
            throw new Exception("Too many rows to export. Apply some more filters and try again");
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
        Long warehouseId = outwardInventory.getWarehouse().getWarehouseId();
        for (InwardOutwardList ioList : outwardInventory.getInwardOutwardList()) {
            Double stock = ioList.getQuantity();
            Double currentStock = stockRepo
                    .findStockForProductAndWarehouse(ioList.getProduct().getProductId(), warehouseId).get(0)
                    .getQuantityInHand();
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

            if (!oiData.getWarehouseId().equals(outwardInventory.getWarehouse().getWarehouseId()))
                throw new Exception("Warehouse should not be modified while updating outward inventory record");
            List<Long> productsInPayload = oiData.getProductWithQuantities().stream()
                    .map(ProductWithQuantity::getProductId).collect(Collectors.toList());

            List<Long> productsInExistingRecord = outwardInventory.getInwardOutwardList().stream()
                    .map(InwardOutwardList::getProduct).map(Product::getProductId).collect(Collectors.toList());
            if (!(productsInPayload.containsAll(productsInExistingRecord)
                    && productsInPayload.size() == productsInExistingRecord.size()))
                throw new Exception("Inventory List should not be modified while updating an outward inventory record");
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

    public Set<InwardOutwardList> fetchInwardOutwardList(List<ProductWithQuantity> productWithQuantities,
                                                         Warehouse warehouse) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();
        for (ProductWithQuantity productWithQuantity : productWithQuantities) {
            InwardOutwardList inwardOutwardList = new InwardOutwardList();
            Product product = productRepo.findById(productWithQuantity.getProductId()).get();
            inwardOutwardList.setProduct(product);
            inwardOutwardList.setQuantity(productWithQuantity.getQuantity());
            inwardOutwardListSet.add(inwardOutwardList);
            inwardOutwardList.setWarehouse(warehouse);
        }
        return inwardOutwardListSet;
    }

    private void consumeBatchesForOutward(OutwardInventory outwardInventory, OutwardInventoryData oiData) {
        Long warehouseId = outwardInventory.getWarehouse().getWarehouseId();
        boolean anyOverride = false;

        Map<Long, ProductWithQuantity> pwqByProductId = oiData.getProductWithQuantities().stream()
                .collect(Collectors.toMap(ProductWithQuantity::getProductId, p -> p));

        for (InwardOutwardList iol : outwardInventory.getInwardOutwardList()) {
            Long productId = iol.getProduct().getProductId();
            Double qtyToConsume = iol.getQuantity();
            ProductWithQuantity pwq = pwqByProductId.get(productId);
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
}
