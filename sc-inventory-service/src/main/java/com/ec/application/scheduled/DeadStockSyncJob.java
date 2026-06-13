package com.ec.application.scheduled;

import com.ec.application.service.DeadStockSyncOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeadStockSyncJob {

    private static final Logger log = LoggerFactory.getLogger(DeadStockSyncJob.class);

    private final DeadStockSyncOrchestrator orchestrator;

    @Scheduled(cron = "0 15/30 * * * *") // every 30 min, offset 15 min from low stock job
    public void run() {
        log.info("Dead stock sync job started");
        orchestrator.syncAllTenants();
        log.info("Dead stock sync job completed");
    }
}
