package com.ec.application.scheduled;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    StockSyncJob stockSyncJob;

    @Autowired
    PriorityComputeService priorityComputeService;

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

    @Scheduled(cron = "0 0 * * * *")
    public void updateClosingStock() throws Exception {
        List<String> tenants = schemaConfig.getNonMasterSchemaList();
        for (String tenantName : tenants) {
            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            log.info("Update ClosingStock being triggered for tenant " + tenantName);
            allInventoryService.updateClosingStock();
            allInventoryService.updateAllInventoryTable();

            com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
        }
    }
}
