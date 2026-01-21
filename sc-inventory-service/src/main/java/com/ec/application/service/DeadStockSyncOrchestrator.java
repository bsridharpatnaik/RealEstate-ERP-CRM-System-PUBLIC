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
import com.ec.application.service.DeadStockSyncService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class DeadStockSyncOrchestrator {

    private static final String JOB_NAME = "DEAD_STOCK_SYNC";

    private final DeadStockSyncService tenantSyncService;
    private final SchemaConfig schemaConfig;
    private final JobExecutionLogRepository jobRepo;

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    private static final Logger log =
            LoggerFactory.getLogger(DeadStockSyncOrchestrator.class);

    @UseDefaultTenant  // IMPORTANT: master schema
    public String syncAllTenants() {

        if (!syncRunning.compareAndSet(false, true)) {
            return "Dead stock sync is already running. Please refresh page in few minutes.";
        }

        JobExecutionLog job =
                jobRepo.findByJobName(JOB_NAME)
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
            for (String tenant : schemaConfig.getNonMasterSchemaList()) {
                try {
                    tenantSyncService.syncSingleTenant(tenant);
                } catch (Exception e) {
                    log.error("DeadStock sync failed for tenant {}", tenant, e);
                }
            }
            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
            job.setLastStatus("SUCCESS");
            job.setLastEndTime(new Date());
            job.setUpdatedAt(new Date());
            jobRepo.save(job);

        } catch (Exception e) {

            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
            job.setLastStatus("FAILED");
            job.setLastEndTime(new Date());
            job.setLastError(e.getMessage());
            job.setUpdatedAt(new Date());
            jobRepo.save(job);

            throw e;

        } finally {
            syncRunning.set(false);
        }

        return "Dead stock sync triggered. Please refresh page in few minutes.";
    }
}
