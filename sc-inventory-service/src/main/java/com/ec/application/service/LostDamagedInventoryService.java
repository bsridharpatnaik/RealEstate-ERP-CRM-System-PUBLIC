package com.ec.application.service;

import java.text.ParseException;
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
     * - LOST_DAMAGED + batchId: drain that batch. Validates qty <= qtyRemaining.
     * - LOST_DAMAGED + no batchId: throws — batchId is required for batch-tracked products.
     * - EXCESS_FOUND + existingBatchId: add qty to an existing batch.
     * - EXCESS_FOUND + no existingBatchId: create a new InventoryBatch.
     */
    private InventoryBatch handleBatchOnCreate(LostDamagedInventory entity,
                                                CreateLostOrDamagedInventoryData payload) {
        Product product = entity.getProduct();
        if (product == null || !product.isBatchTracked()) return null;

        if ("LOST_DAMAGED".equals(payload.getEntryType())) {
            // LD4: require batchId for batch-tracked products
            if (payload.getBatchId() == null) {
                throw new IllegalArgumentException(
                        "This product is batch-tracked. Please select the batch the loss came from.");
            }
            InventoryBatch batch = inventoryBatchRepository.findById(payload.getBatchId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Batch not found: " + payload.getBatchId()));
            // LD2: validate qty instead of silently flooring to 0
            if (payload.getQuantity() > batch.getQtyRemaining()) {
                throw new IllegalArgumentException(
                        "Cannot mark " + payload.getQuantity()
                        + " as lost/damaged. Batch only has " + batch.getQtyRemaining()
                        + " units remaining.");
            }
            batch.setQtyRemaining(batch.getQtyRemaining() - payload.getQuantity());
            return inventoryBatchRepository.save(batch);
        }

        if ("EXCESS_FOUND".equals(payload.getEntryType())) {
            // LD3: add to existing batch if user selected one
            if (payload.getExistingBatchId() != null) {
                InventoryBatch batch = inventoryBatchRepository.findById(payload.getExistingBatchId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Batch not found: " + payload.getExistingBatchId()));
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
     * - LOST_DAMAGED: restore qty back to the referenced batch.
     * - EXCESS_FOUND: zero out the created batch (soft-delete is handled by stock reversal; zeroing prevents double-counting).
     */
    private void reverseBatchOnDelete(LostDamagedInventory entity) {
        InventoryBatch batch = entity.getBatch();
        if (batch == null) return;

        if ("LOST_DAMAGED".equals(entity.getEntryType())) {
            // Restore qty that was drained on create
            batch.setQtyRemaining(batch.getQtyRemaining() + entity.getQuantity());
            inventoryBatchRepository.save(batch);
        } else if ("EXCESS_FOUND".equals(entity.getEntryType())) {
            // Reduce by what was added — clamp to 0 if outwards consumed some of it
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
