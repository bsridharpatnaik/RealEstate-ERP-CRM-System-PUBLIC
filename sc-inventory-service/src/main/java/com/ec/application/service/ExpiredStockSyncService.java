package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.GlobalExpiredStockReport;
import com.ec.application.model.InventoryBatch;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.GlobalExpiredStockReportRepository;
import com.ec.application.repository.InventoryBatchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpiredStockSyncService {

    private static final Logger log = LoggerFactory.getLogger(ExpiredStockSyncService.class);

    private final SchemaConfig schemaConfig;
    private final GlobalExpiredStockReportRepository repo;
    private final InventoryBatchRepository inventoryBatchRepository;

    // No @Transactional — the routing DataSource picks the schema per-query via ThreadLocalStorage.
    // Wrapping in a single transaction would bind the connection to masterschema at transaction start
    // (before setTenantName), causing all tenant queries to hit master and return empty.
    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        log.info("Starting expired stock sync for tenant: {}", tenantSchema);

        try {
            // Step 1: Load batches from tenant schema — build rows immediately while tenant is still set
            // so that lazy-loaded Product and Warehouse relationships resolve against the correct schema.
            ThreadLocalStorage.setTenantName(tenantSchema);
            List<InventoryBatch> batches = inventoryBatchRepository.findAllWithExpiryAndStock();

            Date today = truncateToDay(new Date());
            Date syncedAt = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            List<GlobalExpiredStockReport> rows = new ArrayList<>();

            for (InventoryBatch b : batches) {
                // Access lazy relationships HERE — tenant schema still active
                if (b.getProduct() == null || b.getWarehouse() == null) continue;

                Date expiry = b.getExpiryDate();
                Date expiryDay = truncateToDay(expiry);
                long diffMs = expiryDay.getTime() - today.getTime();
                int daysUntilExpiry = (int) (diffMs / 86_400_000L);

                GlobalExpiredStockReport row = new GlobalExpiredStockReport();
                row.setTenantSchema(tenantSchema);
                row.setProductId(b.getProduct().getProductId());
                row.setProductName(b.getProduct().getProductName());
                row.setProductCode(b.getProduct().getProductCode());
                row.setUnit(b.getProduct().getMeasurementUnit());
                row.setCategory(b.getProduct().getCategory() != null
                        ? b.getProduct().getCategory().getCategoryName() : null);
                row.setWarehouseId(b.getWarehouse().getWarehouseId());
                row.setWarehouseName(b.getWarehouse().getWarehouseName());
                row.setBatchId(b.getBatchId());
                row.setLotNumber(b.getLotNumber());
                row.setBrand(b.getBrand());
                row.setExpiryDate(sdf.format(expiry));
                row.setReceivedDate(b.getReceivedDate() != null ? sdf.format(b.getReceivedDate()) : null);
                row.setQtyRemaining(b.getQtyRemaining());
                row.setDaysUntilExpiry(daysUntilExpiry);
                row.setSyncedAt(syncedAt);
                rows.add(row);
            }

            // Step 2: Switch to master — delete old rows, save new ones
            ThreadLocalStorage.setTenantName(masterSchema);
            repo.deleteByTenantSchema(tenantSchema);

            if (rows.isEmpty()) {
                log.info("No expiry-tracked batches for tenant: {}.", tenantSchema);
                return;
            }

            repo.saveAll(rows);
            log.info("Expired stock sync complete for tenant: {}. Saved {} rows.", tenantSchema, rows.size());

        } finally {
            ThreadLocalStorage.setTenantName(masterSchema);
        }
    }

    private Date truncateToDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
