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

    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        log.info("Starting low stock sync for tenant: {}", tenantSchema);

        // ── Step 1: Load StockSummary from master schema ─────────────────────
        ThreadLocalStorage.setTenantName(masterSchema);
        List<StockSummary> allRows = stockSummaryRepo.findByTenantSchema(tenantSchema);

        // Filter: reorderLevel > 0 AND qtyInHand < reorderLevel
        List<StockSummary> lowStockRows = allRows.stream()
                .filter(s -> s.getReorderLevel() != null
                          && s.getReorderLevel() > 0.0
                          && s.getQuantityInHand() != null
                          && s.getQuantityInHand() < s.getReorderLevel())
                .collect(Collectors.toList());

        // If no low-stock products, remove all existing rows for this tenant
        if (lowStockRows.isEmpty()) {
            log.info("No low-stock products for tenant: {}. Clearing existing rows.", tenantSchema);
            lowStockRepo.deleteByTenantSchema(tenantSchema);
            return;
        }

        List<Long> lowStockProductIds = lowStockRows.stream()
                .map(StockSummary::getProductId)
                .distinct()
                .collect(Collectors.toList());

        // ── Step 2: Load product metadata ────────────────────────────────────
        Map<Long, Product> productMap = productRepo.findAllById(lowStockProductIds)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        // ── Step 3: Aggregate per product (sum qty across warehouses, use product-level reorderLevel) ─
        // StockSummary has one row per product×warehouse; reorderLevel is same across all warehouse
        // rows for a given (tenant, product). We sum qty and use the reorderLevel from any row.
        Map<Long, Double> qtyByProduct = new HashMap<>();
        Map<Long, Double> reorderByProduct = new HashMap<>();

        for (StockSummary s : lowStockRows) {
            qtyByProduct.merge(s.getProductId(), s.getQuantityInHand(), Double::sum);
            reorderByProduct.putIfAbsent(s.getProductId(), s.getReorderLevel());
        }

        Date syncedAt = new Date();
        int inserted = 0, updated = 0;

        // ── Step 4: Upsert low-stock rows — preserve lowStockSince if already exists ─
        for (Long productId : lowStockProductIds) {
            double totalQty   = qtyByProduct.getOrDefault(productId, 0.0);
            double reorder    = reorderByProduct.getOrDefault(productId, 0.0);

            // Re-check after aggregation (sum of all warehouses might be above reorder)
            if (totalQty >= reorder) continue;

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

        // ── Step 5: Remove products that recovered (back above reorder) ──────
        // Use the filtered lowStockProductIds (post-aggregation check)
        lowStockRepo.deleteRecoveredProducts(tenantSchema, lowStockProductIds);

        log.info("Low stock sync done for {}. Inserted={}, Updated={}", tenantSchema, inserted, updated);
    }
}
