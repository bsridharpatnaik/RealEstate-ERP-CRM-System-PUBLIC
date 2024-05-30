package com.ec.crm.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
public class SchedulerService {

    @Autowired
    SendCRMNotificationsService sendCRMNotificationsService;

    @Autowired
    LeadService leadService;

    @Value("${schemas.list}")
    private String schemasList;

    Logger log = LoggerFactory.getLogger(SchedulerService.class);

    // @Scheduled(cron = "* * * * * *")

    @Scheduled(fixedDelay = 600000) // 10 minute;
    public void sendStockNotificationEmailInEvening() throws Exception {
        SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm");
        log.info("Check and send notification to mobile. Current Time - " + localDateFormat.format(new Date()));
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
            //sendCRMNotificationsService.sendNotificationForUpcomingActivities();
            sendCRMNotificationsService.sendSMSNotificationForUpcomingActivities();
            com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
        }
    }

    @Scheduled(cron = "*/5 * * * *")
    public void updateLeadDerivedFields() {
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            try {
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                log.info("Executing update for lead derived fields at " + new SimpleDateFormat("HH:mm").format(new Date()));
                leadService.updateLeadDerivedFields();
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
            }
            catch (Exception e) {
                //Intentional exception.
                log.info("Exception updateLeadDerivedFields " + e.getMessage());
            }
        }
    }

    @Scheduled(cron = "*/5 * * * *")
    public void updateLeadNotesAndStagnantDays() {
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            try {
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                log.info("Executing update for updateLeadNotesAndStagnantDays at " + new SimpleDateFormat("HH:mm").format(new Date()));
                leadService.updateLeadNotesAndStagnantDays();
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
            }
            catch (Exception e) {
                //Intentional exception.
                log.info("Exception updatingLeadNotesAndStagnantDays " + e.getMessage());
            }
        }
    }

    @Scheduled(cron = "*/5 * * * *")
    public void callUpdatePipelineActivityForLead() {
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            try {
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                log.info("Executing update for updatePipelineActivityForLead at " + new SimpleDateFormat("HH:mm").format(new Date()));
                leadService.updatePipelineActivityForLead();
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
            }
            catch (Exception e) {
                //Intentional ignore.
                log.info("Exception updatePipelineActivityForLead " + e.getMessage());
            }
        }
    }
}
