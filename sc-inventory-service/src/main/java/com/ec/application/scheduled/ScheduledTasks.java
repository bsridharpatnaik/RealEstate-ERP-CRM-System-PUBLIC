package com.ec.application.scheduled;

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

    /** Runs at midnight IST (18:30 UTC) every day. Recomputes PO priority from indent expected dates. */
    @Scheduled(cron = "0 30 18 * * *")
    public void computePoPriority() {
        List<String> tenants = schemaConfig.getNonMasterSchemaList();
        for (String tenantName : tenants) {
            try {
                com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                log.info("Computing PO priority for tenant {}", tenantName);
                priorityComputeService.recomputeAllPriorities();
            } catch (Exception e) {
                log.error("Error computing PO priority for tenant {}: {}", tenantName, e.getMessage());
            } finally {
                com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
            }
        }
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
