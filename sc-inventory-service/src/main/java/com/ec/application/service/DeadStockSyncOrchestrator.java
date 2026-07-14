package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.model.JobExecutionLog;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class DeadStockSyncOrchestrator {

    private static final String JOB_NAME = "DEAD_STOCK_SYNC";
    private static final Logger log = LoggerFactory.getLogger(DeadStockSyncOrchestrator.class);

    private final DeadStockSyncService syncService;
    private final SchemaConfig schemaConfig;
    private final JobExecutionLogRepository jobRepo;

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    @UseDefaultTenant
    public String syncAllTenants() {
        if (!syncRunning.compareAndSet(false, true)) {
            return "Dead stock sync is already running. Please try again in a few minutes.";
        }

        JobExecutionLog job = jobRepo.findByJobName(JOB_NAME)
                .orElseGet(() -> {
                    JobExecutionLog j = new JobExecutionLog();
                    j.setJobName(JOB_NAME);
                    return j;
                });

        job.setLastStartTime(new Date());
        job.setLastStatus("RUNNING");
        job.setTriggeredBy("API");
        job.setUpdatedAt(new Date());
        jobRepo.save(job);

        try {
            List<String> failed = new ArrayList<>();
            for (String tenant : schemaConfig.getNonMasterSchemaList()) {
                try {
                    syncService.syncSingleTenant(tenant);
                } catch (Exception e) {
                    log.error("Dead stock sync failed for tenant: {}", tenant, e);
                    failed.add(tenant);
                }
            }

            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
            String status = failed.isEmpty() ? "SUCCESS" : "PARTIAL";
            job.setLastStatus(status);
            job.setLastEndTime(new Date());
            job.setUpdatedAt(new Date());
            if (!failed.isEmpty()) {
                job.setLastError("Failed tenants: " + String.join(", ", failed));
            }
            jobRepo.save(job);

            return failed.isEmpty()
                    ? "Dead stock sync completed successfully."
                    : "Dead stock sync completed with errors. Failed tenants: " + String.join(", ", failed);

        } catch (Exception e) {
            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
            job.setLastStatus("FAILED");
            job.setLastEndTime(new Date());
            job.setLastError(e.getMessage());
            job.setUpdatedAt(new Date());
            jobRepo.save(job);
            log.error("Dead stock sync failed", e);
            return "Dead stock sync failed: " + e.getMessage();
        } finally {
            syncRunning.set(false);
        }
    }
}
