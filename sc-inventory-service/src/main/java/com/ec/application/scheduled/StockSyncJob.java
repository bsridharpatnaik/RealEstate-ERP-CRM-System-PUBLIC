package com.ec.application.scheduled;

import com.ec.application.service.StockSyncOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockSyncJob {

    private final StockSyncOrchestrator deadStockSyncService;

    Logger log = LoggerFactory.getLogger(StockSyncJob.class);

    @Scheduled(cron = "0 15 * * * *") // every hour at :15
    public void run() {
        log.info("DeadStock global sync job started");
        deadStockSyncService.syncAllTenants();
        log.info("DeadStock global sync job completed");
    }
}