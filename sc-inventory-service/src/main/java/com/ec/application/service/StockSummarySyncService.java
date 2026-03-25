package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.StockSummaryDTO;
import com.ec.application.model.Product;
import com.ec.application.model.ProductTenantConfig;
import com.ec.application.model.StockSummary;
import com.ec.application.model.Stock;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.ProductTenantConfigRepository;
import com.ec.application.repository.StockSummaryRepo;
import com.ec.application.repository.StockRepo;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockSummarySyncService {

    private final StockSummaryRepo stockSummaryRepo;
    private final StockRepo stockRepo;
    private final ProductRepo productRepo;
    private final ProductTenantConfigRepository configRepo;
    private final SchemaConfig schemaConfig;
    private final TenantService tenantService;

    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        System.out.println("▶️ Starting Stock sync for tenant: " + tenantSchema);

        try {
            // ---------- Step 1: Get last sync time from MASTER ----------
            ThreadLocalStorage.setTenantName(masterSchema);
            Date lastSyncTime = stockSummaryRepo.findLastSyncTimeByTenantSchema(tenantSchema);
            if (lastSyncTime == null) {
                lastSyncTime = new Date(0); // 01-01-1970 00:00:00
                System.out.println("⏱ Last sync time not found. Using minimum date: " + lastSyncTime);
            } else {
                System.out.println("⏱ Last sync time: " + lastSyncTime);
            }

            // ---------- Step 2: Fetch stocks from TENANT ----------
            ThreadLocalStorage.setTenantName(tenantSchema);
            List<Stock> stocks = stockRepo.findStocksUpdatedAfter(lastSyncTime);

            System.out.println("Stock records fetched: " + stocks.size());

            if (!stocks.isEmpty()) {
                // ---------- Step 3: Map to DTO ----------
                List<StockSummaryDTO> stocksDTOs = mapStockToStockDTO(stocks, tenantSchema);

                // ---------- Step 4: Persist into MASTER ----------
                ThreadLocalStorage.setTenantName(masterSchema);

                Date now = new Date();
                int created = 0;
                int updated = 0;

                for (StockSummaryDTO stock : stocksDTOs) {
                    boolean isNew = findOrCreateAndUpdateStockSummary(stock, now);
                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }
                }
                System.out.println("Stock sync completed for tenant: " + tenantSchema + " | Created: " + created + " | Updated: " + updated);
            } else {
                System.out.println("No stock changes for tenant: " + tenantSchema);
            }

            // ---------- Step 5: Always sync reorder levels (runs even when no stock changed) ----------
            syncReorderLevels(tenantSchema, masterSchema);

            // ---------- Step 6: Always sync measurement units (runs even when no stock changed) ----------
            syncMeasurementUnits(tenantSchema, masterSchema);

        } catch (Exception e) {
            System.out.println("Stock sync FAILED for tenant: " + tenantSchema);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Syncs reorder_level for every (tenantSchema, productId) row in stock_summary.
     * Always executed — reorder level can change independently of stock movements.
     *
     * Logic:
     *  1. From MASTER: get all productIds that already have a stock_summary row for this tenant.
     *  2. From MASTER: load global Product.reorderQuantity for those products.
     *  3. From TENANT: load any tenant-specific overrides from product_tenant_config.
     *  4. Compute effective = override if set, else global.
     *  5. Bulk-update stock_summary.reorder_level in MASTER (one @Modifying query per product).
     */
    private void syncReorderLevels(String tenantSchema, String masterSchema) {
        System.out.println("🔄 Syncing reorder levels for tenant: " + tenantSchema);

        // Step 1: productIds with existing stock rows (MASTER)
        ThreadLocalStorage.setTenantName(masterSchema);
        List<Long> productIds = stockSummaryRepo.findDistinctProductIdsByTenantSchema(tenantSchema);
        if (productIds.isEmpty()) {
            System.out.println("No stock rows for tenant: " + tenantSchema + " — skipping reorder level sync");
            return;
        }

        // Step 2: global reorder quantities (MASTER — Products live in master schema)
        List<Product> products = productRepo.findByProductIdIn(productIds);
        Map<Long, Double> globalDefaults = products.stream()
                .collect(Collectors.toMap(
                        Product::getProductId,
                        p -> p.getReorderQuantity() != null ? p.getReorderQuantity() : 0.0
                ));

        // Step 3: tenant-specific overrides (TENANT schema)
        ThreadLocalStorage.setTenantName(tenantSchema);
        List<ProductTenantConfig> overrideList = configRepo.findByProductIds(productIds);
        Map<Long, Double> overrides = overrideList.stream()
                .filter(c -> c.getReorderLevel() != null)
                .collect(Collectors.toMap(ProductTenantConfig::getProductId,
                                          ProductTenantConfig::getReorderLevel));

        // Step 4 + 5: compute effective levels and persist in MASTER
        Map<Long, Double> effectiveLevels = new HashMap<>();
        for (Long productId : productIds) {
            Double effective = overrides.containsKey(productId)
                    ? overrides.get(productId)
                    : globalDefaults.getOrDefault(productId, 0.0);
            effectiveLevels.put(productId, effective);
        }

        // Switch back to master schema before persisting — @UseDefaultTenant on
        // persistReorderLevels is bypassed (same-class call), so we set it manually.
        ThreadLocalStorage.setTenantName(masterSchema);
        persistReorderLevels(tenantSchema, productIds, effectiveLevels);

        System.out.println("✅ Reorder level sync done for tenant: " + tenantSchema + " | Products: " + productIds.size());
    }

    /**
     * Persists computed reorder levels into stock_summary (MASTER schema).
     * Runs in its own transaction so @Modifying queries are committed.
     */
    @UseDefaultTenant
    @Transactional
    void persistReorderLevels(String tenantSchema, List<Long> productIds, Map<Long, Double> effectiveLevels) {
        for (Long productId : productIds) {
            Double level = effectiveLevels.get(productId);
            if (level != null) {
                stockSummaryRepo.updateReorderLevel(tenantSchema, productId, level);
            }
        }
    }

    /**
     * Syncs measurement_unit for every (tenantSchema, productId) row in stock_summary.
     * Always executed — measurement unit can change on the Product independently of stock movements.
     *
     * Logic:
     *  1. From MASTER: get all productIds that already have a stock_summary row for this tenant.
     *  2. From MASTER: load current Product.measurementUnit for those products.
     *  3. Bulk-update stock_summary.measurement_unit in MASTER.
     */
    private void syncMeasurementUnits(String tenantSchema, String masterSchema) {
        System.out.println("🔄 Syncing measurement units for tenant: " + tenantSchema);

        // Step 1: productIds with existing stock rows (MASTER)
        ThreadLocalStorage.setTenantName(masterSchema);
        List<Long> productIds = stockSummaryRepo.findDistinctProductIdsByTenantSchema(tenantSchema);
        if (productIds.isEmpty()) {
            System.out.println("No stock rows for tenant: " + tenantSchema + " — skipping measurement unit sync");
            return;
        }

        // Step 2: current measurement units from Product (MASTER — Products live in master schema)
        List<Product> products = productRepo.findByProductIdIn(productIds);
        Map<Long, String> measurementUnits = products.stream()
                .collect(Collectors.toMap(
                        Product::getProductId,
                        p -> p.getMeasurementUnit() != null ? p.getMeasurementUnit() : ""
                ));

        // Step 3: persist in MASTER
        persistMeasurementUnits(tenantSchema, productIds, measurementUnits);

        System.out.println("✅ Measurement unit sync done for tenant: " + tenantSchema + " | Products: " + productIds.size());
    }

    /**
     * Persists computed measurement units into stock_summary (MASTER schema).
     * Runs in its own transaction so @Modifying queries are committed.
     */
    @UseDefaultTenant
    @Transactional
    void persistMeasurementUnits(String tenantSchema, List<Long> productIds, Map<Long, String> measurementUnits) {
        for (Long productId : productIds) {
            String unit = measurementUnits.get(productId);
            if (unit != null) {
                stockSummaryRepo.updateMeasurementUnit(tenantSchema, productId, unit);
            }
        }
    }

    // ------------------------------------------------------------
    // MASTER schema operation
    // ------------------------------------------------------------
    @UseDefaultTenant
    @Transactional
    private boolean findOrCreateAndUpdateStockSummary(StockSummaryDTO stock, Date syncedAt) {
        List<StockSummary> existing = stockSummaryRepo.findByTenantSchemaAndProductIdAndWarehouseId(stock.getTenantSchema(), stock.getProductId(), stock.getWarehouseId());

        // ---------- CREATE ----------
        if (existing == null || existing.isEmpty()) {
            StockSummary newSummary = buildStockSummary(stock, syncedAt);
            stockSummaryRepo.save(newSummary);
            System.out.println("➕ Created StockSummary | Product: " + stock.getProductId());
            return true;
        }

        // ---------- UPDATE ----------
        StockSummary summary = existing.get(0);
        summary.setQuantityInHand(stock.getQuantityInHand());
        summary.setMeasurementUnit(stock.getMeasurementUnit());
        summary.setSyncedAt(syncedAt);
        stockSummaryRepo.save(summary);
        System.out.println("✏️ Updated StockSummary | Product: " + stock.getProductId());
        return false;
    }

    private StockSummary buildStockSummary(StockSummaryDTO stock, Date syncedAt) {
        StockSummary summary = new StockSummary();
        summary.setTenantSchema(stock.getTenantSchema());
        summary.setProductId(stock.getProductId());
        summary.setProductName(stock.getProductName());
        summary.setProductCode(stock.getProductCode());
        summary.setWarehouseId(stock.getWarehouseId());
        summary.setWarehouseName(stock.getWarehouseName());
        summary.setQuantityInHand(stock.getQuantityInHand());
        summary.setMeasurementUnit(stock.getMeasurementUnit());
        summary.setSyncedAt(syncedAt);
        return summary;
    }

    // ------------------------------------------------------------
    // Tenant-side mapping
    // ------------------------------------------------------------
    private List<StockSummaryDTO> mapStockToStockDTO(List<Stock> stocks, String tenantSchema) {
        List<StockSummaryDTO> result = new ArrayList<>(stocks.size());
        for (Stock stock : stocks) {
            if (stock == null || stock.getProduct() == null || stock.getWarehouse() == null) {
                continue;
            }
            StockSummaryDTO dto = new StockSummaryDTO();
            dto.setTenantSchema(tenantSchema);
            dto.setTenantCode(tenantSchema);
            dto.setProductId(stock.getProduct().getProductId());
            dto.setProductCode(stock.getProduct().getProductCode());
            dto.setProductName(stock.getProduct().getProductName());
            dto.setWarehouseId(stock.getWarehouse().getWarehouseId());
            dto.setWarehouseName(stock.getWarehouse().getWarehouseName());
            dto.setQuantityInHand(stock.getQuantityInHand());
            dto.setMeasurementUnit(stock.getProduct().getMeasurementUnit());
            result.add(dto);
        }
        return result;
    }
}