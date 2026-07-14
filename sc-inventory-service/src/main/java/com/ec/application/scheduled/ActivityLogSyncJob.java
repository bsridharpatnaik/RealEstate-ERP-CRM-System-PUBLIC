package com.ec.application.scheduled;

import com.ec.application.service.ActivityLogGlobalSyncOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogSyncJob {

    private final ActivityLogGlobalSyncOrchestrator orchestrator;

    private static final Logger log = LoggerFactory.getLogger(ActivityLogSyncJob.class);

    @Scheduled(cron = "0 */15 * * * *") // every 15 minutes
    public void run() {
        log.info("Activity log global sync job started");
        orchestrator.syncAllTenants();
        log.info("Activity log global sync job completed");
    }
}
