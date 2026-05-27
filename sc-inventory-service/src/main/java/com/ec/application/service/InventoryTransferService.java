package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.Filters.InventoryTransferSpecification;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.*;
import com.ec.application.exception.EntityNotFoundException;
import com.ec.application.exception.InsufficientStockException;
import com.ec.application.mapper.InventoryTransferMapper;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InventoryBatchRepository;
import com.ec.application.repository.InventoryTransferRepository;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.WarehouseRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryTransferService {

    private static final Logger log = LoggerFactory.getLogger(InventoryTransferService.class);

    private final InventoryTransferRepository inventoryTransferRepository;
    private final StockService stockService;
    private final TenantService tenantService;
    private final ProductRepo productRepo;
    private final WarehouseRepo warehouseRepo;
    private final InventoryTransferMapper inventoryTransferMapper;
    private final PopulateDropdownService populateDropdownService;
    private final InventoryBatchRepository inventoryBatchRepository;

    @Value("${master.schema}")
    private String masterSchema;

    /* =====================================================
       MAIN API
       ===================================================== */

    public InventoryTransferResult createTransfer(CreateTransferDTO dto) throws Exception {
        replaceTenantNamesForSuncity(dto);
        validateTransferRequest(dto);
        validateDuplicateProducts(dto);
        validateSourceStockAvailability(dto);

        String sourceTenant = dto.getSourceTenant();
        String targetTenant = dto.getTargetTenant();

        Warehouse sourceWarehouse = withTenant(sourceTenant,
                () -> warehouseRepo.findById(dto.getSourceWarehouseId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Source warehouse not found")));

        Warehouse targetWarehouse = withTenant(targetTenant,
                () -> warehouseRepo.findById(dto.getTargetWarehouseId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Target warehouse not found")));


        Map<Long, Product> productMap = fetchProducts(dto, masterSchema);
        InventoryTransfer transfer = buildTransferEntity(dto, sourceWarehouse, targetWarehouse, productMap);
        List<TransferItemResult> itemResults = new ArrayList<>();
        List<InventoryTransferItem> successfulItems = new ArrayList<>();

        /* =====================================================
           PER-PRODUCT SAGA
           ===================================================== */
        for (InventoryTransferItem item : transfer.getItems()) {

            try {
                // DEBIT SOURCE
                Double sourceClosingStock = withTenant(sourceTenant, () ->
                        stockService.updateStock(item.getProductId(), sourceWarehouse.getWarehouseId(), item.getQuantity(), "outward"));

                try {
                    // CREDIT TARGET
                    Double targetClosingStock = withTenant(targetTenant, () ->
                            stockService.updateStock(item.getProductId(), targetWarehouse.getWarehouseId(), item.getQuantity(), "inward"));

                    // SET CLOSING STOCKS ONLY ON FULL SUCCESS
                    item.setSourceClosingStock(sourceClosingStock);
                    item.setTargetClosingStock(targetClosingStock);
                    successfulItems.add(item);
                    itemResults.add(new TransferItemResult(item.getProductId(), true, "Transfer successful"));

                    // SPLIT BATCHES: deduct from source, create in target (#6) — best-effort
                    // Find matching DTO item for this transfer item (to get override batches)
                    final InventoryTransferItemDTO dtoItem = dto.getItems().stream()
                            .filter(d -> d.getProductId().equals(item.getProductId()))
                            .findFirst().orElse(null);
                    final Product sourceProduct = productMap.get(item.getProductId());
                    List<TransferredBatchData> transferredBatches = new ArrayList<>();
                    try {
                        withTenant(sourceTenant, () -> {
                            transferredBatches.addAll(deductSourceBatchesForTransfer(
                                    item.getProductId(), sourceWarehouse.getWarehouseId(), item.getQuantity(),
                                    sourceProduct, dtoItem));
                            return null;
                        });
                        withTenant(targetTenant, () -> {
                            createTargetBatchesForTransfer(
                                    item.getProductId(), targetWarehouse.getWarehouseId(), transferredBatches);
                            return null;
                        });
                    } catch (Exception batchEx) {
                        log.error("Batch split failed for transfer productId={}, error={}",
                                item.getProductId(), batchEx.getMessage());
                    }

                } catch (Exception creditEx) {

                    // COMPENSATE DEBIT
                    withTenant(sourceTenant, () -> {
                        stockService.updateStock(item.getProductId(), sourceWarehouse.getWarehouseId(), item.getQuantity(), "inward");
                        return null;
                    });

                    itemResults.add(new TransferItemResult(item.getProductId(), false, "Credit failed: " + creditEx.getMessage()));
                }

            } catch (Exception debitEx) {
                itemResults.add(new TransferItemResult(item.getProductId(), false, "Debit failed: " + debitEx.getMessage()));
            }
        }
        /* =====================================================
           PERSIST ONLY SUCCESSFUL ITEMS
           ===================================================== */
        transfer.setItems(successfulItems);
        if (!successfulItems.isEmpty()) {
            withTenant(masterSchema, () -> inventoryTransferRepository.save(transfer));
            withTenant(sourceTenant, () -> inventoryTransferRepository.save(copyForTenant(transfer)));

            if (!sourceTenant.equals(targetTenant)) {
                withTenant(targetTenant, () -> {
                    inventoryTransferRepository.save(copyForTenant(transfer));
                    return null;
                });
            }
        }
        boolean fullySuccessful = itemResults.stream().allMatch(TransferItemResult::isSuccess);
        return new InventoryTransferResult(transfer.getTransferId(), fullySuccessful, itemResults);
    }

    /* =====================================================
       PRE-VALIDATION (UX ONLY)
       ===================================================== */

    /**
     * PRE-VALIDATION ONLY.
     * This improves UX by failing early.
     * Stock correctness is enforced during debit.
     */
    private void validateSourceStockAvailability(CreateTransferDTO transfer) throws Exception {
        withTenant(transfer.getSourceTenant(), () -> {
            List<Long> productIds = transfer.getItems()
                    .stream()
                    .map(InventoryTransferItemDTO::getProductId)
                    .collect(Collectors.toList());
            Map<Long, Double> stockMap = stockService.findStockForProductsInWarehouse(transfer.getSourceWarehouseId(), productIds, transfer.getSourceTenant());
            List<LowStockItem> lowStockItems = new ArrayList<>();
            for (InventoryTransferItemDTO item : transfer.getItems()) {
                double available = stockMap.getOrDefault(item.getProductId(), 0.0);

                if (available < item.getQuantity()) {
                    lowStockItems.add(new LowStockItem(item.getProductId(), available, item.getQuantity()));
                }
            }

            if (!lowStockItems.isEmpty()) {
                throw new InsufficientStockException(lowStockItems);
            }

            return null;
        });
    }

    private InventoryTransfer copyForTenant(InventoryTransfer original) {
        InventoryTransfer t = new InventoryTransfer();
        t.setTransferId(original.getTransferId());
        t.setSourceTenant(original.getSourceTenant());
        t.setTargetTenant(original.getTargetTenant());
        t.setSourceWarehouseId(original.getSourceWarehouseId());
        t.setTargetWarehouseId(original.getTargetWarehouseId());
        t.setSourceWarehouseName(original.getSourceWarehouseName());
        t.setTargetWarehouseName(original.getTargetWarehouseName());
        t.setTransferDate(original.getTransferDate());
        t.setRemarks(original.getRemarks());

        List<InventoryTransferItem> items = original.getItems().stream().map(i -> {
            InventoryTransferItem ni = new InventoryTransferItem();
            ni.setTransferItemId(i.getTransferItemId());
            ni.setProductId(i.getProductId());
            ni.setProductCode(i.getProductCode());
            ni.setProductName(i.getProductName());
            ni.setMeasurementUnit(i.getMeasurementUnit());
            ni.setQuantity(i.getQuantity());
            ni.setSourceClosingStock(i.getSourceClosingStock());
            ni.setTargetClosingStock(i.getTargetClosingStock());
            ni.setInventoryTransfer(t);
            return ni;
        }).collect(Collectors.toList());

        t.setItems(items);
        return t;
    }


    /* =====================================================
       ENTITY BUILDING
       ===================================================== */

    private InventoryTransfer buildTransferEntity(
            CreateTransferDTO dto,
            Warehouse source,
            Warehouse target,
            Map<Long, Product> productMap) {

        InventoryTransfer transfer = new InventoryTransfer();
        transfer.setSourceTenant(dto.getSourceTenant());
        transfer.setTargetTenant(dto.getTargetTenant());
        transfer.setSourceWarehouseId(source.getWarehouseId());
        transfer.setTargetWarehouseId(target.getWarehouseId());
        transfer.setSourceWarehouseName(source.getWarehouseName());
        transfer.setTargetWarehouseName(target.getWarehouseName());
        transfer.setTransferDate(dto.getTransferDate());
        transfer.setRemarks(dto.getRemarks());
        List<InventoryTransferItem> items = dto.getItems().stream().map(i -> {
            Product p = productMap.get(i.getProductId());

            InventoryTransferItem item = new InventoryTransferItem();
            item.setProductId(p.getProductId());
            item.setProductCode(p.getProductCode());
            item.setProductName(p.getProductName());
            item.setMeasurementUnit(p.getMeasurementUnit());
            item.setQuantity(i.getQuantity());
            item.setInventoryTransfer(transfer);
            return item;
        }).collect(Collectors.toList());

        transfer.setItems(items);
        return transfer;
    }

    private Map<Long, Product> fetchProducts(CreateTransferDTO dto, String tenantName) {

        ThreadLocalStorage.setTenantName(tenantName);
        List<Long> productIds = dto.getItems()
                .stream()
                .map(InventoryTransferItemDTO::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepo.findByProductIdIn(productIds);

        if (products.size() != productIds.size()) {
            throw new IllegalArgumentException("One or more products not found");
        }

        return products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
    }

    /* =====================================================
       VALIDATIONS
       ===================================================== */

    private void validateTransferRequest(CreateTransferDTO transfer) {

        if (transfer == null)
            throw new IllegalArgumentException("Transfer request cannot be null");

        if (transfer.getItems() == null || transfer.getItems().isEmpty())
            throw new IllegalArgumentException("At least one item is required");

        if (transfer.getItems().size() > 5)
            throw new IllegalArgumentException("Maximum 5 items allowed");

        if (transfer.getSourceTenant().equalsIgnoreCase(transfer.getTargetTenant()) && transfer.getSourceWarehouseId().equals(transfer.getTargetWarehouseId())) {
            throw new IllegalArgumentException("Source and target warehouse must be different");
        }

        for (InventoryTransferItemDTO item : transfer.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for product " + item.getProductId());
            }
        }
    }

    private void validateDuplicateProducts(CreateTransferDTO transfer) {

        Set<Long> productIds = new HashSet<>();
        for (InventoryTransferItemDTO item : transfer.getItems()) {
            if (!productIds.add(item.getProductId())) {
                throw new IllegalArgumentException("Duplicate product in transfer: " + item.getProductId());
            }
        }
    }

    private void replaceTenantNamesForSuncity(CreateTransferDTO transfer) {
        transfer.setSourceTenant(transfer.getSourceTenant());
        transfer.setTargetTenant(transfer.getTargetTenant());
    }

    /* =====================================================
       BATCH SPLIT HELPERS (#6)
       ===================================================== */

    private List<TransferredBatchData> deductSourceBatchesForTransfer(
            Long productId, Long sourceWarehouseId, Double qtyToTransfer,
            Product product, InventoryTransferItemDTO dtoItem) {

        List<BatchOverrideEntry> overrideBatches = dtoItem != null ? dtoItem.getOverrideBatches() : null;

        if (overrideBatches != null && !overrideBatches.isEmpty()) {
            // User specified exact batches to transfer
            List<TransferredBatchData> transferred = new ArrayList<>();
            for (BatchOverrideEntry entry : overrideBatches) {
                if (entry.getQty() == null || entry.getQty() <= 0) continue;
                InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId()).orElse(null);
                if (batch == null) continue;
                double consume = Math.min(entry.getQty(), batch.getQtyRemaining());
                batch.setQtyRemaining(batch.getQtyRemaining() - consume);
                inventoryBatchRepository.save(batch);
                transferred.add(new TransferredBatchData(
                        batch.getBatchId(), batch.getBrand(), batch.getLotNumber(),
                        batch.getExpiryDate(), batch.getReceivedDate(), consume));
            }
            return transferred;
        }

        // Default: FEFO for BATCH_WITH_EXPIRY, FIFO by receivedDate for BATCH_ONLY
        boolean useExpiry = product != null && product.requiresExpiry();
        List<InventoryBatch> orderedBatches = useExpiry
                ? inventoryBatchRepository.findAvailableBatchesFifoOrder(productId, sourceWarehouseId)
                : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceived(productId, sourceWarehouseId);

        double totalBatchQty = orderedBatches.stream().mapToDouble(InventoryBatch::getQtyRemaining).sum();
        // Pre-feature stock consumed silently first (true FIFO)
        double preFeatureStock = Math.max(qtyToTransfer - totalBatchQty, 0.0);
        double remaining = qtyToTransfer - preFeatureStock;

        List<TransferredBatchData> transferred = new ArrayList<>();
        for (InventoryBatch batch : orderedBatches) {
            if (remaining <= 0) break;
            double consume = Math.min(remaining, batch.getQtyRemaining());
            batch.setQtyRemaining(batch.getQtyRemaining() - consume);
            inventoryBatchRepository.save(batch);
            transferred.add(new TransferredBatchData(
                    batch.getBatchId(), batch.getBrand(), batch.getLotNumber(),
                    batch.getExpiryDate(), batch.getReceivedDate(), consume));
            remaining -= consume;
        }
        return transferred;
    }

    private void createTargetBatchesForTransfer(
            Long productId, Long targetWarehouseId, List<TransferredBatchData> transferredBatches) {

        for (TransferredBatchData td : transferredBatches) {
            InventoryBatch targetBatch = new InventoryBatch();
            targetBatch.setInwardId(0L); // sentinel: transfer-origin batch
            targetBatch.setProduct(productRepo.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId)));
            targetBatch.setWarehouse(warehouseRepo.findById(targetWarehouseId).orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + targetWarehouseId)));
            targetBatch.setBrand(td.brand);
            targetBatch.setLotNumber(td.lotNumber);
            targetBatch.setExpiryDate(td.expiryDate);
            targetBatch.setReceivedDate(td.receivedDate);
            targetBatch.setQtyReceived(td.qty);
            targetBatch.setQtyRemaining(td.qty);
            inventoryBatchRepository.save(targetBatch);
        }
    }

    private static class TransferredBatchData {
        final Long batchId;
        final String brand;
        final String lotNumber;
        final Date expiryDate;
        final Date receivedDate;
        final double qty;

        TransferredBatchData(Long batchId, String brand, String lotNumber,
                             Date expiryDate, Date receivedDate, double qty) {
            this.batchId = batchId;
            this.brand = brand;
            this.lotNumber = lotNumber;
            this.expiryDate = expiryDate;
            this.receivedDate = receivedDate;
            this.qty = qty;
        }
    }

    /**
     * Preview which batches will be deducted from source for a transfer item.
     * Mirrors deductSourceBatchesForTransfer but read-only.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> previewTransferBatches(
            String sourceTenant, Long productId, Long sourceWarehouseId, Double qty,
            List<BatchOverrideEntry> overrideBatches) throws Exception {

        return withTenant(sourceTenant, () -> {
            com.ec.application.model.Product product;
            try {
                ThreadLocalStorage.setTenantName(masterSchema);
                product = productRepo.findById(productId).orElse(null);
            } finally {
                ThreadLocalStorage.setTenantName(sourceTenant);
            }

            List<InventoryBatch> batches;
            if (overrideBatches != null && !overrideBatches.isEmpty()) {
                batches = new ArrayList<>();
                for (BatchOverrideEntry entry : overrideBatches) {
                    inventoryBatchRepository.findById(entry.getBatchId()).ifPresent(batches::add);
                }
            } else {
                boolean useExpiry = product != null && product.requiresExpiry();
                batches = useExpiry
                        ? inventoryBatchRepository.findAvailableBatchesFifoOrder(productId, sourceWarehouseId)
                        : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceived(productId, sourceWarehouseId);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            double remaining = qty;
            for (InventoryBatch b : batches) {
                if (remaining <= 0) break;
                double consume = (overrideBatches != null && !overrideBatches.isEmpty())
                        ? overrideBatches.stream()
                                .filter(e -> e.getBatchId().equals(b.getBatchId()))
                                .mapToDouble(e -> e.getQty() != null ? e.getQty() : 0).sum()
                        : Math.min(remaining, b.getQtyRemaining());
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("batchId", b.getBatchId());
                item.put("brand", b.getBrand());
                item.put("lotNumber", b.getLotNumber());
                item.put("expiryDate", b.getExpiryDate());
                item.put("receivedDate", b.getReceivedDate());
                item.put("qtyAvailable", b.getQtyRemaining());
                item.put("qtyToTransfer", consume);
                result.add(item);
                remaining -= consume;
            }
            return result;
        });
    }

    /* =====================================================
       TENANT HELPER
       ===================================================== */

    private <T> T withTenant(String tenant, Callable<T> action) throws Exception {
        try {
            ThreadLocalStorage.setTenantName(tenant);
            return action.call();
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    public ReturnInventoryTransferData fetchTransfers(FilterDataList filterDataList, Pageable pageable) throws ParseException {
        ReturnInventoryTransferData returnData = new ReturnInventoryTransferData();
        Specification<InventoryTransfer> spec = InventoryTransferSpecification.getSpecification(filterDataList);
        Page<InventoryTransfer> page = (spec != null) ? inventoryTransferRepository.findAll(spec, pageable) : inventoryTransferRepository.findAll(pageable);
        returnData.setInventoryTransfers(page);
        returnData.setItDropdown(populateDropdownService.fetchData("inventorytransfer"));
        returnData.setMaxAllowedInventory(5);
        return returnData;
    }

    public ResponseEntity<CurrentStockResponse> fetchCurrentStock(
            String tenant, Long productId, Long warehouseId) {

        try {
            ThreadLocalStorage.setTenantName(tenant);

            Double stock = stockService.findStockForProductWarehouse(productId, warehouseId);
            return ResponseEntity.ok(new CurrentStockResponse(tenant, productId, warehouseId, stock == null ? 0.0 : stock));
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    public List<CurrentStockResponse> fetchCurrentStockInBulk(BulkCurrentStockRequest request) throws Exception {
        try {
            String sourceTenant = request.getTenant();
            ThreadLocalStorage.setTenantName(sourceTenant);
            Map<Long, Double> stockMap = stockService.findStockForProductsInWarehouse(request.getWarehouseId(), request.getProductIds(), sourceTenant);
            List<CurrentStockResponse> response = new ArrayList<>();

            for (Long productId : request.getProductIds()) {
                response.add(new CurrentStockResponse(request.getTenant(), productId, request.getWarehouseId(), stockMap.getOrDefault(productId, 0.0)));
            }
            return response;
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    public InventoryTransfer getOneInventoryTransfer(Long transferId) {
        return inventoryTransferRepository.findById(transferId).orElseThrow(() -> new IllegalArgumentException("Inventory Transfer not found"));
    }
}
