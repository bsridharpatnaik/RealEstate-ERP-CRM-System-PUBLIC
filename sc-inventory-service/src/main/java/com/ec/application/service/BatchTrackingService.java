package com.ec.application.service;

import com.ec.application.data.BatchConsumptionPreviewDTO;
import com.ec.application.data.BatchOverrideEntry;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        writeOff.setWriteOffDate(new Date());
        writeOff.setWrittenOffBy(currentUser);

        return batchWriteOffRepository.save(writeOff);
    }

    public List<InventoryBatch> getBatchesForProduct(Long productId, Long warehouseId) {
        List<InventoryBatch> batches = warehouseId != null
                ? inventoryBatchRepository.findAllBatchesForProduct(productId, warehouseId)
                : inventoryBatchRepository.findAllBatchesForProductAllWarehouses(productId);
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

    private long computeUntrackedCount(List<Long> restrictToIds) {
        List<Long> batchTrackedIds = new ArrayList<>(productRepo.findBatchTrackedProductIds());
        if (batchTrackedIds.isEmpty()) return 0;

        if (restrictToIds != null) {
            batchTrackedIds.retainAll(restrictToIds);
            if (batchTrackedIds.isEmpty()) return 0;
        }

        List<StockInformationFromView> stockItems =
                stockInformationRepo.findByProductIdInAndTotalQuantityInHandGreaterThan(batchTrackedIds, 0.0);
        if (stockItems.isEmpty()) return 0;

        List<Long> stockProductIds = stockItems.stream()
                .map(StockInformationFromView::getProductId)
                .collect(Collectors.toList());

        List<Object[]> batchSums = inventoryBatchRepository.sumQtyRemainingGroupByProduct(stockProductIds);
        Map<Long, Double> batchQtyMap = new HashMap<>();
        for (Object[] row : batchSums) {
            batchQtyMap.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
        }

        long count = 0;
        for (StockInformationFromView si : stockItems) {
            double batchQty = batchQtyMap.getOrDefault(si.getProductId(), 0.0);
            if (si.getTotalQuantityInHand() > batchQty + 0.001) {
                count++;
            }
        }
        return count;
    }

    public StockTilesDTO getStockTiles(FilterDataList filterDataList) {
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        Date now      = Date.from(today.atStartOfDay(zone).toInstant());
        Date in30     = Date.from(today.plusDays(30).atStartOfDay(zone).toInstant());
        Date in60     = Date.from(today.plusDays(60).atStartOfDay(zone).toInstant());
        Date cutoff30 = Date.from(today.minusDays(30).atStartOfDay(zone).toInstant());
        Date cutoff60 = Date.from(today.minusDays(60).atStartOfDay(zone).toInstant());
        Date cutoff90 = Date.from(today.minusDays(90).atStartOfDay(zone).toInstant());

        // Determine whether a meaningful filter is present
        boolean hasFilter = filterDataList != null
                && filterDataList.getFilterData() != null
                && filterDataList.getFilterData().stream()
                        .anyMatch(f -> f.getAttrName() != null
                                && !f.getAttrName().equals("expiryFilter")
                                && f.getAttrValue() != null
                                && !f.getAttrValue().isEmpty());

        long expiring30, expiring60, expired, lowStock, highStock, aging30, aging60, aging90, untrackedCount;

        if (!hasFilter) {
            // Global (unfiltered) counts
            expiring30 = inventoryBatchRepository.countDistinctProductsExpiringBetween(now, in30);
            expiring60 = inventoryBatchRepository.countDistinctProductsExpiringBetween(in30, in60);
            expired    = inventoryBatchRepository.countDistinctProductsExpired(now);
            lowStock   = stockInformationRepo.countByStockStatus("Low");
            highStock  = stockInformationRepo.countByStockStatus("High");
            aging30    = allInventoryRepo.countAgingProducts(cutoff30);
            aging60    = allInventoryRepo.countAgingProducts(cutoff60);
            aging90    = allInventoryRepo.countAgingProducts(cutoff90);
            untrackedCount = computeUntrackedCount(null);
        } else {
            Specification<StockInformationFromView> spec =
                    StockInformationSpecification.getSpecification(filterDataList);

            List<Long> filteredIds = (spec != null)
                    ? stockInformationRepo.findAll(spec).stream()
                            .map(StockInformationFromView::getProductId)
                            .collect(Collectors.toList())
                    : new ArrayList<>();

            if (filteredIds.isEmpty()) {
                return new StockTilesDTO(); // all zeros
            }

            expiring30 = inventoryBatchRepository.countDistinctProductsExpiringBetweenIn(now, in30, filteredIds);
            expiring60 = inventoryBatchRepository.countDistinctProductsExpiringBetweenIn(in30, in60, filteredIds);
            expired    = inventoryBatchRepository.countDistinctProductsExpiredIn(now, filteredIds);
            lowStock   = stockInformationRepo.countByStockStatusAndProductIdIn("Low", filteredIds);
            highStock  = stockInformationRepo.countByStockStatusAndProductIdIn("High", filteredIds);
            aging30    = allInventoryRepo.countAgingProductsIn(cutoff30, filteredIds);
            aging60    = allInventoryRepo.countAgingProductsIn(cutoff60, filteredIds);
            aging90    = allInventoryRepo.countAgingProductsIn(cutoff90, filteredIds);
            untrackedCount = computeUntrackedCount(filteredIds);
        }

        StockTilesDTO dto = new StockTilesDTO();
        dto.setExpiring30Days(expiring30);
        dto.setExpiring60Days(expiring60);
        dto.setExpiredCount(expired);
        dto.setLowStockCount(lowStock);
        dto.setHighStockCount(highStock);
        dto.setAging30Days(aging30);
        dto.setAging60Days(aging60);
        dto.setAging90Days(aging90);
        dto.setUntrackedCount(untrackedCount);
        return dto;
    }

    /**
     * Simulates batch consumption for an outward without persisting anything.
     * Used by frontend to show a preview of which batches will be consumed.
     */
    @Transactional(readOnly = true)
    public BatchConsumptionPreviewDTO previewBatchConsumption(
            Long productId, Long warehouseId, Double qty,
            List<BatchOverrideEntry> overrideBatches) throws Exception {

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new Exception("Product not found: " + productId));

        // FEFO for BATCH_WITH_EXPIRY; FIFO by receivedDate for BATCH_ONLY
        List<InventoryBatch> fifoBatches = product.requiresExpiry()
                ? inventoryBatchRepository.findAvailableBatchesFifoOrder(productId, warehouseId)
                : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceived(productId, warehouseId);

        List<BatchConsumptionPreviewDTO.BatchPreviewItem> items = new ArrayList<>();

        if (overrideBatches != null && !overrideBatches.isEmpty()) {
            // Multi-batch override: user specified exact batches + qtys
            double totalOverride = overrideBatches.stream()
                    .mapToDouble(e -> e.getQty() != null ? e.getQty() : 0).sum();
            if (Math.abs(totalOverride - qty) > 0.001) {
                throw new IllegalArgumentException(
                    "Override batch quantities (" + totalOverride + ") must equal outward quantity (" + qty + ").");
            }
            List<Long> fifoBatchIds = fifoBatches.stream()
                    .map(InventoryBatch::getBatchId).collect(Collectors.toList());

            // Pre-compute what pure FIFO would assign to each batch (for quantity-level override detection)
            Map<Long, Double> fifoExpectedQty = new HashMap<>();
            double fifoRemaining = qty;
            for (InventoryBatch fb : fifoBatches) {
                if (fifoRemaining <= 0) break;
                double fifoConsume = Math.min(fifoRemaining, fb.getQtyRemaining());
                fifoExpectedQty.put(fb.getBatchId(), fifoConsume);
                fifoRemaining -= fifoConsume;
            }

            int fifoPtr = 0;
            for (BatchOverrideEntry entry : overrideBatches) {
                InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                        .orElseThrow(() -> new Exception("Batch not found: " + entry.getBatchId()));
                boolean isFifoOrder = fifoPtr < fifoBatchIds.size()
                        && fifoBatchIds.get(fifoPtr).equals(entry.getBatchId());
                Double expectedQty = fifoExpectedQty.get(entry.getBatchId());
                boolean isFifoQty = expectedQty != null && Math.abs(expectedQty - entry.getQty()) <= 0.001;
                boolean fifoOverridden = !isFifoOrder || !isFifoQty;
                BatchConsumptionPreviewDTO.BatchPreviewItem item = new BatchConsumptionPreviewDTO.BatchPreviewItem();
                item.setBatchId(batch.getBatchId());
                item.setBrand(batch.getBrand());
                item.setLotNumber(batch.getLotNumber());
                item.setExpiryDate(batch.getExpiryDate());
                item.setReceivedDate(batch.getReceivedDate());
                item.setQtyConsumed(entry.getQty());
                item.setQtyAvailable(batch.getQtyRemaining());
                item.setFifoOverridden(fifoOverridden);
                items.add(item);
                fifoPtr++;
            }
        } else {
            // Pure FIFO: simulate consumption from oldest batches first
            double remaining = qty;
            for (InventoryBatch batch : fifoBatches) {
                if (remaining <= 0) break;
                double consume = Math.min(remaining, batch.getQtyRemaining());
                BatchConsumptionPreviewDTO.BatchPreviewItem item = new BatchConsumptionPreviewDTO.BatchPreviewItem();
                item.setBatchId(batch.getBatchId());
                item.setBrand(batch.getBrand());
                item.setLotNumber(batch.getLotNumber());
                item.setExpiryDate(batch.getExpiryDate());
                item.setReceivedDate(batch.getReceivedDate());
                item.setQtyConsumed(consume);
                item.setQtyAvailable(batch.getQtyRemaining());
                item.setFifoOverridden(false);
                items.add(item);
                remaining -= consume;
            }
        }

        BatchConsumptionPreviewDTO dto = new BatchConsumptionPreviewDTO();
        dto.setProductId(productId);
        dto.setBatches(items);
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
        if (!product.isBatchTracked())
            throw new IllegalArgumentException("Stock split is only allowed for batch-tracked products (BATCH_ONLY or BATCH_WITH_EXPIRY).");

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

        boolean requireExpiry = product.requiresExpiry();
        Date today = new Date();
        List<InventoryBatch> created = new ArrayList<>();
        for (StockSplitRequest.BatchEntry entry : request.getBatches()) {
            if (entry.getQty() == null || entry.getQty() <= 0)
                throw new IllegalArgumentException("Each batch quantity must be greater than zero.");
            if (requireExpiry && entry.getExpiryDate() == null)
                throw new IllegalArgumentException("Expiry date is required for each batch.");

            InventoryBatch batch = new InventoryBatch();
            batch.setProduct(product);
            batch.setWarehouse(warehouse);
            batch.setInwardId(-1L);
            batch.setBrand(entry.getBrand());
            batch.setLotNumber(entry.getLotNumber());
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
