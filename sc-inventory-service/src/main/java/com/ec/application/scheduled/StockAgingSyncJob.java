package com.ec.application.scheduled;

import com.ec.application.service.StockAgingOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockAgingSyncJob {

    private static final Logger log = LoggerFactory.getLogger(StockAgingSyncJob.class);

    private final StockAgingOrchestrator orchestrator;

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void run() {
        log.info("Stock aging sync job started");
        orchestrator.syncAllTenants();
        log.info("Stock aging sync job completed");
    }
}
