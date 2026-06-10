package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LowStockSyncService {

    private static final Logger log = LoggerFactory.getLogger(LowStockSyncService.class);

    private final SchemaConfig schemaConfig;
    private final StockSummaryRepo stockSummaryRepo;
    private final ProductRepo productRepo;
    private final GlobalLowStockReportRepository lowStockRepo;
    private final ProductTenantConfigRepository configRepo;

    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        log.info("Starting low stock sync for tenant: {}", tenantSchema);

        // ── Step 1: Load StockSummary from master schema ─────────────────────
        ThreadLocalStorage.setTenantName(masterSchema);
        List<StockSummary> allRows = stockSummaryRepo.findByTenantSchema(tenantSchema);

        if (allRows.isEmpty()) {
            log.info("No stock rows for tenant: {}. Clearing existing rows.", tenantSchema);
            lowStockRepo.deleteByTenantSchema(tenantSchema);
            return;
        }

        // ── Step 2: Load project-level reorder overrides from TENANT schema ──
        // ProductTenantConfig lives in each tenant schema and overrides the global
        // Product.reorderQuantity. We read this directly to avoid depending on
        // StockSummary.reorderLevel being up-to-date.
        List<Long> allProductIds = allRows.stream()
                .map(StockSummary::getProductId).distinct().collect(Collectors.toList());

        ThreadLocalStorage.setTenantName(tenantSchema);
        List<ProductTenantConfig> overrideList = configRepo.findByProductIds(allProductIds);
        Map<Long, Double> tenantOverrides = overrideList.stream()
                .filter(c -> c.getReorderLevel() != null && c.getReorderLevel() > 0.0)
                .collect(Collectors.toMap(ProductTenantConfig::getProductId,
                                          ProductTenantConfig::getReorderLevel));

        // ── Step 3: Compute effective reorder level per product ───────────────
        // Priority: tenant-specific config > StockSummary.reorderLevel (which holds
        // the global Product.reorderQuantity as fallback, already synced by StockSummarySyncService)
        Map<Long, Double> globalFallback = new HashMap<>();
        for (StockSummary s : allRows) {
            globalFallback.putIfAbsent(s.getProductId(),
                    s.getReorderLevel() != null ? s.getReorderLevel() : 0.0);
        }

        // ── Step 4: Filter rows where qty < effective reorder level ───────────
        // Aggregate qty per product first (one row per product×warehouse in StockSummary)
        Map<Long, Double> qtyByProduct = new HashMap<>();
        for (StockSummary s : allRows) {
            if (s.getQuantityInHand() != null) {
                qtyByProduct.merge(s.getProductId(), s.getQuantityInHand(), Double::sum);
            }
        }

        List<Long> lowStockProductIds = new ArrayList<>();
        for (Long productId : allProductIds) {
            double totalQty = qtyByProduct.getOrDefault(productId, 0.0);
            double effectiveReorder = tenantOverrides.containsKey(productId)
                    ? tenantOverrides.get(productId)
                    : globalFallback.getOrDefault(productId, 0.0);
            if (effectiveReorder > 0.0 && totalQty < effectiveReorder) {
                lowStockProductIds.add(productId);
            }
        }

        // If no low-stock products, remove all existing rows for this tenant
        if (lowStockProductIds.isEmpty()) {
            log.info("No low-stock products for tenant: {}. Clearing existing rows.", tenantSchema);
            lowStockRepo.deleteByTenantSchema(tenantSchema);
            return;
        }

        // ── Step 5: Load product metadata (fresh — picks up renames/unit changes) ─
        ThreadLocalStorage.setTenantName(masterSchema);
        Map<Long, Product> productMap = productRepo.findAllById(lowStockProductIds)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        Date syncedAt = new Date();
        int inserted = 0, updated = 0;

        // ── Step 6: Upsert low-stock rows — preserve lowStockSince on updates ─
        for (Long productId : lowStockProductIds) {
            double totalQty = qtyByProduct.getOrDefault(productId, 0.0);
            double reorder  = tenantOverrides.containsKey(productId)
                    ? tenantOverrides.get(productId)
                    : globalFallback.getOrDefault(productId, 0.0);

            Product product = productMap.get(productId);

            Optional<GlobalLowStockReport> existing = lowStockRepo
                    .findByTenantSchemaAndProductId(tenantSchema, productId);

            GlobalLowStockReport row = existing.orElseGet(GlobalLowStockReport::new);
            boolean isNew = !existing.isPresent();

            row.setTenantSchema(tenantSchema);
            row.setProductId(productId);
            row.setProductName(product != null ? product.getProductName() : "Unknown");
            row.setProductCode(product != null ? product.getProductCode() : null);
            row.setUnit(product != null ? product.getMeasurementUnit() : null);
            row.setCategory(product != null && product.getCategory() != null
                    ? product.getCategory().getCategoryName() : null);
            row.setQtyInHand(totalQty);
            row.setReorderLevel(reorder);
            row.setDeficit(reorder - totalQty);
            // Only set lowStockSince on first detection — preserve on updates
            if (isNew) row.setLowStockSince(syncedAt);
            row.setSyncedAt(syncedAt);

            lowStockRepo.save(row);
            if (isNew) inserted++; else updated++;
        }

        // ── Step 7: Remove products that recovered (back above reorder) ───────
        lowStockRepo.deleteRecoveredProducts(tenantSchema, lowStockProductIds);

        log.info("Low stock sync done for {}. Inserted={}, Updated={}", tenantSchema, inserted, updated);
    }
}
