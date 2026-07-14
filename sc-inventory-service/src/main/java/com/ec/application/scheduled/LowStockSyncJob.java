package com.ec.application.scheduled;

import com.ec.application.service.LowStockOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LowStockSyncJob {

    private static final Logger log = LoggerFactory.getLogger(LowStockSyncJob.class);

    private final LowStockOrchestrator orchestrator;

    @Scheduled(cron = "0 0/30 * * * *") // every 30 minutes
    public void run() {
        log.info("Low stock sync job started");
        orchestrator.syncAllTenants();
        log.info("Low stock sync job completed");
    }
}
