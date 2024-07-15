package com.ec.crm.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ec.crm.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerService {

    @Autowired
    SendCRMNotificationsService sendCRMNotificationsService;

    @Autowired
    LeadService leadService;

    @Value("${schemas.list}")
    private String schemasList;

    @Autowired
    AllActivitiesService allActivitiesService;

    Logger log = LoggerFactory.getLogger(SchedulerService.class);

    @Scheduled(cron = "0 0 18 * * *")
    public void sendUpcomingActivities() throws Exception {
        log.info("Sending sendUpcomingActivities Email");
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            ThreadLocalStorage.setTenantName(tenantName);
            allActivitiesService.sendEveningEmailForLeadActivity(tenantName);
            ThreadLocalStorage.setTenantName(null);
        }
    }

    @Scheduled(fixedDelay = 60 * 1000 * 15) // 15 minute;
    public void updateLeadDerivedFields() {
        log.info("Triggered store procedure updateLeadDerivedFields - " + new SimpleDateFormat("HH:mm").format(new Date()));
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            try {
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                log.info("Executing update for lead derived fields at " + new SimpleDateFormat("HH:mm").format(new Date()));
                leadService.updateLeadDerivedFields();
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
            } catch (Exception e) {
                //Intentional exception.
                log.info("Exception updateLeadDerivedFields - " + tenantName + "Message - " + e.getMessage());
            }
        }
        log.info("Completed store procedure updateLeadDerivedFields - " + new SimpleDateFormat("HH:mm").format(new Date()));
    }

    @Scheduled(fixedDelay = 60 * 1000 * 180) // 3 hours
    public void updateLeadNotesAndStagnantDays() {
        log.info("Triggered store procedure updateLeadNotesAndStagnantDays - " + new SimpleDateFormat("HH:mm").format(new Date()));
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            try {
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                log.info("Executing update for updateLeadNotesAndStagnantDays at " + new SimpleDateFormat("HH:mm").format(new Date()));
                leadService.updateLeadNotesAndStagnantDays();
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
            } catch (Exception e) {
                //Intentional exception.
                log.info("Exception updatingLeadNotesAndStagnantDays for tenant - " + tenantName + "Message - " + e.getMessage());
            }
        }
        log.info("Completed store procedure updateLeadNotesAndStagnantDays - " + new SimpleDateFormat("HH:mm").format(new Date()));
    }

    @Scheduled(fixedDelay = 60 * 1000 * 20) //18 minutes
    public void callUpdatePipelineActivityForLead() {
        log.info("Triggered store procedure callUpdatePipelineActivityForLead " + new SimpleDateFormat("HH:mm").format(new Date()));
        String[] tenants = schemasList.split(",");
        for (String tenantName : tenants) {
            try {
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
                log.info("Executing update for updatePipelineActivityForLead at " + new SimpleDateFormat("HH:mm").format(new Date()));
                leadService.updatePipelineActivityForLead();
                com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
            } catch (Exception e) {
                //Intentional ignore.
                log.info("Exception updatePipelineActivityForLead - " + tenantName + "Message - " + e.getMessage());
            }
        }
        log.info("Completed store procedure callUpdatePipelineActivityForLead " + new SimpleDateFormat("HH:mm").format(new Date()));
    }
}
