package com.ec.application.scheduled;

import com.ec.application.service.DeadStockSyncOrchestrator;
import com.ec.application.service.DeadStockSyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeadStockSyncJob {

    private final DeadStockSyncOrchestrator deadStockSyncService;

    Logger log = LoggerFactory.getLogger(DeadStockSyncJob.class);

    @Scheduled(cron = "0 */30 * * * *") // every 30 mins
    public void run() {
        log.info("DeadStock global sync job started");
        deadStockSyncService.syncAllTenants();
        log.info("DeadStock global sync job completed");
    }
}