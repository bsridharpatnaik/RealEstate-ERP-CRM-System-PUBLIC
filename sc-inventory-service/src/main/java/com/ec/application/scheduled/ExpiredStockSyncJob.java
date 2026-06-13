package com.ec.application.scheduled;

import com.ec.application.service.ExpiredStockOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpiredStockSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredStockSyncJob.class);

    private final ExpiredStockOrchestrator orchestrator;

    /** Run once daily at 02:00 AM — expiry status doesn't change during the day. */
    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        log.info("Expired stock sync job started");
        orchestrator.syncAllTenants();
        log.info("Expired stock sync job completed");
    }
}
