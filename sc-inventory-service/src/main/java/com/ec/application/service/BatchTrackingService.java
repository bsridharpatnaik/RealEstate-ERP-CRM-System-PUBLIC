package com.ec.application.service;

import com.ec.application.data.StockTilesDTO;
import com.ec.application.data.WriteOffRequestDTO;
import com.ec.application.model.BatchWriteOff;
import com.ec.application.model.InventoryBatch;
import com.ec.application.model.InventoryNotification;
import com.ec.application.repository.BatchWriteOffRepository;
import com.ec.application.repository.InventoryBatchRepository;
import com.ec.application.repository.InventoryNotificationRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
        Date now = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date in30 = Date.from(today.plusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date in60 = Date.from(today.plusDays(60).atStartOfDay(ZoneId.systemDefault()).toInstant());

        long expiring30 = inventoryBatchRepository.countDistinctProductsExpiringBetween(now, in30);
        long expiring60 = inventoryBatchRepository.countDistinctProductsExpiringBetween(in30, in60);
        long expired = inventoryBatchRepository.countDistinctProductsExpired(now);

        return new StockTilesDTO(expiring30, expiring60, expired);
    }

    public List<BatchWriteOff> getWriteOffHistory(Long batchId) {
        return batchWriteOffRepository.findByBatch_BatchIdOrderByWriteOffDateDesc(batchId);
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
