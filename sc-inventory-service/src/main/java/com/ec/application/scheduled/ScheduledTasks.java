package com.ec.application.scheduled;

import com.ec.application.ReusableClasses.EmailHelper;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.JobFailureAlertDTO;
import com.ec.application.data.ProjectStockEmailData;
import com.ec.application.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ec.application.data.ExpiryAlertRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
public class ScheduledTasks {
    @Autowired
    StockService stockService;

    @Autowired
    private SchemaConfig schemaConfig;

    Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    SMSService smsService;

    @Autowired
    AsyncServiceInventory asyncServiceInventory;

    @Autowired
    private AsyncService asyncService;

    @Autowired
    AllInventoryService allInventoryService;

    @Autowired
    StockSyncJob stockSyncJob;

    @Autowired
    StockBalanceValidationService stockBalanceValidationService;

    @Autowired
    StockEmailReportService stockEmailReportService;

    @Autowired
    EmailHelper emailHelper;

    @Autowired
    Environment environment;

    @Autowired
    com.ec.application.service.BatchTrackingService batchTrackingService;

    @Autowired
    ProjectConstantsService projectConstantsService;

 /*   //@Scheduled(cron = "0 0 9,18 * * *")
    public void sendStockNotificationEmailInEvening() throws Exception {
        log.info("Sending Stock Notification Email in evening");
        List<String> tenants = schemaConfig.getSchemaList();
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            stockService.sendStockNotificationEmail();
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
        }
    }

    //@Scheduled(cron = "0 0 18 * * ?")
    public void sendStockValidationEmail() throws Exception {
        log.info("Sending Stock Notification Email in evening");
        List<String> tenants = schemaConfig.getSchemaList();
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            stockService.sendStockValidationEmail();
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
        }
    }

    @Scheduled(cron = "0 0 20 * * MON-SAT")
    public void sendIOStats() throws Exception {
        smsService.sendIOStats();
    }*/

    /** True only when a "prod" profile is active — gates side-effecting jobs so QA/staging stay silent. */
    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.contains("prod"));
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Kolkata")
    public void sendDailyStockEmailReport() {
        // Only the prod instance sends to the real recipient list; QA/staging must stay silent
        // (recipients auto-seed real addresses on every startup — see EmailRecipientService).
        if (!isProdProfile()) {
            log.info("Daily stock email report skipped — non-prod profile");
            return;
        }
        log.info("Daily stock email report triggered");
        try {
            List<String> tenants = schemaConfig.getNonMasterSchemaList();
            Map<Long, Double> netRateMap = stockEmailReportService.fetchLatestNetRateMap();
            List<ProjectStockEmailData> allProjects = new ArrayList<>();

            Calendar cal = Calendar.getInstance();
            Date today = truncateToDay(cal);
            Date day60 = addDays(cal, 60);

            List<ExpiryAlertRow> expiring30 = new ArrayList<>();  // 0–nearExpiryDays
            List<ExpiryAlertRow> expiring60 = new ArrayList<>();  // nearExpiryDays+1 – 60 days

            for (String tenantName : tenants) {
                com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                try {
                    // Near-expiry window is per-tenant config — compute inside the loop
                    int nearDays = projectConstantsService.getNearExpiryDays();
                    Date dayNear     = addDays(cal, nearDays);
                    Date dayNearPlus = addDays(cal, nearDays + 1);
                    allProjects.add(stockEmailReportService.collectTenantStockData(tenantName, netRateMap));
                    expiring30.addAll(stockEmailReportService.collectExpiryRows(tenantName, today, dayNear));
                    if (nearDays < 60)
                        expiring60.addAll(stockEmailReportService.collectExpiryRows(tenantName, dayNearPlus, day60));
                } finally {
                    com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
                }
            }
            if (!allProjects.isEmpty()) {
                byte[] excelBytes = stockEmailReportService.buildExcelBytes(allProjects);
                emailHelper.sendDailyStockReport(allProjects, excelBytes, expiring30, expiring60);
            } else {
                log.info("No tenants found — skipping daily stock report");
            }
        } catch (Exception e) {
            log.error("Daily stock email report failed", e);
        }
    }

    /** Strips time component — start of today. */
    private static Date truncateToDay(Calendar cal) {
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /** Returns a new Date = today + days, time 23:59:59 (end of that day). */
    private static Date addDays(Calendar cal, int days) {
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Kolkata")
    public void processExpiryAlerts() {
        if (!isProdProfile()) {
            log.info("Expiry alert job skipped — non-prod profile");
            return;
        }
        log.info("Expiry alert job triggered");
        List<String> tenants = schemaConfig.getNonMasterSchemaList();
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            try {
                batchTrackingService.processExpiryAlerts();
            } catch (Exception e) {
                log.error("Expiry alert processing failed for tenant: {}", tenantName, e);
            } finally {
                com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kolkata")
    public void runNightlyStockBalanceValidation() {
        if (!isProdProfile()) {
            log.info("Nightly stock balance validation skipped — non-prod profile");
            return;
        }
        log.info("Nightly stock balance validation triggered");
        try {
            stockBalanceValidationService.runNightlyValidation();
        } catch (Exception e) {
            log.error("Nightly stock balance validation failed", e);
        }
    }

    @Scheduled(cron = "0 20 * * * *") // every hour at :20
    public void updateClosingStock() throws Exception {
        List<String> tenants = schemaConfig.getNonMasterSchemaList();
        List<JobFailureAlertDTO> failures = new ArrayList<>();
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            try {
                log.info("Update ClosingStock being triggered for tenant " + tenantName);
                allInventoryService.updateClosingStock();
                allInventoryService.updateAllInventoryTable();
            } catch (Exception e) {
                log.error("Closing stock update failed for tenant: {}", tenantName, e);
                failures.add(new JobFailureAlertDTO(
                    "Closing Stock Update", tenantName, e.getMessage(), null, new Date()
                ));
            } finally {
                com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
            }
        }
        if (!failures.isEmpty()) {
            boolean isProd = Arrays.stream(environment.getActiveProfiles())
                    .anyMatch(p -> p.contains("prod"));
            if (isProd && !tenants.isEmpty()) {
                com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenants.get(0));
                try {
                    emailHelper.sendJobFailureAlert(failures, "Closing Stock Update");
                } finally {
                    com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
                }
            }
        }
    }
}
