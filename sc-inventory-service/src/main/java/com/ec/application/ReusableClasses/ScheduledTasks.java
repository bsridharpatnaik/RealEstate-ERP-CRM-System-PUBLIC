package com.ec.application.ReusableClasses;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ec.application.data.StockInformationExportDAO;
import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;

//@Component
@EnableScheduling
public class ScheduledTasks {
    @Autowired
    StockService stockService;

    @Value("${schemas.list}")
    private String schemasList;

    Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    SMSService smsService;

    @Autowired
    AsyncServiceInventory asyncServiceInventory;

    @Autowired
    private AsyncService asyncService;

    @Scheduled(cron = "0 0 22 * * ?")
    public void backFillClosingStockForSameDay() {
        log.info("Backfilling Closing Stock for the day");
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            asyncService.run(() ->
            {
                try {
                    asyncServiceInventory.backFillClosingStock(ThreadLocalStorage.getTenantName(), "", new Date(), "scheduler");
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            });
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
            log.info("Backfilling Closing Stock for the day completed");
        }
    }

    @Scheduled(cron = "0 0 9,18 * * *")
    public void sendStockNotificationEmailInEvening() throws Exception {
        log.info("Sending Stock Notification Email in evening");
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            stockService.sendStockNotificationEmail();
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
        }
    }

    @Scheduled(cron = "0 0 18 * * ?")
    public void sendStockValidationEmail() throws Exception {
        log.info("Sending Stock Notification Email in evening");
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            stockService.sendStockValidationEmail();
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
        }
    }

    @Scheduled(cron = "0 0 18 * * MON-SAT")
    public void sendIOStats() throws Exception {
        smsService.sendIOStats();
    }
}
