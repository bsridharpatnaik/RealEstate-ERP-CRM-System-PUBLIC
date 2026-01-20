package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.ec.application.service.DeadStockSyncService;

@Service
@RequiredArgsConstructor
public class DeadStockSyncOrchestrator {

    private final DeadStockSyncService tenantSyncService;
    private final SchemaConfig schemaConfig;
    Logger log = LoggerFactory.getLogger(DeadStockSyncOrchestrator.class);

    public void syncAllTenants() {

        for (String tenant : schemaConfig.getNonMasterSchemaList()) {
            try {
                tenantSyncService.syncSingleTenant(tenant);
            } catch (Exception e) {
                log.error("DeadStock sync failed for tenant {}", tenant, e);
            }
        }
    }
}
