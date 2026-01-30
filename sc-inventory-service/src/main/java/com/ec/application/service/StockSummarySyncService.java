package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.StockSummaryDTO;
import com.ec.application.model.StockSummary;
import com.ec.application.model.Stock;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.StockSummaryRepo;
import com.ec.application.repository.StockRepo;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSummarySyncService {

    private final StockSummaryRepo stockSummaryRepo;
    private final StockRepo stockRepo;
    private final SchemaConfig schemaConfig;
    private final TenantService tenantService;

    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        System.out.println("▶️ Starting Stock sync for tenant: " + tenantSchema);

        try {
            // ---------- Step 1: Get last sync time from MASTER ----------
            ThreadLocalStorage.setTenantName(masterSchema);
            Date lastSyncTime = stockSummaryRepo.findLastSyncTime();
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

            if (stocks.isEmpty()) {
                System.out.println("No Stock changes for tenant: " + tenantSchema);
                return;
            }

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

        } catch (Exception e) {
            System.out.println("Stock sync FAILED for tenant: " + tenantSchema);
            e.printStackTrace();
            throw e;
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