package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.DeadStockSummary;
import com.ec.application.model.Stock;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.DeadStockSummaryRepo;
import com.ec.application.repository.StockRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeadStockSyncService {

    private final StockRepo stockRepo;
    private final DeadStockSummaryRepo deadStockSummaryRepo;
    private final SchemaConfig schemaConfig; // your tenant map helper
    private final TenantService tenantService;

    Logger log = LoggerFactory.getLogger(DeadStockSyncService.class);

    /**
     * API-triggered sync
     */
    public void syncAllTenants() {
        for (String tenant : schemaConfig.getNonMasterSchemaList()) {
            syncSingleTenant(tenant);
        }
    }

    /**
     * Scheduler-safe method
     */
    public void syncSingleTenant(String tenantSchema) {
        log.info("DeadStock sync started for tenant {}", tenantSchema);

        try {
            /* -------------------------
               Step 1: Read from tenant
               ------------------------- */
            ThreadLocalStorage.setTenantName(tenantSchema);
            List<Stock> deadStocks = stockRepo.findDeadStocks();

            /* -------------------------
               Step 2: Switch to master
               ------------------------- */
            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());

            // simple + safe strategy
            deadStockSummaryRepo.deleteByTenantSchema(tenantSchema);

            List<DeadStockSummary> summaries = deadStocks.stream()
                    .map(stock -> mapToSummary(stock, tenantSchema))
                    .collect(Collectors.toList());
            deadStockSummaryRepo.saveAll(summaries);
            log.info("DeadStock sync completed for tenant {} | rows={}",
                    tenantSchema, summaries.size());

        } catch (Exception e) {
            log.error("DeadStock sync failed for tenant {}", tenantSchema, e);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    private DeadStockSummary mapToSummary(Stock stock, String tenantSchema) {
        DeadStockSummary s = new DeadStockSummary();
        s.setTenantSchema(tenantService.removePrefixForSuncity(tenantSchema));
        s.setProductId(stock.getProduct().getProductId());
        s.setProductName(stock.getProduct().getProductName());
        s.setWarehouseId(stock.getWarehouse().getWarehouseId());
        s.setWarehouseName(stock.getWarehouse().getWarehouseName());
        s.setQuantityInHand(stock.getQuantityInHand());
        s.setSourceLastModifiedDate(stock.getLastModifiedDate());
        s.setSyncedAt(new Date());
        return s;
    }
}
