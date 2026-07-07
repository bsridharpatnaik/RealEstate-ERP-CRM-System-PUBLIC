package com.ec.application.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.EmailHelper;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.ExpiryAlertRow;
import com.ec.application.data.ProjectStockEmailData;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.StockBalanceValidationService;
import com.ec.application.service.StockEmailReportService;
import com.ec.application.service.StockService;

@RestController
@RequestMapping(value = "/email", produces = {"application/json", "text/json"})
public class AutomaticEmailController {

    Logger log = LoggerFactory.getLogger(AutomaticEmailController.class);

    @Autowired
    EmailHelper emailHelper;

    @Autowired
    StockService stockService;

    @Autowired
    StockBalanceValidationService stockBalanceValidationService;

    @Autowired
    StockEmailReportService stockEmailReportService;

    @Autowired
    SchemaConfig schemaConfig;

    @Autowired
    com.ec.application.service.ProjectConstantsService projectConstantsService;

    @GetMapping("/stockupdate")
    public void sendEmail() throws Exception {
        stockService.sendStockNotificationEmail();
    }

    @GetMapping("/stockvalidation")
    public void sendStockValidationEmail() throws Exception {
        stockService.sendStockValidationEmail();
    }

    @GetMapping("/stockbalancecheck")
    public void runStockBalanceCheck() throws Exception {
        stockBalanceValidationService.runNightlyValidation();
    }

    /**
     * Downloads the daily stock report Excel directly — use for local testing
     * when SMTP is not reachable. Verifies data and Excel structure without sending email.
     * GET /email/dailystockreport/download
     */
    @GetMapping("/dailystockreport/download")
    public ResponseEntity<byte[]> downloadDailyStockReport() {
        try {
            List<String> tenants = schemaConfig.getNonMasterSchemaList();
            Map<Long, Double> netRateMap = stockEmailReportService.fetchLatestNetRateMap();
            List<ProjectStockEmailData> allProjects = new ArrayList<>();
            for (String tenantName : tenants) {
                ThreadLocalStorage.setTenantName(tenantName);
                try {
                    allProjects.add(stockEmailReportService.collectTenantStockData(tenantName, netRateMap));
                } finally {
                    ThreadLocalStorage.setTenantName(null);
                }
            }
            byte[] excelBytes = stockEmailReportService.buildExcelBytes(allProjects);
            String fileName = "Stock_Report_" +
                    new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Excel download for stock report failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Manual trigger for the daily stock report email.
     * Use this for local testing — no need to wait for the 9 PM cron.
     * GET /email/dailystockreport
     */
    @GetMapping("/dailystockreport")
    public String triggerDailyStockReport() {
        try {
            List<String> tenants = schemaConfig.getNonMasterSchemaList();
            Map<Long, Double> netRateMap = stockEmailReportService.fetchLatestNetRateMap();
            List<ProjectStockEmailData> allProjects = new ArrayList<>();

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();
            cal.setTime(today); cal.add(Calendar.DAY_OF_YEAR, 60);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59); Date day60 = cal.getTime();

            List<ExpiryAlertRow> expiring30 = new ArrayList<>();
            List<ExpiryAlertRow> expiring60 = new ArrayList<>();

            for (String tenantName : tenants) {
                ThreadLocalStorage.setTenantName(tenantName);
                try {
                    // Near-expiry window is per-tenant config — compute inside the loop
                    int nearDays = projectConstantsService.getNearExpiryDays();
                    cal.setTime(today); cal.add(Calendar.DAY_OF_YEAR, nearDays);
                    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59); Date dayNear = cal.getTime();
                    cal.setTime(today); cal.add(Calendar.DAY_OF_YEAR, nearDays + 1); Date dayNearPlus = cal.getTime();
                    allProjects.add(stockEmailReportService.collectTenantStockData(tenantName, netRateMap));
                    expiring30.addAll(stockEmailReportService.collectExpiryRows(tenantName, today, dayNear));
                    if (nearDays < 60)
                        expiring60.addAll(stockEmailReportService.collectExpiryRows(tenantName, dayNearPlus, day60));
                } finally {
                    ThreadLocalStorage.setTenantName(null);
                }
            }
            if (allProjects.isEmpty()) {
                return "No tenants found — nothing to send";
            }
            byte[] excelBytes = stockEmailReportService.buildExcelBytes(allProjects);
            emailHelper.sendDailyStockReport(allProjects, excelBytes, expiring30, expiring60);
            return "Daily stock report triggered successfully for " + allProjects.size() + " project(s)";
        } catch (Exception e) {
            log.error("Manual trigger of daily stock report failed", e);
            return "Failed: " + e.getMessage();
        }
    }
}
