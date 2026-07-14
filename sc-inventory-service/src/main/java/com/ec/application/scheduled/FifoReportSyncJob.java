package com.ec.application.scheduled;

import com.ec.application.service.FifoReportSyncOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FifoReportSyncJob {

    private static final Logger log = LoggerFactory.getLogger(FifoReportSyncJob.class);

    private final FifoReportSyncOrchestrator orchestrator;

    @Scheduled(cron = "0 5 * * * *") // every hour at :05
    public void run() {
        log.info("FIFO report sync job started");
        orchestrator.syncAllTenants();
        log.info("FIFO report sync job completed");
    }
}
