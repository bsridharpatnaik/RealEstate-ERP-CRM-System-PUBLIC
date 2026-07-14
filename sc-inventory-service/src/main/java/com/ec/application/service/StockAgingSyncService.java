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
    private final GlobalStockAgingChunkRepository agingChunkRepo;

    /** One FIFO chunk of stock still present: qty, inward date, and its age in days. */
    private static class AgeChunk {
        final double quantity;
        final Date inwardDate;
        final int ageDays;
        AgeChunk(double quantity, Date inwardDate, int ageDays) {
            this.quantity = quantity;
            this.inwardDate = inwardDate;
            this.ageDays = ageDays;
        }
    }

    /** Raw inward row used to walk FIFO order: oldest first. */
    private static class InwardRow {
        final Date date;
        final double quantity;
        InwardRow(Date date, double quantity) {
            this.date = date;
            this.quantity = quantity;
        }
    }

    /**
     * Walks inward rows oldest→newest, consuming from {@code qtyInHand} until exhausted.
     * Mirrors StockService.calculateStockAges() — assumes FIFO: the qty currently in hand is
     * made up of the oldest available inward quantities first.
     */
    private static List<AgeChunk> walkFifoChunks(List<InwardRow> inwardRows, double qtyInHand, long todayMs) {
        List<AgeChunk> chunks = new ArrayList<>();
        double remaining = qtyInHand;
        for (InwardRow row : inwardRows) {
            if (remaining <= 0) break;
            double take = Math.min(remaining, row.quantity);
            int ageDays = (int) ((todayMs - truncateToDay(row.date).getTime()) / 86_400_000L);
            chunks.add(new AgeChunk(take, row.date, ageDays));
            remaining -= take;
        }
        return chunks;
    }

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

        // ── Step 2: Get ALL stock-increase rows per (productId, warehouseId), oldest first ──
        // Native query — must run in TENANT schema. Used to FIFO-walk "oldest stock still
        // present" rather than just taking the most recent inward date. Must include every
        // transaction type that adds to stock — matches all_inventory_view's closingstock
        // CASE (Inward, Transfer-In, Excess-Found); Transfer-Out/Outward/Lost-Damaged/Write-Off
        // are deductions and are intentionally excluded.
        ThreadLocalStorage.setTenantName(tenantSchema);
        @SuppressWarnings("unchecked")
        List<Object[]> inwardRowsRaw = em.createNativeQuery(
                "SELECT productid, warehouse_id, date, quantity " +
                "FROM all_inventory " +
                "WHERE type IN ('Inward', 'Transfer-In', 'Excess-Found') AND productid IN (:productIds) " +
                "ORDER BY productid, warehouse_id, id ASC"
        ).setParameter("productIds", activeProductIds)
         .getResultList();

        // Map: productId → (warehouseId → ordered list of inward rows, oldest first)
        Map<Long, Map<Long, List<InwardRow>>> inwardRowMap = new HashMap<>();
        for (Object[] row : inwardRowsRaw) {
            if (row[0] == null || row[1] == null || row[2] == null) continue;
            Long productId   = ((Number) row[0]).longValue();
            Long warehouseId = ((Number) row[1]).longValue();
            Date date        = (Date) row[2];
            double quantity  = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            inwardRowMap.computeIfAbsent(productId, k -> new HashMap<>())
                        .computeIfAbsent(warehouseId, k -> new ArrayList<>())
                        .add(new InwardRow(date, quantity));
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

        // Wipe and fully rebuild detail/chunk rows for this tenant ONCE, rather than per
        // product — each save()/delete() against the (unpooled) master-schema DataSource opens
        // its own raw DB connection, so per-row calls in a loop can pile up thousands of
        // connections across a multi-tenant sync run and exhaust local ephemeral ports.
        agingDetailRepo.deleteByTenantSchema(tenantSchema);
        agingChunkRepo.deleteByTenantSchema(tenantSchema);

        List<GlobalStockAgingReport> reportsToSave = new ArrayList<>();
        List<GlobalStockAgingDetail> detailsToSave = new ArrayList<>();
        List<GlobalStockAgingChunk> chunksToSave = new ArrayList<>();

        int inserted = 0, updated = 0;
        for (Map.Entry<Long, List<StockSummary>> entry : byProduct.entrySet()) {
            Long productId = entry.getKey();
            List<StockSummary> rows = entry.getValue();

            Product product = productMap.get(productId);

            // Roll-up totals
            double totalQty = rows.stream().mapToDouble(StockSummary::getQuantityInHand).sum();

            // Per-warehouse FIFO walk: which inward chunks make up the qty currently in hand,
            // oldest first. Build this once per warehouse, then reuse for both the header
            // roll-up (oldest chunk overall) and the per-warehouse detail/breakdown rows.
            Map<Long, List<InwardRow>> warehouseInwardRows = inwardRowMap.getOrDefault(productId, Collections.emptyMap());
            Map<Long, List<AgeChunk>> warehouseChunks = new HashMap<>();
            for (StockSummary s : rows) {
                if (s.getQuantityInHand() == null || s.getQuantityInHand() <= 0.001) continue;
                List<InwardRow> whRows = warehouseInwardRows.getOrDefault(s.getWarehouseId(), Collections.emptyList());
                warehouseChunks.put(s.getWarehouseId(), walkFifoChunks(whRows, s.getQuantityInHand(), todayMs));
            }

            // Headline aging = age of the OLDEST chunk still present across all warehouses
            // (worst case / dead-stock risk), not the most recent inward. Each warehouse's
            // chunks[0] is already its oldest surviving stock (walkFifoChunks walks oldest→newest).
            Date oldestInwardDate = null;
            int minAgingDays = 9999;
            boolean foundAnyChunk = false;
            for (List<AgeChunk> chunks : warehouseChunks.values()) {
                if (chunks.isEmpty()) continue;
                AgeChunk oldest = chunks.get(0);
                if (!foundAnyChunk || oldest.ageDays > minAgingDays) {
                    minAgingDays = oldest.ageDays;
                    oldestInwardDate = oldest.inwardDate;
                    foundAnyChunk = true;
                }
            }
            Date latestInwardDate = oldestInwardDate; // field name kept for minimal diff; now holds the OLDEST traceable inward date

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

            reportsToSave.add(report);
            if (isNew) inserted++; else updated++;

            // ── Build detail rows (per warehouse) — collected, saved in bulk after the loop ──
            for (StockSummary s : rows) {
                if (s.getQuantityInHand() == null || s.getQuantityInHand() <= 0.001) continue;
                List<AgeChunk> chunks = warehouseChunks.getOrDefault(s.getWarehouseId(), Collections.emptyList());

                // chunks[0] is the oldest surviving stock (walkFifoChunks walks oldest→newest),
                // so it drives this warehouse's headline age.
                AgeChunk oldestChunk = chunks.isEmpty() ? null : chunks.get(0);
                int whAging = oldestChunk != null ? oldestChunk.ageDays : 9999;
                Date whDate = oldestChunk != null ? oldestChunk.inwardDate : null;

                GlobalStockAgingDetail detail = new GlobalStockAgingDetail();
                detail.setTenantSchema(tenantSchema);
                detail.setProductId(productId);
                detail.setWarehouseId(s.getWarehouseId());
                detail.setWarehouseName(s.getWarehouseName());
                detail.setQtyInHand(s.getQuantityInHand());
                detail.setAgingDays(whAging);
                detail.setLastInwardDate(whDate);
                detail.setAgingBucket(computeBucket(whAging));
                detailsToSave.add(detail);

                // Persist the FIFO chunk breakdown for the expand/drill-down UI
                int sortOrder = 0;
                for (AgeChunk c : chunks) {
                    GlobalStockAgingChunk chunk = new GlobalStockAgingChunk();
                    chunk.setTenantSchema(tenantSchema);
                    chunk.setProductId(productId);
                    chunk.setWarehouseId(s.getWarehouseId());
                    chunk.setQuantity(c.quantity);
                    chunk.setInwardDate(c.inwardDate);
                    chunk.setAgeDays(c.ageDays);
                    chunk.setSortOrder(sortOrder++);
                    chunksToSave.add(chunk);
                }
            }
        }

        // ── Bulk-save everything for this tenant in three round trips instead of thousands ──
        agingReportRepo.saveAll(reportsToSave);
        agingDetailRepo.saveAll(detailsToSave);
        agingChunkRepo.saveAll(chunksToSave);

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
