package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.DeadStockDTO;
import com.ec.application.model.DeadStockSummary;
import com.ec.application.model.Product;
import com.ec.application.model.Stock;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.DeadStockSummaryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeadStockSyncService {

    private final DeadStockSummaryRepo deadStockSummaryRepo;
    private final StockRepo stockRepo;
    private final SchemaConfig schemaConfig;
    private final TenantService tenantService;

    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        String tenantWithPrefix = tenantService.changeTenantForSuncity(tenantSchema);
        System.out.println("▶️ Starting DeadStock sync for tenant: " + tenantSchema);

        try {
            // ---------- Step 1: Get last sync time from MASTER ----------
            ThreadLocalStorage.setTenantName(masterSchema);
            Date lastSyncTime = deadStockSummaryRepo.findLastSyncTime();
            if (lastSyncTime == null) {
                lastSyncTime = new Date(0); // 01-01-1970 00:00:00
                System.out.println("⏱ Last sync time not found. Using minimum date: " + lastSyncTime);
            } else {
                System.out.println("⏱ Last sync time: " + lastSyncTime);
            }

            // ---------- Step 2: Fetch dead stocks from TENANT ----------
            ThreadLocalStorage.setTenantName(tenantWithPrefix);
            List<Stock> stocks = stockRepo.findDeadStocksUpdatedAfter(lastSyncTime);

            System.out.println("📦 Dead stock records fetched: " + stocks.size());

            if (stocks.isEmpty()) {
                System.out.println("No dead stock changes for tenant: " + tenantSchema);
                return;
            }

            // ---------- Step 3: Map to DTO ----------
            List<DeadStockDTO> deadStocks = mapStockToDeadStockDTO(stocks, tenantSchema);

            // ---------- Step 4: Persist into MASTER ----------
            ThreadLocalStorage.setTenantName(masterSchema);

            Date now = new Date();
            int created = 0;
            int updated = 0;

            for (DeadStockDTO deadStock : deadStocks) {
                boolean isNew = findOrCreateAndUpdateDeadStockSummary(deadStock, now);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            }

            System.out.println("DeadStock sync completed for tenant: " + tenantSchema + " | Created: " + created + " | Updated: " + updated);

        } catch (Exception e) {
            System.out.println("DeadStock sync FAILED for tenant: " + tenantSchema);
            e.printStackTrace();
            throw e;
        }
    }

    // ------------------------------------------------------------
    // MASTER schema operation
    // ------------------------------------------------------------
    @UseDefaultTenant
    @Transactional
    private boolean findOrCreateAndUpdateDeadStockSummary(DeadStockDTO deadStock, Date syncedAt) {
        List<DeadStockSummary> existing = deadStockSummaryRepo.findByTenantSchemaAndProductIdAndWarehouseId(deadStock.getTenantSchema(), deadStock.getProductId(), deadStock.getWarehouseId());

        // ---------- CREATE ----------
        if (existing == null || existing.isEmpty()) {
            DeadStockSummary newSummary = buildDeadStockSummary(deadStock, syncedAt);
            deadStockSummaryRepo.save(newSummary);
            System.out.println("➕ Created DeadStockSummary | Product: " + deadStock.getProductId() + " | Warehouse: " + deadStock.getWarehouseId());
            return true;
        }

        // ---------- UPDATE ----------
        DeadStockSummary summary = existing.get(0);
        summary.setQuantityInHand(deadStock.getQuantityInHand());
        summary.setSyncedAt(syncedAt);
        deadStockSummaryRepo.save(summary);
        System.out.println("✏️ Updated DeadStockSummary | Product: " + deadStock.getProductId() + " | Warehouse: " + deadStock.getWarehouseId());
        return false;
    }

    private DeadStockSummary buildDeadStockSummary(DeadStockDTO deadStock, Date syncedAt) {
        DeadStockSummary summary = new DeadStockSummary();
        summary.setTenantSchema(deadStock.getTenantSchema());
        summary.setProductId(deadStock.getProductId());
        summary.setProductName(deadStock.getProductName());
        summary.setProductCode(deadStock.getProductCode());
        summary.setWarehouseId(deadStock.getWarehouseId());
        summary.setWarehouseName(deadStock.getWarehouseName());
        summary.setQuantityInHand(deadStock.getQuantityInHand());
        summary.setSyncedAt(syncedAt);
        return summary;
    }

    // ------------------------------------------------------------
    // Tenant-side mapping
    // ------------------------------------------------------------
    private List<DeadStockDTO> mapStockToDeadStockDTO(List<Stock> stocks, String tenantSchema) {
        List<DeadStockDTO> result = new ArrayList<>(stocks.size());
        for (Stock stock : stocks) {
            if (stock == null || stock.getProduct() == null || stock.getWarehouse() == null) {
                continue;
            }
            DeadStockDTO dto = new DeadStockDTO();
            dto.setTenantSchema(tenantSchema);
            dto.setTenantCode(tenantSchema);
            dto.setProductId(stock.getProduct().getProductId());
            dto.setProductCode(stock.getProduct().getProductCode());
            dto.setProductName(stock.getProduct().getProductName());
            dto.setWarehouseId(stock.getWarehouse().getWarehouseId());
            dto.setWarehouseName(stock.getWarehouse().getWarehouseName());
            dto.setQuantityInHand(stock.getQuantityInHand());
            result.add(dto);
        }
        return result;
    }
}