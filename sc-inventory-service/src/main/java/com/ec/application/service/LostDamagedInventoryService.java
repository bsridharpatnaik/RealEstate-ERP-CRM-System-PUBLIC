package com.ec.application.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.application.ReusableClasses.ActivityLogDescription;
import com.ec.application.ReusableClasses.ReusableMethods;
import java.util.Collections;
import com.ec.application.data.BatchOverrideEntry;
import com.ec.application.data.CreateLostOrDamagedInventoryData;
import com.ec.application.data.LostDamagedReturnData;
import com.ec.application.model.InventoryBatch;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.model.Product;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.InventoryBatchRepository;
import com.ec.application.repository.LostDamagedInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.LostDamagedInventorySpecification;

@Service
@Transactional
public class LostDamagedInventoryService {
    @Autowired
    LostDamagedInventoryRepo lostDamagedInventoryRepo;

    @Autowired
    ProductRepo productRepo;

    @Autowired
    ProductService productService;

    @Autowired
    StockService stockService;

    @Autowired
    StockRepo stockRepo;

    @Autowired
    WarehouseRepo warehouseRepo;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @Autowired
    InventoryNotificationService inventoryNotificationService;

    @Autowired
    InventoryBatchRepository inventoryBatchRepository;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    ActivityLogService activityLogService;

    Logger log = LoggerFactory.getLogger(LostDamagedInventoryService.class);

    @Transactional(rollbackFor = Exception.class)
    public LostDamagedInventory createData(CreateLostOrDamagedInventoryData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        LostDamagedInventory lostDamagedInventory = new LostDamagedInventory();
        validatePayload(payload);
        populateData(lostDamagedInventory, payload);
        Double closingStock = adjustStockBeforeCreate(payload);
        lostDamagedInventory.setClosingStock(closingStock);

        // Batch handling
        InventoryBatch batchRef = handleBatchOnCreate(lostDamagedInventory, payload);
        lostDamagedInventory.setBatch(batchRef);

        String addedNotifType = "EXCESS_FOUND".equals(lostDamagedInventory.getEntryType())
                ? "excessfoundadded" : "lostdamagedadded";
        inventoryNotificationService.pushQuantityEditedNotification(lostDamagedInventory.getProduct(),
                lostDamagedInventory.getWarehouse().getWarehouseName(), addedNotifType,
                lostDamagedInventory.getQuantity());
        LostDamagedInventory saved = lostDamagedInventoryRepo.save(lostDamagedInventory);
        String createUser = resolveCurrentUser();
        String createProduct = saved.getProduct() != null ? saved.getProduct().getProductName() : "Unknown";
        activityLogService.record("CREATED", saved.getEntryType(), String.valueOf(saved.getLostdamagedid()),
                ActivityLogDescription.withItems(saved.getEntryType() + " entry " + saved.getLostdamagedid()
                        + " created by " + createUser,
                        Collections.singletonList(ActivityLogDescription.item(createProduct, saved.getQuantity()))),
                createUser);
        return saved;
    }

    /**
     * Handles batch side-effects on create:
     * - LOST_DAMAGED + batchEntries (multi): drain each listed batch by its qty; validate sum == total.
     * - LOST_DAMAGED + batchId (single, legacy): drain that batch.
     * - EXCESS_FOUND + excessBatchEntries (multi): add qty to each listed existing batch.
     * - EXCESS_FOUND + existingBatchId (single): add qty to that batch.
     * - EXCESS_FOUND + no existing batch: create a new InventoryBatch.
     */
    private InventoryBatch handleBatchOnCreate(LostDamagedInventory entity,
                                                CreateLostOrDamagedInventoryData payload) {
        // Always clear stale JSON from a previous save (important on update path)
        entity.setBatchEntriesJson(null);

        Product product = entity.getProduct();
        if (product == null || !product.isBatchTracked()) return null;

        // Block LOST_DAMAGED if untracked stock exists — same guard as outward
        if ("LOST_DAMAGED".equals(payload.getEntryType())) {
            Long productId = product.getProductId();
            Long warehouseId = entity.getWarehouse().getWarehouseId();
            List<InventoryBatch> availableBatches = product.requiresExpiry()
                    ? inventoryBatchRepository.findAvailableBatchesFifoOrder(productId, warehouseId)
                    : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceived(productId, warehouseId);
            double totalBatchQty = availableBatches.stream()
                    .mapToDouble(InventoryBatch::getQtyRemaining).sum();
            Double currentStock = stockService.findStockForProductWarehouse(productId, warehouseId);
            double stock = currentStock != null ? currentStock : 0.0;
            double untrackedStock = stock - totalBatchQty;
            if (untrackedStock > 0.001) {
                throw new IllegalArgumentException(
                    "Product '" + product.getProductName() + "' has "
                    + String.format("%.3f", untrackedStock)
                    + " units of untracked stock in this warehouse. "
                    + "Go to Stock → Batches tab and split existing stock into batches first.");
            }
        }

        if ("LOST_DAMAGED".equals(payload.getEntryType())) {
            List<BatchOverrideEntry> entries = payload.getBatchEntries();
            if (entries != null && !entries.isEmpty()) {
                // Filter to valid entries first — prevents null batchId entries from passing sum check
                List<BatchOverrideEntry> validEntries = entries.stream()
                        .filter(e -> e.getBatchId() != null && e.getQty() != null && e.getQty() > 0)
                        .collect(java.util.stream.Collectors.toList());
                if (validEntries.isEmpty()) {
                    throw new IllegalArgumentException("No valid batch entries provided for lost/damaged.");
                }
                // Duplicate batchId check
                java.util.Set<Long> seenIds = new java.util.HashSet<>();
                for (BatchOverrideEntry e : validEntries) {
                    if (!seenIds.add(e.getBatchId())) {
                        throw new IllegalArgumentException("Duplicate batch ID " + e.getBatchId() + " in lost/damaged entries.");
                    }
                }
                double total = validEntries.stream().mapToDouble(BatchOverrideEntry::getQty).sum();
                if (Math.abs(total - payload.getQuantity()) > 0.001) {
                    throw new IllegalArgumentException(
                            "Sum of batch quantities (" + total + ") does not match total quantity (" + payload.getQuantity() + ").");
                }
                // Validate ownership + availability before any saves
                Long warehouseId = entity.getWarehouse().getWarehouseId();
                for (BatchOverrideEntry entry : validEntries) {
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                    if (!batch.getProduct().getProductId().equals(product.getProductId())) {
                        throw new IllegalArgumentException("Batch #" + entry.getBatchId()
                                + " belongs to product '" + batch.getProduct().getProductName()
                                + "', not the selected product.");
                    }
                    if (!batch.getWarehouse().getWarehouseId().equals(warehouseId)) {
                        throw new IllegalArgumentException("Batch #" + entry.getBatchId()
                                + " belongs to warehouse '" + batch.getWarehouse().getWarehouseName()
                                + "', not the selected warehouse.");
                    }
                    if (entry.getQty() > batch.getQtyRemaining() + 0.001) {
                        throw new IllegalArgumentException("Batch " + entry.getBatchId()
                                + " only has " + String.format("%.3f", batch.getQtyRemaining()) + " units remaining.");
                    }
                }
                // All validations passed — apply
                InventoryBatch firstBatch = null;
                List<BatchOverrideEntry> saved = new ArrayList<>();
                for (BatchOverrideEntry entry : validEntries) {
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                    batch.setQtyRemaining(batch.getQtyRemaining() - entry.getQty());
                    inventoryBatchRepository.save(batch);
                    BatchOverrideEntry enriched = new BatchOverrideEntry();
                    enriched.setBatchId(entry.getBatchId());
                    enriched.setQty(entry.getQty());
                    enriched.setBrand(batch.getBrand());
                    enriched.setLotNumber(batch.getLotNumber());
                    if (batch.getExpiryDate() != null) {
                        enriched.setExpiryDate(
                            new java.text.SimpleDateFormat("dd-MM-yyyy").format(batch.getExpiryDate()));
                    }
                    saved.add(enriched);
                    if (firstBatch == null) firstBatch = batch;
                }
                try {
                    entity.setBatchEntriesJson(
                            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(saved));
                } catch (Exception e) { log.warn("Failed to serialize batchEntriesJson: " + e.getMessage()); }
                return firstBatch;
            }
            // Single batch path (backward compat / edit from old record)
            if (payload.getBatchId() == null) {
                throw new IllegalArgumentException(
                        "This product is batch-tracked. Please select the batch the loss came from.");
            }
            InventoryBatch batch = inventoryBatchRepository.findById(payload.getBatchId())
                    .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + payload.getBatchId()));
            if (!batch.getProduct().getProductId().equals(product.getProductId())) {
                throw new IllegalArgumentException("Batch #" + payload.getBatchId()
                        + " belongs to a different product.");
            }
            if (!batch.getWarehouse().getWarehouseId().equals(entity.getWarehouse().getWarehouseId())) {
                throw new IllegalArgumentException("Batch #" + payload.getBatchId()
                        + " belongs to a different warehouse.");
            }
            if (payload.getQuantity() > batch.getQtyRemaining() + 0.001) {
                throw new IllegalArgumentException("Cannot mark " + payload.getQuantity()
                        + " as lost/damaged. Batch only has " + String.format("%.3f", batch.getQtyRemaining()) + " units remaining.");
            }
            batch.setQtyRemaining(batch.getQtyRemaining() - payload.getQuantity());
            return inventoryBatchRepository.save(batch);
        }

        if ("EXCESS_FOUND".equals(payload.getEntryType())) {
            List<BatchOverrideEntry> excessEntries = payload.getExcessBatchEntries();
            if (excessEntries != null && !excessEntries.isEmpty()) {
                // Filter valid entries first — prevents null batchId entries from passing sum check
                List<BatchOverrideEntry> validExcess = excessEntries.stream()
                        .filter(e -> e.getBatchId() != null && e.getQty() != null && e.getQty() > 0)
                        .collect(java.util.stream.Collectors.toList());
                if (validExcess.isEmpty()) {
                    throw new IllegalArgumentException("No valid batch entries provided for excess found.");
                }
                // Duplicate batchId check
                java.util.Set<Long> seenExcessIds = new java.util.HashSet<>();
                for (BatchOverrideEntry e : validExcess) {
                    if (!seenExcessIds.add(e.getBatchId())) {
                        throw new IllegalArgumentException("Duplicate batch ID " + e.getBatchId() + " in excess found entries.");
                    }
                }
                double total = validExcess.stream().mapToDouble(BatchOverrideEntry::getQty).sum();
                if (Math.abs(total - payload.getQuantity()) > 0.001) {
                    throw new IllegalArgumentException(
                            "Sum of batch quantities (" + total + ") does not match total quantity (" + payload.getQuantity() + ").");
                }
                // Validate ownership before saves
                Long warehouseId = entity.getWarehouse().getWarehouseId();
                for (BatchOverrideEntry entry : validExcess) {
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                    if (!batch.getProduct().getProductId().equals(product.getProductId())) {
                        throw new IllegalArgumentException("Batch #" + entry.getBatchId()
                                + " belongs to a different product.");
                    }
                    if (!batch.getWarehouse().getWarehouseId().equals(warehouseId)) {
                        throw new IllegalArgumentException("Batch #" + entry.getBatchId()
                                + " belongs to a different warehouse.");
                    }
                }
                // Apply
                InventoryBatch firstBatch = null;
                List<BatchOverrideEntry> saved = new ArrayList<>();
                for (BatchOverrideEntry entry : validExcess) {
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                    batch.setQtyRemaining(batch.getQtyRemaining() + entry.getQty());
                    inventoryBatchRepository.save(batch);
                    BatchOverrideEntry enriched = new BatchOverrideEntry();
                    enriched.setBatchId(entry.getBatchId());
                    enriched.setQty(entry.getQty());
                    enriched.setBrand(batch.getBrand());
                    enriched.setLotNumber(batch.getLotNumber());
                    if (batch.getExpiryDate() != null) {
                        enriched.setExpiryDate(
                            new java.text.SimpleDateFormat("dd-MM-yyyy").format(batch.getExpiryDate()));
                    }
                    saved.add(enriched);
                    if (firstBatch == null) firstBatch = batch;
                }
                try {
                    entity.setBatchEntriesJson(
                            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(saved));
                } catch (Exception e) { log.warn("Failed to serialize batchEntriesJson: " + e.getMessage()); }
                return firstBatch;
            }
            // Single existingBatchId path
            if (payload.getExistingBatchId() != null) {
                InventoryBatch batch = inventoryBatchRepository.findById(payload.getExistingBatchId())
                        .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + payload.getExistingBatchId()));
                if (!batch.getProduct().getProductId().equals(product.getProductId())) {
                    throw new IllegalArgumentException("Batch #" + payload.getExistingBatchId()
                            + " belongs to a different product.");
                }
                if (!batch.getWarehouse().getWarehouseId().equals(entity.getWarehouse().getWarehouseId())) {
                    throw new IllegalArgumentException("Batch #" + payload.getExistingBatchId()
                            + " belongs to a different warehouse.");
                }
                batch.setQtyRemaining(batch.getQtyRemaining() + payload.getQuantity());
                return inventoryBatchRepository.save(batch);
            }
            // Upsert: find existing batch with same metadata before creating a new one
            Warehouse warehouse = entity.getWarehouse();
            List<InventoryBatch> existing = inventoryBatchRepository
                    .findAllActiveByProductAndWarehouse(product.getProductId(), warehouse.getWarehouseId());
            InventoryBatch match = findMatchingBatchForUpsert(existing,
                    payload.getLotNumber(), payload.getBrand(), payload.getExpiryDate());
            if (match != null) {
                match.setQtyReceived(match.getQtyReceived() + payload.getQuantity());
                match.setQtyRemaining(match.getQtyRemaining() + payload.getQuantity());
                return inventoryBatchRepository.save(match);
            }
            InventoryBatch newBatch = new InventoryBatch();
            newBatch.setProduct(product);
            newBatch.setWarehouse(warehouse);
            newBatch.setInwardId(0L); // sentinel: excess-found origin
            newBatch.setBrand(payload.getBrand());
            newBatch.setLotNumber(payload.getLotNumber());
            newBatch.setExpiryDate(payload.getExpiryDate());
            newBatch.setReceivedDate(new java.util.Date());
            newBatch.setQtyReceived(payload.getQuantity());
            newBatch.setQtyRemaining(payload.getQuantity());
            return inventoryBatchRepository.save(newBatch);
        }

        return null;
    }

    public LostDamagedReturnData findFiilteredostDamagedList(FilterDataList filterDataList, Pageable pageable)
            throws ParseException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Specification<LostDamagedInventory> spec = LostDamagedInventorySpecification.getSpecification(filterDataList);

        LostDamagedReturnData lostDamagedReturnData = new LostDamagedReturnData();
        if (spec != null)
            lostDamagedReturnData.setLostDamagedInventories(lostDamagedInventoryRepo.findAll(spec, pageable));
        else
            lostDamagedReturnData.setLostDamagedInventories(lostDamagedInventoryRepo.findAll(pageable));
        lostDamagedReturnData.setLdDropdown(populateDropdownService.fetchData("lostdamaged"));
        return lostDamagedReturnData;
    }

    @Transactional(rollbackFor = Exception.class)
    private Double adjustStockBeforeCreate(CreateLostOrDamagedInventoryData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Long warehouseId = warehouseRepo.findById(payload.getWarehouseId()).get().getWarehouseId();
        String operation = "EXCESS_FOUND".equals(payload.getEntryType()) ? "inward" : "outward";
        return stockService.updateStock(payload.getProductId(), warehouseId, payload.getQuantity(), operation);
    }

    private void populateData(LostDamagedInventory lostDamagedInventory, CreateLostOrDamagedInventoryData payload)
            throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        lostDamagedInventory.setLocationOfTheft(payload.getTheftLocation());
        lostDamagedInventory.setQuantity(payload.getQuantity());
        lostDamagedInventory.setProduct(productService.findSingleProduct(payload.getProductId()));
        lostDamagedInventory.setDate(payload.getDate());
        lostDamagedInventory.setWarehouse(warehouseRepo.findById(payload.getWarehouseId()).get());
        lostDamagedInventory.setFileInformations(ReusableMethods.convertFilesListToSet(payload.getFileInformations()));
        lostDamagedInventory.setAdditionalComment(payload.getAdditionalComment());
        lostDamagedInventory.setEntryType(payload.getEntryType() != null ? payload.getEntryType() : "LOST_DAMAGED");
    }

    @Transactional(rollbackFor = Exception.class)
    public LostDamagedInventory UpdateData(CreateLostOrDamagedInventoryData payload, Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<LostDamagedInventory> lostDamagedInventoryOpt = lostDamagedInventoryRepo.findById(id);
        validatePayload(payload);
        if (!lostDamagedInventoryOpt.isPresent())
            throw new Exception("Lost Damaged inventory by ID " + id + " Not found");
        LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
        Double oldStock = lostDamagedInventory.getQuantity();
        AdjustStockBeforeDelete(lostDamagedInventory);
        reverseBatchOnDelete(lostDamagedInventory); // reverse old batch effect before re-applying
        populateData(lostDamagedInventory, payload);
        lostDamagedInventory.setClosingStock(adjustStockBeforeCreate(payload));

        // Re-apply batch handling with new payload
        InventoryBatch newBatchRef = handleBatchOnCreate(lostDamagedInventory, payload);
        lostDamagedInventory.setBatch(newBatchRef);
        if (!oldStock.equals(lostDamagedInventory.getQuantity())) {
            String modifiedNotifType = "EXCESS_FOUND".equals(lostDamagedInventory.getEntryType())
                    ? "excessfoundmodified" : "lostdamagedmodified";
            inventoryNotificationService.pushQuantityEditedNotification(lostDamagedInventory.getProduct(),
                    lostDamagedInventory.getWarehouse().getWarehouseName(), modifiedNotifType,
                    lostDamagedInventory.getQuantity());
        }

        LostDamagedInventory updated = lostDamagedInventoryRepo.save(lostDamagedInventory);
        String updateUser = resolveCurrentUser();
        String updProduct = updated.getProduct() != null ? updated.getProduct().getProductName() : "Unknown";
        activityLogService.record("UPDATED", updated.getEntryType(), String.valueOf(id),
                ActivityLogDescription.withItems(updated.getEntryType() + " entry " + id + " updated by " + updateUser,
                        Collections.singletonList(ActivityLogDescription.itemChanged(updProduct, oldStock, updated.getQuantity()))),
                updateUser);
        return updated;
    }

    private void validatePayload(CreateLostOrDamagedInventoryData payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!productRepo.findById(payload.getProductId()).isPresent())
            throw new Exception("Invalid Product ID");
        if (!warehouseRepo.findById(payload.getWarehouseId()).isPresent())
            throw new Exception("Invalid warehouse ID");
        warehouseRepo.findById(payload.getWarehouseId()).ifPresent(w -> {
            if (com.ec.application.constants.ProjectConstants.deadStockWarehouseName
                    .equalsIgnoreCase(w.getWarehouseName())) {
                throw new RuntimeException("Lost/Damaged entry cannot be created for Dead Stock Warehouse. " +
                        "Use 'Move from Dead Stock' on the Stock page to transfer stock first.");
            }
        });
        if (payload.getQuantity() <= 0)
            throw new Exception("Quantity should be greater than zero");
    }

    public LostDamagedInventory findById(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<LostDamagedInventory> lostDamagedInventoryOpt = lostDamagedInventoryRepo.findById(id);
        if (!lostDamagedInventoryOpt.isPresent())
            throw new Exception("Lost Damaged Inventory by ID " + id + " Not found");
        LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
        return lostDamagedInventory;
    }

    @Transactional(rollbackFor = Exception.class)
    public void DeleteData(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<LostDamagedInventory> lostDamagedInventoryOpt = lostDamagedInventoryRepo.findById(id);
        if (!lostDamagedInventoryOpt.isPresent())
            throw new Exception("Machinery On rent by ID " + id + " Not found");
        LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
        validateBatchStateBeforeDelete(lostDamagedInventory);
        AdjustStockBeforeDelete(lostDamagedInventory);
        reverseBatchOnDelete(lostDamagedInventory);
        lostDamagedInventoryRepo.softDeleteById(id);
        String deleteUser = resolveCurrentUser();
        activityLogService.record("DELETED", lostDamagedInventory.getEntryType(), String.valueOf(id),
                lostDamagedInventory.getEntryType() + " entry " + id + " deleted by " + deleteUser, deleteUser);
    }

    private String resolveCurrentUser() {
        try { return userDetailsService.getCurrentUser().getUsername(); }
        catch (Exception e) { return "System"; }
    }


    /**
     * Reverses batch side-effects when deleting or updating a LostDamagedInventory entry.
     * Uses batchEntriesJson for multi-batch records; falls back to single-batch for legacy records.
     */
    /**
     * Guards against deleting an EXCESS_FOUND record whose added stock was already consumed by outward.
     * Reverting would silently clamp batch qtyRemaining at 0 while reducing stock,
     * creating a stock < batch_sum inconsistency.
     *
     * Only EXCESS_FOUND needs this guard — LOST_DAMAGED reversal always ADDS back to batch (safe).
     */
    private void validateBatchStateBeforeDelete(LostDamagedInventory entity) throws Exception {
        if (!"EXCESS_FOUND".equals(entity.getEntryType())) return;

        String entriesJson = entity.getBatchEntriesJson();
        if (entriesJson != null && !entriesJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<BatchOverrideEntry> entries = mapper.readValue(entriesJson,
                        mapper.getTypeFactory().constructCollectionType(List.class, BatchOverrideEntry.class));
                for (BatchOverrideEntry entry : entries) {
                    if (entry.getBatchId() == null || entry.getQty() == null) continue;
                    inventoryBatchRepository.findById(entry.getBatchId()).ifPresent(batch -> {
                        if (batch.getQtyRemaining() < entry.getQty() - 0.001) {
                            throw new IllegalStateException(
                                "Cannot delete this Excess Found entry. The stock added to Batch #" + batch.getBatchId()
                                + " (product: " + batch.getProduct().getProductName() + ")"
                                + " has already been partially consumed via outward ("
                                + String.format("%.3f", entry.getQty() - batch.getQtyRemaining())
                                + " units consumed). "
                                + "Please reverse those outward records first before deleting this entry.");
                        }
                    });
                }
                return;
            } catch (IllegalStateException e) {
                throw new Exception(e.getMessage());
            } catch (Exception ignored) {
                // JSON parse failure — fall through to single-batch check
            }
        }

        // Single batch path
        InventoryBatch batch = entity.getBatch();
        if (batch != null) {
            double excessQty = entity.getQuantity();
            if (batch.getQtyRemaining() < excessQty - 0.001) {
                throw new Exception(
                    "Cannot delete this Excess Found entry. The stock added to Batch #" + batch.getBatchId()
                    + " (product: " + batch.getProduct().getProductName() + ")"
                    + " has already been partially consumed via outward ("
                    + String.format("%.3f", excessQty - batch.getQtyRemaining())
                    + " units consumed). "
                    + "Please reverse those outward records first before deleting this entry.");
            }
        }
    }

    private void reverseBatchOnDelete(LostDamagedInventory entity) {
        String entriesJson = entity.getBatchEntriesJson();
        if (entriesJson != null && !entriesJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                List<BatchOverrideEntry> entries = mapper.readValue(entriesJson,
                        mapper.getTypeFactory().constructCollectionType(List.class, BatchOverrideEntry.class));
                for (BatchOverrideEntry entry : entries) {
                    if (entry.getBatchId() == null) continue;
                    inventoryBatchRepository.findById(entry.getBatchId()).ifPresent(batch -> {
                        double qty = entry.getQty() != null ? entry.getQty() : 0.0;
                        if ("LOST_DAMAGED".equals(entity.getEntryType())) {
                            batch.setQtyRemaining(batch.getQtyRemaining() + qty);
                        } else {
                            batch.setQtyRemaining(Math.max(0.0, batch.getQtyRemaining() - qty));
                        }
                        inventoryBatchRepository.save(batch);
                    });
                }
                return;
            } catch (Exception e) {
                log.warn("Failed to parse batchEntriesJson for reversal, falling back to single batch: " + e.getMessage());
            }
        }
        reverseSingleBatch(entity);
    }

    private void reverseSingleBatch(LostDamagedInventory entity) {
        InventoryBatch batch = entity.getBatch();
        if (batch == null) return;
        if ("LOST_DAMAGED".equals(entity.getEntryType())) {
            batch.setQtyRemaining(batch.getQtyRemaining() + entity.getQuantity());
            inventoryBatchRepository.save(batch);
        } else if ("EXCESS_FOUND".equals(entity.getEntryType())) {
            double restored = Math.max(0.0, batch.getQtyRemaining() - entity.getQuantity());
            batch.setQtyRemaining(restored);
            inventoryBatchRepository.save(batch);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void AdjustStockBeforeDelete(LostDamagedInventory lostDamagedInventory) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        String reverseOp = "EXCESS_FOUND".equals(lostDamagedInventory.getEntryType()) ? "outward" : "inward";
        stockService.updateStock(lostDamagedInventory.getProduct().getProductId(),
                lostDamagedInventory.getWarehouse().getWarehouseId(), lostDamagedInventory.getQuantity(), reverseOp);
    }

    public Page<LostDamagedInventory> findAll(Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Page<LostDamagedInventory> allLODInv = lostDamagedInventoryRepo.findAll(pageable);
        return allLODInv;
    }

    // ── Batch upsert helpers ─────────────────────────────────────────────────

    /**
     * Finds an existing batch whose lot#, brand, and expiryDate match.
     * receivedDate is intentionally excluded from the key — excess-found entries
     * use today's date, so two entries on different days for the same physical batch
     * would otherwise not merge.
     */
    private InventoryBatch findMatchingBatchForUpsert(List<InventoryBatch> existing,
                                                       String lotNumber, String brand, Date expiryDate) {
        for (InventoryBatch b : existing) {
            if (!nullSafeStringEquals(b.getLotNumber(), lotNumber)) continue;
            if (!nullSafeStringEquals(b.getBrand(), brand)) continue;
            if (!nullSafeDateEquals(b.getExpiryDate(), expiryDate)) continue;
            return b;
        }
        return null;
    }

    private boolean nullSafeStringEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private boolean nullSafeDateEquals(Date a, Date b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return truncateToDay(a).equals(truncateToDay(b));
    }

    private Date truncateToDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
