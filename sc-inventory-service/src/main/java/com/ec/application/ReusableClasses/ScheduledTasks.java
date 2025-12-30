package com.ec.application.ReusableClasses;

import com.ec.application.config.SchemaConfig;
import com.ec.application.datasync.DeadStockSyncJob;
import com.ec.application.service.*;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;

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
    DeadStockSyncJob deadStockSyncJob;

    @Scheduled(cron = "0 0 9,18 * * *")
    public void sendStockNotificationEmailInEvening() throws Exception {
        log.info("Sending Stock Notification Email in evening");
        List<String> tenants = schemaConfig.getSchemaList();
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            stockService.sendStockNotificationEmail();
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
        }
    }

    @Scheduled(cron = "0 0 18 * * ?")
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
    }

    @Scheduled(cron = "0 0 * * * *")
    public void updateClosingStock() throws Exception {
        List<String> tenants = schemaConfig.getSchemaList();
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            log.info("Update ClosingStock being triggered for tenant " + tenantName);
            allInventoryService.updateAllInventoryTable();
            allInventoryService.updateClosingStock();
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
        }
    }

    //@Scheduled(cron = "0 0 * * * *")
    @Scheduled(cron = "0 * * * * *")
    public void syncDeadStock() {
        //log.info("Starting scheduled dead stock sync job");
        //deadStockSyncJob.updateDeadStockInMasterAsync();
    }
}
