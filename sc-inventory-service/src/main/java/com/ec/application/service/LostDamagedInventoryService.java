package com.ec.application.service;

import java.text.ParseException;
import java.util.ArrayList;
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

        if ("LOST_DAMAGED".equals(payload.getEntryType())) {
            List<BatchOverrideEntry> entries = payload.getBatchEntries();
            if (entries != null && !entries.isEmpty()) {
                // Multi-batch drain
                double total = entries.stream().mapToDouble(e -> e.getQty() != null ? e.getQty() : 0.0).sum();
                if (Math.abs(total - payload.getQuantity()) > 0.001) {
                    throw new IllegalArgumentException(
                            "Sum of batch quantities (" + total + ") does not match total quantity (" + payload.getQuantity() + ").");
                }
                InventoryBatch firstBatch = null;
                List<BatchOverrideEntry> saved = new ArrayList<>();
                for (BatchOverrideEntry entry : entries) {
                    if (entry.getBatchId() == null || entry.getQty() == null || entry.getQty() <= 0) continue;
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                    if (entry.getQty() > batch.getQtyRemaining()) {
                        throw new IllegalArgumentException("Batch " + entry.getBatchId()
                                + " only has " + batch.getQtyRemaining() + " units remaining.");
                    }
                    batch.setQtyRemaining(batch.getQtyRemaining() - entry.getQty());
                    inventoryBatchRepository.save(batch);
                    saved.add(entry);
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
            if (payload.getQuantity() > batch.getQtyRemaining()) {
                throw new IllegalArgumentException("Cannot mark " + payload.getQuantity()
                        + " as lost/damaged. Batch only has " + batch.getQtyRemaining() + " units remaining.");
            }
            batch.setQtyRemaining(batch.getQtyRemaining() - payload.getQuantity());
            return inventoryBatchRepository.save(batch);
        }

        if ("EXCESS_FOUND".equals(payload.getEntryType())) {
            List<BatchOverrideEntry> excessEntries = payload.getExcessBatchEntries();
            if (excessEntries != null && !excessEntries.isEmpty()) {
                // Multi-batch add to existing
                double total = excessEntries.stream().mapToDouble(e -> e.getQty() != null ? e.getQty() : 0.0).sum();
                if (Math.abs(total - payload.getQuantity()) > 0.001) {
                    throw new IllegalArgumentException(
                            "Sum of batch quantities (" + total + ") does not match total quantity (" + payload.getQuantity() + ").");
                }
                InventoryBatch firstBatch = null;
                List<BatchOverrideEntry> saved = new ArrayList<>();
                for (BatchOverrideEntry entry : excessEntries) {
                    if (entry.getBatchId() == null || entry.getQty() == null || entry.getQty() <= 0) continue;
                    InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                    batch.setQtyRemaining(batch.getQtyRemaining() + entry.getQty());
                    inventoryBatchRepository.save(batch);
                    saved.add(entry);
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
                batch.setQtyRemaining(batch.getQtyRemaining() + payload.getQuantity());
                return inventoryBatchRepository.save(batch);
            }
            // Create new batch
            Warehouse warehouse = entity.getWarehouse();
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

}
