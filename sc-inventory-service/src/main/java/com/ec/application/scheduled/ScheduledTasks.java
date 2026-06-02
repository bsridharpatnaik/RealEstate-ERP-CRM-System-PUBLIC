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
    PriorityComputeService priorityComputeService;

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

    /** Runs every hour from 9 AM to 6 PM IST. Recomputes PO priority from indent expected dates.
     *  POs are stored in the master schema — @UseDefaultTenant sets the correct schema context. */
    @Scheduled(cron = "0 0 9-18 * * *", zone = "Asia/Kolkata")
    @UseDefaultTenant
    public void computePoPriority() {
        log.info("Scheduled PO priority recompute triggered");
        priorityComputeService.recomputeAllPriorities();
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Kolkata")
    public void sendDailyStockEmailReport() {
        log.info("Daily stock email report triggered");
        try {
            List<String> tenants = schemaConfig.getNonMasterSchemaList();
            Map<Long, Double> netRateMap = stockEmailReportService.fetchLatestNetRateMap();
            List<ProjectStockEmailData> allProjects = new ArrayList<>();

            // Expiry windows
            Calendar cal = Calendar.getInstance();
            Date today = truncateToDay(cal);
            Date day30  = addDays(cal, 30);
            Date day31  = addDays(cal, 31);
            Date day60  = addDays(cal, 60);

            List<ExpiryAlertRow> expiring30 = new ArrayList<>();  // 0–30 days
            List<ExpiryAlertRow> expiring60 = new ArrayList<>();  // 31–60 days

            for (String tenantName : tenants) {
                com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                try {
                    allProjects.add(stockEmailReportService.collectTenantStockData(tenantName, netRateMap));
                    expiring30.addAll(stockEmailReportService.collectExpiryRows(tenantName, today, day30));
                    expiring60.addAll(stockEmailReportService.collectExpiryRows(tenantName, day31, day60));
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
        log.info("Nightly stock balance validation triggered");
        try {
            stockBalanceValidationService.runNightlyValidation();
        } catch (Exception e) {
            log.error("Nightly stock balance validation failed", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
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
