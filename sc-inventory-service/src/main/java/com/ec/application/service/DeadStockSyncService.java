package com.ec.application.service;

import com.ec.application.constants.ProjectConstants;
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
public class DeadStockSyncService {

    private static final Logger log = LoggerFactory.getLogger(DeadStockSyncService.class);

    private final SchemaConfig schemaConfig;
    private final StockRepo stockRepo;
    private final GlobalDeadStockReportRepository deadStockRepo;
    private final InventoryBatchRepository inventoryBatchRepository;

    @PersistenceContext
    private EntityManager em;

    // No @Transactional — routing DataSource picks schema per-query via ThreadLocalStorage.
    // A wrapping transaction would bind to masterschema at start, before setTenantName runs.
    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        log.info("Starting dead stock sync for tenant: {}", tenantSchema);

        try {
            // Step 1: Load dead stock directly from tenant schema Stock table (avoids stock_summary staleness)
            ThreadLocalStorage.setTenantName(tenantSchema);
            List<Stock> deadStockItems = stockRepo.findDeadStocks().stream()
                    .filter(s -> s.getQuantityInHand() != null && s.getQuantityInHand() > 0)
                    .collect(Collectors.toList());

            // Step 2: Delete existing rows for this tenant in master
            ThreadLocalStorage.setTenantName(masterSchema);
            deadStockRepo.deleteByTenantSchema(tenantSchema);

            if (deadStockItems.isEmpty()) {
                log.info("No dead stock for tenant: {}. Cleared existing rows.", tenantSchema);
                return;
            }

            List<Long> productIds = deadStockItems.stream()
                    .map(s -> s.getProduct().getProductId())
                    .collect(Collectors.toList());

            // Step 3: Load last PO rates from master schema
            ThreadLocalStorage.setTenantName(masterSchema);
            Map<Long, Double> poRateMap = fetchLastPoRates(productIds);

            // Step 4: Load batch data from tenant schema
            ThreadLocalStorage.setTenantName(tenantSchema);
            List<InventoryBatch> deadStockBatches = inventoryBatchRepository
                    .findActiveByWarehouseName(ProjectConstants.deadStockWarehouseName);

            Map<Long, List<InventoryBatch>> batchesByProduct = deadStockBatches.stream()
                    .filter(b -> b.getProduct() != null)
                    .collect(Collectors.groupingBy(b -> b.getProduct().getProductId()));

            // Step 5: Build report rows — product metadata comes directly from Stock entity
            List<GlobalDeadStockReport> reportRows = new ArrayList<>();
            Date now = new Date();

            for (Stock stockRow : deadStockItems) {
                Product product = stockRow.getProduct();
                Long productId = product.getProductId();
                String productName = product.getProductName();
                String productCode = product.getProductCode();
                String unit = product.getMeasurementUnit();
                String category = product.getCategory() != null ? product.getCategory().getCategoryName() : null;
                Double totalQty = stockRow.getQuantityInHand();
                Double lastPoRate = poRateMap.get(productId);

                List<InventoryBatch> batches = batchesByProduct.get(productId);

                if (batches == null || batches.isEmpty()) {
                    reportRows.add(buildRow(tenantSchema, productId, productName, productCode,
                            unit, category, totalQty, lastPoRate, null, now));
                } else {
                    for (InventoryBatch batch : batches) {
                        reportRows.add(buildRow(tenantSchema, productId, productName, productCode,
                                unit, category, totalQty, lastPoRate, batch, now));
                    }
                }
            }

            ThreadLocalStorage.setTenantName(masterSchema);
            deadStockRepo.saveAll(reportRows);
            log.info("Dead stock sync complete for tenant: {}. Saved {} rows.", tenantSchema, reportRows.size());

        } finally {
            ThreadLocalStorage.setTenantName(masterSchema);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Double> fetchLastPoRates(List<Long> productIds) {
        // Master schema — last PO effective rate per product
        List<Object[]> rows = em.createNativeQuery(
                "SELECT pol.product_id, pol.rate, pol.discountPercent, pol.gstPercent " +
                "FROM purchase_order_line pol " +
                "JOIN purchase_order po ON po.purchase_order_id = pol.po_id " +
                "WHERE po.is_deleted = false " +
                "  AND pol.product_id IN (:productIds) " +
                "  AND po.po_date = (" +
                "      SELECT MAX(po2.po_date) FROM purchase_order po2 " +
                "      JOIN purchase_order_line pol2 ON pol2.po_id = po2.purchase_order_id " +
                "      WHERE pol2.product_id = pol.product_id AND po2.is_deleted = false" +
                "  ) " +
                "GROUP BY pol.product_id, pol.rate, pol.discountPercent, pol.gstPercent"
        ).setParameter("productIds", productIds).getResultList();

        Map<Long, Double> rateMap = new HashMap<>();
        for (Object[] row : rows) {
            Long productId    = ((Number) row[0]).longValue();
            double rate       = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            double discount   = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double gst        = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double effective  = rate - (rate * discount / 100.0) + (rate * gst / 100.0);
            rateMap.putIfAbsent(productId, effective);
        }
        return rateMap;
    }

    private GlobalDeadStockReport buildRow(String tenantSchema, Long productId, String productName,
                                            String productCode, String unit, String category,
                                            Double totalQty, Double lastPoRate, InventoryBatch batch, Date now) {
        GlobalDeadStockReport row = new GlobalDeadStockReport();
        row.setTenantSchema(tenantSchema);
        row.setProductId(productId);
        row.setProductName(productName);
        row.setProductCode(productCode);
        row.setUnit(unit);
        row.setCategory(category);
        row.setQty(totalQty);
        row.setLastPoRate(lastPoRate);
        row.setSyncedAt(now);

        if (batch != null) {
            row.setBatchId(batch.getBatchId());
            row.setBatchLotNumber(batch.getLotNumber());
            row.setBatchBrand(batch.getBrand());
            row.setBatchReceivedDate(batch.getReceivedDate());
            row.setBatchExpiryDate(batch.getExpiryDate());
            row.setBatchQtyRemaining(batch.getQtyRemaining());
        }
        return row;
    }
}
