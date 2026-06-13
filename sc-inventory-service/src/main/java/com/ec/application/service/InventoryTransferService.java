package com.ec.application.service;

import com.ec.application.constants.ProjectConstants;
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
    private final BatchTrackingService batchTrackingService;
    private final StockCommentService stockCommentService;
    private final ActivityLogService activityLogService;
    private final UserDetailsService userDetailsService;

    @Value("${master.schema}")
    private String masterSchema;

    /* =====================================================
       MAIN API
       ===================================================== */

    public InventoryTransferResult createTransfer(CreateTransferDTO dto) throws Exception {
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

            // Declare compensation lists outside all try blocks so the catch can always see them,
            // even when helpers threw mid-loop. Helpers populate these incrementally (not on return)
            // so partial saves are visible to the compensation path.
            final List<TransferredBatchData> transferredBatches = new ArrayList<>();
            final List<Long> createdTargetBatchIds = new ArrayList<>();

            try {
                // DEBIT SOURCE
                Double sourceClosingStock = withTenant(sourceTenant, () ->
                        stockService.updateStock(item.getProductId(), sourceWarehouse.getWarehouseId(), item.getQuantity(), "outward"));

                // Track whether credit succeeded so we know what to roll back
                final Double[] targetClosingStockRef = {null};

                try {
                    // CREDIT TARGET
                    targetClosingStockRef[0] = withTenant(targetTenant, () ->
                            stockService.updateStock(item.getProductId(), targetWarehouse.getWarehouseId(), item.getQuantity(), "inward"));

                    // BATCH — helpers populate transferredBatches / createdTargetBatchIds
                    // incrementally so compensation sees partial saves if a helper throws mid-loop.
                    final InventoryTransferItemDTO dtoItem = dto.getItems().stream()
                            .filter(d -> d.getProductId().equals(item.getProductId()))
                            .findFirst().orElse(null);
                    final Product sourceProduct = productMap.get(item.getProductId());

                    withTenant(sourceTenant, () -> {
                        deductSourceBatchesForTransfer(
                                item.getProductId(), sourceWarehouse.getWarehouseId(), item.getQuantity(),
                                sourceProduct, dtoItem, transferredBatches);
                        return null;
                    });
                    withTenant(targetTenant, () -> {
                        createTargetBatchesForTransfer(
                                item.getProductId(), targetWarehouse.getWarehouseId(), transferredBatches,
                                sourceProduct, createdTargetBatchIds);
                        return null;
                    });

                    // SET CLOSING STOCKS ONLY ON FULL SUCCESS
                    item.setSourceClosingStock(sourceClosingStock);
                    item.setTargetClosingStock(targetClosingStockRef[0]);
                    successfulItems.add(item);
                    itemResults.add(new TransferItemResult(item.getProductId(), true, "Transfer successful"));

                } catch (Exception creditOrBatchEx) {
                    // COMPENSATE CREDIT if it already went through
                    if (targetClosingStockRef[0] != null) {
                        try {
                            withTenant(targetTenant, () -> {
                                stockService.updateStock(item.getProductId(), targetWarehouse.getWarehouseId(), item.getQuantity(), "outward");
                                return null;
                            });
                        } catch (Exception reverseEx) {
                            log.error("Failed to reverse target credit for productId={}: {}", item.getProductId(), reverseEx.getMessage());
                        }
                    }
                    // COMPENSATE DEBIT
                    try {
                        withTenant(sourceTenant, () -> {
                            stockService.updateStock(item.getProductId(), sourceWarehouse.getWarehouseId(), item.getQuantity(), "inward");
                            return null;
                        });
                    } catch (Exception reverseEx) {
                        log.error("Failed to reverse source debit for productId={}: {}", item.getProductId(), reverseEx.getMessage());
                    }
                    // REVERSE SOURCE BATCH DEDUCTIONS — visible even if helper threw mid-loop
                    if (!transferredBatches.isEmpty()) {
                        try {
                            withTenant(sourceTenant, () -> {
                                for (TransferredBatchData tbd : transferredBatches) {
                                    inventoryBatchRepository.findById(tbd.batchId).ifPresent(b -> {
                                        b.setQtyRemaining(b.getQtyRemaining() + tbd.qty);
                                        inventoryBatchRepository.save(b);
                                    });
                                }
                                return null;
                            });
                        } catch (Exception reverseEx) {
                            log.error("Failed to reverse source batch deductions for productId={}: {}", item.getProductId(), reverseEx.getMessage());
                        }
                    }
                    // SOFT-DELETE PARTIALLY-CREATED TARGET BATCHES
                    if (!createdTargetBatchIds.isEmpty()) {
                        try {
                            withTenant(targetTenant, () -> {
                                for (Long bid : createdTargetBatchIds) {
                                    inventoryBatchRepository.findById(bid).ifPresent(b -> {
                                        b.setDeleted(true);
                                        b.setQtyRemaining(0.0);
                                        inventoryBatchRepository.save(b);
                                    });
                                }
                                return null;
                            });
                        } catch (Exception reverseEx) {
                            log.error("Failed to delete created target batches for productId={}: {}", item.getProductId(), reverseEx.getMessage());
                        }
                    }
                    itemResults.add(new TransferItemResult(item.getProductId(), false, creditOrBatchEx.getMessage()));
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

        if (!successfulItems.isEmpty()) {
            logTransferActivity(transfer, sourceWarehouse, targetWarehouse, successfulItems, sourceTenant);
        }

        return new InventoryTransferResult(transfer.getTransferId(), fullySuccessful, itemResults);
    }

    private void logTransferActivity(InventoryTransfer transfer, Warehouse sourceWarehouse, Warehouse targetWarehouse,
                                      List<InventoryTransferItem> successfulItems, String sourceTenant) {
        String user = resolveCurrentUser();
        boolean involvesDeadStock = ProjectConstants.deadStockWarehouseName.equals(sourceWarehouse.getWarehouseName())
                || ProjectConstants.deadStockWarehouseName.equals(targetWarehouse.getWarehouseName());

        boolean toDeadStock = ProjectConstants.deadStockWarehouseName.equals(targetWarehouse.getWarehouseName());
        for (InventoryTransferItem item : successfulItems) {
            String productName = item.getProductName() != null ? item.getProductName() : "Product";
            Long productId = item.getProductId();

            try {
                String desc = com.ec.application.ReusableClasses.ActivityLogDescription.of(
                        "Transfer #" + transfer.getTransferId() + " — " + user
                                + " moved " + item.getQuantity() + " units of " + productName
                                + " from " + sourceWarehouse.getWarehouseName()
                                + " to " + targetWarehouse.getWarehouseName());
                activityLogService.record("CREATED", "INVENTORY_TRANSFER",
                        String.valueOf(transfer.getTransferId()), desc, user);
            } catch (Exception e) {
                log.warn("Failed to log activity for transfer item productId={}", productId, e);
            }

            if (involvesDeadStock) {
                try {
                    String commentText = "Transfer #" + transfer.getTransferId() + " — " + user
                            + " moved " + item.getQuantity() + " units "
                            + (toDeadStock ? "to Dead Stock Warehouse" : "from Dead Stock Warehouse")
                            + " on " + new java.text.SimpleDateFormat("dd-MM-yyyy").format(transfer.getTransferDate());
                    stockCommentService.addSystemComment(productId, productName, commentText, "TRANSFER_AUTO",
                            transfer.getTransferId());
                } catch (Exception e) {
                    log.warn("Failed to create auto stock comment for transfer item productId={}", productId, e);
                }
            }
        }
    }

    private String resolveCurrentUser() {
        try { return userDetailsService.getCurrentUser().getUsername(); }
        catch (Exception e) { return "System"; }
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

    /* =====================================================
       BATCH SPLIT HELPERS (#6)
       ===================================================== */

    /**
     * Deducts source batches for a transfer item.
     * Populates {@code out} incrementally — each batch is added immediately after its save —
     * so the caller's compensation path can reverse partial saves even if this method throws mid-loop.
     */
    private void deductSourceBatchesForTransfer(
            Long productId, Long sourceWarehouseId, Double qtyToTransfer,
            Product product, InventoryTransferItemDTO dtoItem,
            List<TransferredBatchData> out) {

        // Untracked-stock guard — runs for BOTH override and FIFO paths.
        // Must execute before the override early-return so manual batch selection
        // cannot bypass it when untracked stock exists.
        if (product != null && product.isBatchTracked()) {
            List<InventoryBatch> allBatches = product.requiresExpiry()
                    ? inventoryBatchRepository.findAvailableBatchesFifoOrder(productId, sourceWarehouseId)
                    : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceived(productId, sourceWarehouseId);
            double totalBatchQty = allBatches.stream().mapToDouble(InventoryBatch::getQtyRemaining).sum();
            Double currentStock = stockService.findStockForProductWarehouse(productId, sourceWarehouseId);
            double originalStock = (currentStock != null ? currentStock : 0.0) + qtyToTransfer;
            double untrackedStock = originalStock - totalBatchQty;
            if (untrackedStock > 0.001) {
                throw new IllegalArgumentException(
                        "Product '" + product.getProductName() + "' has "
                        + String.format("%.3f", untrackedStock)
                        + " units of untracked stock in the source warehouse. "
                        + "Go to Stock → Batches tab and split existing stock into batches first.");
            }
        }

        List<BatchOverrideEntry> overrideBatches = dtoItem != null ? dtoItem.getOverrideBatches() : null;

        if (overrideBatches != null && !overrideBatches.isEmpty()) {
            // Filter to valid entries only — skip null/zero-qty to prevent sum-check bypass
            List<BatchOverrideEntry> validEntries = overrideBatches.stream()
                    .filter(e -> e.getBatchId() != null && e.getQty() != null && e.getQty() > 0)
                    .collect(Collectors.toList());
            if (validEntries.isEmpty()) {
                throw new IllegalArgumentException("No valid batch entries provided for transfer override.");
            }
            // Duplicate batchId check
            Set<Long> seenBatchIds = new HashSet<>();
            for (BatchOverrideEntry e : validEntries) {
                if (!seenBatchIds.add(e.getBatchId())) {
                    throw new IllegalArgumentException("Duplicate batch ID " + e.getBatchId() + " in transfer override.");
                }
            }
            // PASS 1: validate total + per-batch ownership + availability — NO SAVES YET
            double totalConsumed = validEntries.stream().mapToDouble(BatchOverrideEntry::getQty).sum();
            if (Math.abs(totalConsumed - qtyToTransfer) > 0.001) {
                throw new IllegalArgumentException(
                        "Override batch total (" + totalConsumed
                        + ") does not equal transfer quantity (" + qtyToTransfer + ").");
            }
            for (BatchOverrideEntry entry : validEntries) {
                InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                        .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                // Ownership: product must match
                if (!batch.getProduct().getProductId().equals(productId)) {
                    throw new IllegalArgumentException(
                            "Batch #" + entry.getBatchId() + " belongs to product '"
                            + batch.getProduct().getProductName() + "', not the transferred product.");
                }
                // Ownership: warehouse must match source
                if (!batch.getWarehouse().getWarehouseId().equals(sourceWarehouseId)) {
                    throw new IllegalArgumentException(
                            "Batch #" + entry.getBatchId() + " belongs to warehouse '"
                            + batch.getWarehouse().getWarehouseName() + "', not the source warehouse.");
                }
                if (entry.getQty() > batch.getQtyRemaining() + 0.001) {
                    throw new IllegalArgumentException(
                            "Batch #" + entry.getBatchId() + " has only "
                            + String.format("%.3f", batch.getQtyRemaining())
                            + " units remaining — requested " + entry.getQty() + " for transfer.");
                }
            }
            // PASS 2: all validations passed — save and populate out incrementally
            for (BatchOverrideEntry entry : validEntries) {
                InventoryBatch batch = inventoryBatchRepository.findById(entry.getBatchId())
                        .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + entry.getBatchId()));
                batch.setQtyRemaining(batch.getQtyRemaining() - entry.getQty());
                inventoryBatchRepository.save(batch);
                out.add(new TransferredBatchData(batch.getBatchId(), batch.getBrand(), batch.getLotNumber(),
                        batch.getExpiryDate(), batch.getReceivedDate(), entry.getQty()));
            }
            return; // override path done
        }

        // Not batch-tracked — nothing to deduct
        if (product == null || !product.isBatchTracked()) {
            return;
        }

        // FIFO / FEFO path — orderedBatches already fetched by the guard above; re-fetch for clarity
        boolean useExpiry = product.requiresExpiry();
        List<InventoryBatch> orderedBatches = useExpiry
                ? inventoryBatchRepository.findAvailableBatchesFifoOrder(productId, sourceWarehouseId)
                : inventoryBatchRepository.findAvailableBatchesFifoOrderByReceived(productId, sourceWarehouseId);

        double remaining = qtyToTransfer;
        for (InventoryBatch batch : orderedBatches) {
            if (remaining <= 0) break;
            double consume = Math.min(remaining, batch.getQtyRemaining());
            batch.setQtyRemaining(batch.getQtyRemaining() - consume);
            inventoryBatchRepository.save(batch);
            // Populate incrementally so compensation sees partial saves if we throw later
            out.add(new TransferredBatchData(batch.getBatchId(), batch.getBrand(), batch.getLotNumber(),
                    batch.getExpiryDate(), batch.getReceivedDate(), consume));
            remaining -= consume;
        }
        if (remaining > 0.001) {
            throw new IllegalArgumentException(
                    "Insufficient batch quantity. " + remaining + " units could not be allocated from available batches.");
        }
    }

    /**
     * Creates target batches for a transfer.
     * Populates {@code createdIds} incrementally — each ID is added immediately after its save —
     * so the caller's compensation path can soft-delete partial creates even if this method throws mid-loop.
     */
    private void createTargetBatchesForTransfer(
            Long productId, Long targetWarehouseId, List<TransferredBatchData> transferredBatches,
            Product product, List<Long> createdIds) {

        if (transferredBatches.isEmpty()) return;
        Warehouse targetWarehouse = warehouseRepo.findById(targetWarehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + targetWarehouseId));
        for (TransferredBatchData td : transferredBatches) {
            InventoryBatch targetBatch = new InventoryBatch();
            targetBatch.setInwardId(0L); // sentinel: transfer-origin batch
            targetBatch.setProduct(product);
            targetBatch.setWarehouse(targetWarehouse);
            targetBatch.setBrand(td.brand);
            targetBatch.setLotNumber(td.lotNumber);
            targetBatch.setExpiryDate(td.expiryDate);
            targetBatch.setReceivedDate(td.receivedDate);
            targetBatch.setQtyReceived(td.qty);
            targetBatch.setQtyRemaining(td.qty);
            InventoryBatch saved = inventoryBatchRepository.save(targetBatch);
            createdIds.add(saved.getBatchId()); // incremental — visible to compensation even on throw
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

        // Fetch product from master schema before switching tenant — avoids connection reuse issue
        // with AbstractRoutingDataSource (connection bound at masterschema would be reused for batch query)
        com.ec.application.model.Product product;
        try {
            ThreadLocalStorage.setTenantName(masterSchema);
            product = productRepo.findById(productId).orElse(null);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
        final boolean useExpiry = product != null && product.requiresExpiry();

        return withTenant(sourceTenant, () -> {
            List<InventoryBatch> batches;
            if (overrideBatches != null && !overrideBatches.isEmpty()) {
                List<Long> ids = overrideBatches.stream()
                        .filter(e -> e.getBatchId() != null)
                        .map(BatchOverrideEntry::getBatchId)
                        .collect(java.util.stream.Collectors.toList());
                batches = batchTrackingService.fetchBatchesByIdsNewTx(ids);
            } else {
                batches = batchTrackingService.fetchAvailableBatchesNewTx(productId, sourceWarehouseId, useExpiry);
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
