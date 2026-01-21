package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.ec.application.service.DeadStockSyncService;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class DeadStockSyncOrchestrator {

    private final DeadStockSyncService tenantSyncService;
    private final SchemaConfig schemaConfig;

    private static final Logger log =
            LoggerFactory.getLogger(DeadStockSyncOrchestrator.class);

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    public String syncAllTenants() {

        // Prevent parallel runs
        if (!syncRunning.compareAndSet(false, true)) {
            return "Dead stock sync is already running. Please refresh page in few minutes.";
        }

        log.info("Dead stock sync started");

        try {
            for (String tenant : schemaConfig.getNonMasterSchemaList()) {
                try {
                    tenantSyncService.syncSingleTenant(tenant);
                } catch (Exception e) {
                    log.error("DeadStock sync failed for tenant {}", tenant, e);
                }
            }
        } finally {
            syncRunning.set(false);
            log.info("Dead stock sync completed");
        }

        return "Dead stock sync triggered. Please refresh page in few minutes.";
    }
}
