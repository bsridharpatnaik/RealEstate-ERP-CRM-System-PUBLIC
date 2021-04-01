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

@Component
@EnableScheduling
public class SchedulerService
{

	@Autowired
	SendCRMNotificationsService sendCRMNotificationsService;

	@Value("${schemas.list}")
	private String schemasList;

	Logger log = LoggerFactory.getLogger(SchedulerService.class);

	// @Scheduled(cron = "* * * * * *")

	@Scheduled(fixedDelay = 600000) // 1 minute; add another zero to make it 10minutes
	public void sendStockNotificationEmailInEvening() throws Exception
	{
		SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm");
		log.info("Check and send notification to mobile. Current Time - " + localDateFormat.format(new Date()));
		String[] tenants = schemasList.split(",");
		for(String tenantName:tenants) {
			com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(tenantName);
			sendCRMNotificationsService.sendNotificationForUpcomingActivities();
			com.ec.crm.multitenant.ThreadLocalStorage.setTenantName(null);
		}
	}

}
