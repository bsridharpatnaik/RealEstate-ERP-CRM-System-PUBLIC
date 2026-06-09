package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockAgingSyncService {

    private static final Logger log = LoggerFactory.getLogger(StockAgingSyncService.class);

    private final SchemaConfig schemaConfig;
    private final StockSummaryRepo stockSummaryRepo;
    private final ProductRepo productRepo;
    private final GlobalStockAgingReportRepository agingReportRepo;
    private final GlobalStockAgingDetailRepository agingDetailRepo;

    @PersistenceContext
    private EntityManager em;

    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        log.info("Starting stock aging sync for tenant: {}", tenantSchema);

        // ── Step 1: Load stock rows from MASTER schema (StockSummary is master-schema table) ──
        ThreadLocalStorage.setTenantName(masterSchema);
        List<StockSummary> stockRows = stockSummaryRepo.findByTenantSchema(tenantSchema);

        // Filter to only positive qty (exclude zero-stock rows)
        List<StockSummary> activeRows = stockRows.stream()
                .filter(s -> s.getQuantityInHand() != null && s.getQuantityInHand() > 0.001)
                .collect(Collectors.toList());

        if (activeRows.isEmpty()) {
            log.info("No active stock rows for tenant: {}", tenantSchema);
            agingReportRepo.markDeletedForTenant(tenantSchema, Collections.singletonList(-1L));
            return;
        }

        List<Long> activeProductIds = activeRows.stream()
                .map(StockSummary::getProductId)
                .distinct()
                .collect(Collectors.toList());

        // ── Step 2: Get last inward date per (productId, warehouseId) ────────
        // Native query — must run in TENANT schema (inward tables are per-tenant)
        ThreadLocalStorage.setTenantName(tenantSchema);
        @SuppressWarnings("unchecked")
        List<Object[]> inwardDates = em.createNativeQuery(
                "SELECT ioe.productId, ioe.warehouse_id, MAX(ii.date) " +
                "FROM inward_inventory ii " +
                "JOIN inwardinventory_entry jt ON jt.inwardid = ii.inwardId " +
                "JOIN inward_outward_entries ioe ON ioe.entryid = jt.entryId " +
                "WHERE ii.is_deleted = false " +
                "GROUP BY ioe.productId, ioe.warehouse_id"
        ).getResultList();

        // Map: productId → (warehouseId → lastInwardDate)
        Map<Long, Map<Long, Date>> inwardDateMap = new HashMap<>();
        for (Object[] row : inwardDates) {
            Long productId  = ((Number) row[0]).longValue();
            Long warehouseId = ((Number) row[1]).longValue();
            Date lastDate   = (Date) row[2];
            inwardDateMap.computeIfAbsent(productId, k -> new HashMap<>())
                         .put(warehouseId, lastDate);
        }

        // ── Step 3: Get last PO rate per productId from master schema ────────
        ThreadLocalStorage.setTenantName(masterSchema);

        // Native query on master schema — last PO per product
        @SuppressWarnings("unchecked")
        List<Object[]> poRates = em.createNativeQuery(
                "SELECT pol.product_id, pol.rate, pol.discountPercent, pol.gstPercent, po.po_date " +
                "FROM purchase_order_line pol " +
                "JOIN purchase_order po ON po.purchase_order_id = pol.po_id " +
                "WHERE po.is_deleted = false " +
                "  AND pol.product_id IN (:productIds) " +
                "  AND po.po_date = (" +
                "      SELECT MAX(po2.po_date) FROM purchase_order po2 " +
                "      JOIN purchase_order_line pol2 ON pol2.po_id = po2.purchase_order_id " +
                "      WHERE pol2.product_id = pol.product_id AND po2.is_deleted = false" +
                "  ) " +
                "GROUP BY pol.product_id, pol.rate, pol.discountPercent, pol.gstPercent, po.po_date"
        ).setParameter("productIds", activeProductIds)
         .getResultList();

        // Map: productId → {effectiveRate, poDate}
        Map<Long, double[]> poRateMap = new HashMap<>();  // double[]{effectiveRate, poDateMs}
        for (Object[] row : poRates) {
            Long productId      = ((Number) row[0]).longValue();
            double rate         = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            double discountPct  = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double gstPct       = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            Date poDate         = (Date) row[4];
            double effectiveRate = rate - (rate * discountPct / 100.0) + (rate * gstPct / 100.0);
            // Keep the row with the latest po_date (in case of multiple lines for same product on same date)
            if (!poRateMap.containsKey(productId)) {
                poRateMap.put(productId, new double[]{
                        effectiveRate,
                        poDate != null ? (double) poDate.getTime() : 0.0
                });
            }
        }

        // ── Step 4: Load product metadata from master ─────────────────────
        Map<Long, Product> productMap = productRepo.findAllById(activeProductIds)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        // ── Step 5: Group stock rows by productId, build rolled-up + detail ─
        Date now     = new Date();
        Date syncedAt = now;
        long todayMs  = truncateToDay(now).getTime();

        // Group active rows by productId
        Map<Long, List<StockSummary>> byProduct = activeRows.stream()
                .collect(Collectors.groupingBy(StockSummary::getProductId));

        ThreadLocalStorage.setTenantName(masterSchema);

        int inserted = 0, updated = 0;
        for (Map.Entry<Long, List<StockSummary>> entry : byProduct.entrySet()) {
            Long productId = entry.getKey();
            List<StockSummary> rows = entry.getValue();

            Product product = productMap.get(productId);

            // Roll-up totals
            double totalQty = rows.stream().mapToDouble(StockSummary::getQuantityInHand).sum();

            // Per-warehouse aging
            Map<Long, Date> warehouseInwardDates = inwardDateMap.getOrDefault(productId, Collections.emptyMap());

            // Find the most recent inward date across all warehouses (minimum aging = most recent stock)
            Date latestInwardDate = null;
            for (StockSummary s : rows) {
                Date whDate = warehouseInwardDates.get(s.getWarehouseId());
                if (whDate != null && (latestInwardDate == null || whDate.after(latestInwardDate))) {
                    latestInwardDate = whDate;
                }
            }

            int minAgingDays = latestInwardDate != null
                    ? (int) ((todayMs - truncateToDay(latestInwardDate).getTime()) / 86_400_000L)
                    : 9999;

            String agingBucket = computeBucket(minAgingDays);

            // POG
            double[] poInfo = poRateMap.get(productId);
            Double pog     = poInfo != null ? poInfo[0] * totalQty : null;
            Double poRate  = poInfo != null ? poInfo[0] : null;
            Date poDate    = poInfo != null && poInfo[1] > 0 ? new Date((long) poInfo[1]) : null;

            // ── Upsert master row ──
            Optional<GlobalStockAgingReport> existing = agingReportRepo
                    .findByTenantSchemaAndProductId(tenantSchema, productId);

            GlobalStockAgingReport report = existing.orElseGet(GlobalStockAgingReport::new);
            boolean isNew = !existing.isPresent();

            report.setTenantSchema(tenantSchema);
            report.setProductId(productId);
            report.setProductName(product != null ? product.getProductName() : "Unknown");
            report.setProductCode(product != null ? product.getProductCode() : null);
            report.setUnit(product != null ? product.getMeasurementUnit() : null);
            report.setCategory(product != null && product.getCategory() != null
                    ? product.getCategory().getCategoryName() : null);
            report.setTotalQtyInHand(totalQty);
            report.setMinAgingDays(minAgingDays);
            report.setLastInwardDate(latestInwardDate);
            report.setAgingBucket(agingBucket);
            report.setPog(pog);
            report.setLastPoRate(poRate);
            report.setLastPoDate(poDate);
            report.setDeleted(false);
            report.setSyncedAt(syncedAt);

            agingReportRepo.save(report);
            if (isNew) inserted++; else updated++;

            // ── Upsert detail rows (per warehouse) ──
            agingDetailRepo.deleteByTenantSchemaAndProductId(tenantSchema, productId);
            for (StockSummary s : rows) {
                if (s.getQuantityInHand() == null || s.getQuantityInHand() <= 0.001) continue;
                Date whDate = warehouseInwardDates.get(s.getWarehouseId());
                int whAging = whDate != null
                        ? (int) ((todayMs - truncateToDay(whDate).getTime()) / 86_400_000L)
                        : 9999;

                GlobalStockAgingDetail detail = new GlobalStockAgingDetail();
                detail.setTenantSchema(tenantSchema);
                detail.setProductId(productId);
                detail.setWarehouseId(s.getWarehouseId());
                detail.setWarehouseName(s.getWarehouseName());
                detail.setQtyInHand(s.getQuantityInHand());
                detail.setAgingDays(whAging);
                detail.setLastInwardDate(whDate);
                detail.setAgingBucket(computeBucket(whAging));
                agingDetailRepo.save(detail);
            }
        }

        // ── Step 6: Soft-delete master rows for products no longer active ────
        agingReportRepo.markDeletedForTenant(tenantSchema, activeProductIds);

        log.info("Stock aging sync done for {}. Inserted={}, Updated={}", tenantSchema, inserted, updated);
    }

    private static Date truncateToDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static String computeBucket(int days) {
        if (days <= 30)  return "0-30";
        if (days <= 60)  return "31-60";
        if (days <= 90)  return "61-90";
        return "90+";
    }
}
