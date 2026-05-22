package com.ec.application.service;

import com.ec.application.data.StockSplitRequest;
import com.ec.application.data.StockTilesDTO;
import com.ec.application.data.WriteOffRequestDTO;
import com.ec.application.model.BatchWriteOff;
import com.ec.application.model.InventoryBatch;
import com.ec.application.model.InventoryNotification;
import com.ec.application.model.Product;
import com.ec.application.model.Warehouse;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.StockInformationSpecification;
import com.ec.application.model.StockInformationFromView;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.application.repository.BatchWriteOffRepository;
import com.ec.application.repository.InventoryBatchRepository;
import com.ec.application.repository.InventoryNotificationRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockInformationRepo;
import com.ec.application.repository.WarehouseRepo;
import org.springframework.data.jpa.domain.Specification;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class BatchTrackingService {

    Logger log = LoggerFactory.getLogger(BatchTrackingService.class);

    @Autowired
    InventoryBatchRepository inventoryBatchRepository;

    @Autowired
    BatchWriteOffRepository batchWriteOffRepository;

    @Autowired
    StockService stockService;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    InventoryNotificationRepo inventoryNotificationRepo;

    @Autowired
    ProductRepo productRepo;

    @Autowired
    WarehouseRepo warehouseRepo;

    @Autowired
    StockInformationRepo stockInformationRepo;

    @Autowired
    AllInventoryRepo allInventoryRepo;

    @Transactional(rollbackFor = Exception.class)
    public BatchWriteOff writeOffBatch(Long batchId, WriteOffRequestDTO request) throws Exception {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new Exception("Batch not found with ID: " + batchId));

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Write-off quantity must be greater than zero.");
        }
        if (request.getQuantity() > batch.getQtyRemaining()) {
            throw new IllegalArgumentException(
                    "Write-off quantity (" + request.getQuantity() + ") exceeds available batch quantity ("
                    + batch.getQtyRemaining() + ").");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required for write-off.");
        }
        if (request.getWriteOffDate() == null) {
            throw new IllegalArgumentException("Write-off date is required.");
        }

        batch.setQtyRemaining(batch.getQtyRemaining() - request.getQuantity());
        inventoryBatchRepository.save(batch);

        stockService.updateStock(
                batch.getProduct().getProductId(),
                batch.getWarehouse().getWarehouseId(),
                request.getQuantity(),
                "outward"
        );

        String currentUser = null;
        try {
            currentUser = userDetailsService.getCurrentUser().getUsername();
        } catch (Exception ignored) {}

        BatchWriteOff writeOff = new BatchWriteOff();
        writeOff.setBatch(batch);
        writeOff.setProductId(batch.getProduct().getProductId());
        writeOff.setProductName(batch.getProduct().getProductName());
        writeOff.setWarehouseId(batch.getWarehouse().getWarehouseId());
        writeOff.setQuantity(request.getQuantity());
        writeOff.setReason(request.getReason().trim());
        writeOff.setWriteOffDate(request.getWriteOffDate());
        writeOff.setWrittenOffBy(currentUser);

        return batchWriteOffRepository.save(writeOff);
    }

    public List<InventoryBatch> getBatchesForProduct(Long productId, Long warehouseId) {
        List<InventoryBatch> batches = inventoryBatchRepository.findAllBatchesForProduct(productId, warehouseId);
        LocalDate today = LocalDate.now();
        for (InventoryBatch batch : batches) {
            if (batch.getExpiryDate() != null) {
                LocalDate expiry = batch.getExpiryDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                long days = ChronoUnit.DAYS.between(today, expiry);
                batch.setDaysUntilExpiry(days);
                batch.setIsExpired(days < 0);
            }
        }
        return batches;
    }

    public StockTilesDTO getStockTiles() {
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        Date now  = Date.from(today.atStartOfDay(zone).toInstant());
        Date in30 = Date.from(today.plusDays(30).atStartOfDay(zone).toInstant());
        Date in60 = Date.from(today.plusDays(60).atStartOfDay(zone).toInstant());

        // Expiry
        long expiring30 = inventoryBatchRepository.countDistinctProductsExpiringBetween(now, in30);
        long expiring60 = inventoryBatchRepository.countDistinctProductsExpiringBetween(in30, in60);
        long expired    = inventoryBatchRepository.countDistinctProductsExpired(now);

        // Stock status
        long lowStock  = stockInformationRepo.countByStockStatus("Low");
        long highStock = stockInformationRepo.countByStockStatus("High");

        // Aging: products with no inward in last N days that still have stock
        Date cutoff30 = Date.from(today.minusDays(30).atStartOfDay(zone).toInstant());
        Date cutoff60 = Date.from(today.minusDays(60).atStartOfDay(zone).toInstant());
        Date cutoff90 = Date.from(today.minusDays(90).atStartOfDay(zone).toInstant());
        long aging30  = allInventoryRepo.countAgingProducts(cutoff30);
        long aging60  = allInventoryRepo.countAgingProducts(cutoff60);
        long aging90  = allInventoryRepo.countAgingProducts(cutoff90);

        StockTilesDTO dto = new StockTilesDTO();
        dto.setExpiring30Days(expiring30);
        dto.setExpiring60Days(expiring60);
        dto.setExpiredCount(expired);
        dto.setLowStockCount(lowStock);
        dto.setHighStockCount(highStock);
        dto.setAging30Days(aging30);
        dto.setAging60Days(aging60);
        dto.setAging90Days(aging90);
        return dto;
    }

    public List<BatchWriteOff> getWriteOffHistory(Long batchId) {
        return batchWriteOffRepository.findByBatch_BatchIdOrderByWriteOffDateDesc(batchId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<InventoryBatch> splitExistingStock(Long productId, StockSplitRequest request) throws Exception {
        if (request.getWarehouseId() == null)
            throw new IllegalArgumentException("Warehouse is required.");
        if (request.getBatches() == null || request.getBatches().isEmpty())
            throw new IllegalArgumentException("At least one batch entry is required.");

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new Exception("Product not found: " + productId));
        if (!Boolean.TRUE.equals(product.getIsExpirable()))
            throw new IllegalArgumentException("Stock split is only allowed for expirable products.");

        Warehouse warehouse = warehouseRepo.findById(request.getWarehouseId())
                .orElseThrow(() -> new Exception("Warehouse not found: " + request.getWarehouseId()));

        Double currentStock = stockService.findStockForProductWarehouse(productId, request.getWarehouseId());
        if (currentStock == null) currentStock = 0.0;

        Double trackedQty = inventoryBatchRepository.sumQtyRemainingByProductAndWarehouse(productId, request.getWarehouseId());
        if (trackedQty == null) trackedQty = 0.0;

        double untrackedQty = currentStock - trackedQty;
        if (untrackedQty < 0.001)
            throw new IllegalArgumentException("No untracked stock remaining — all " + currentStock + " units are already in batches.");

        double requestTotal = request.getBatches().stream()
                .mapToDouble(e -> e.getQty() != null ? e.getQty() : 0.0)
                .sum();
        if (Math.abs(requestTotal - untrackedQty) > 0.001)
            throw new IllegalArgumentException(
                "Batch quantities (" + requestTotal + ") must equal untracked stock (" + untrackedQty + "). Difference: " + Math.abs(requestTotal - untrackedQty));

        Date today = new Date();
        List<InventoryBatch> created = new ArrayList<>();
        for (StockSplitRequest.BatchEntry entry : request.getBatches()) {
            if (entry.getQty() == null || entry.getQty() <= 0)
                throw new IllegalArgumentException("Each batch quantity must be greater than zero.");
            if (entry.getExpiryDate() == null)
                throw new IllegalArgumentException("Expiry date is required for each batch.");

            InventoryBatch batch = new InventoryBatch();
            batch.setProduct(product);
            batch.setWarehouse(warehouse);
            batch.setInwardId(-1L);
            batch.setBrand(entry.getBrand());
            batch.setExpiryDate(entry.getExpiryDate());
            batch.setReceivedDate(today);
            batch.setQtyReceived(entry.getQty());
            batch.setQtyRemaining(entry.getQty());
            created.add(inventoryBatchRepository.save(batch));
        }
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processExpiryAlerts() {
        LocalDate today = LocalDate.now();
        Date now = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date in30 = Date.from(today.plusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date in60 = Date.from(today.plusDays(60).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Expiring within 30 days
        List<InventoryBatch> expiring30 = inventoryBatchRepository.findBatchesExpiringBetween(now, in30);
        for (InventoryBatch batch : expiring30) {
            if (!Boolean.TRUE.equals(batch.getAlertSent30())) {
                pushExpiryNotification(batch, "EXPIRY_ALERT_30");
                batch.setAlertSent30(true);
                inventoryBatchRepository.save(batch);
            }
        }

        // Expiring in 31–60 days (not yet in 30-day window)
        List<InventoryBatch> expiring60 = inventoryBatchRepository.findBatchesExpiringBetween(in30, in60);
        for (InventoryBatch batch : expiring60) {
            if (!Boolean.TRUE.equals(batch.getAlertSent60())) {
                pushExpiryNotification(batch, "EXPIRY_ALERT_60");
                batch.setAlertSent60(true);
                inventoryBatchRepository.save(batch);
            }
        }

        // Already expired
        List<InventoryBatch> expired = inventoryBatchRepository.findExpiredBatchesWithStock(now);
        for (InventoryBatch batch : expired) {
            if (!Boolean.TRUE.equals(batch.getAlertSentExpired())) {
                pushExpiryNotification(batch, "EXPIRY_EXPIRED");
                batch.setAlertSentExpired(true);
                inventoryBatchRepository.save(batch);
            }
        }
    }

    private void pushExpiryNotification(InventoryBatch batch, String type) {
        InventoryNotification notification = new InventoryNotification();
        notification.setProduct(batch.getProduct());
        notification.setWarehouseName(batch.getWarehouse().getWarehouseName());
        notification.setQuantity(batch.getQtyRemaining());
        notification.setType(type);
        notification.setUpdatedBy("SYSTEM");
        inventoryNotificationRepo.save(notification);
    }
}
