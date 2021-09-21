package com.ec.application.ReusableClasses;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ec.application.service.SMSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ec.application.data.StockInformationExportDAO;
import com.ec.application.service.StockHistoryService;
import com.ec.application.service.StockService;
import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;

//@Component
@EnableScheduling
public class ScheduledTasks 
{
	@Autowired
	StockService stockService;

	@Value("${schemas.list}")
	private String schemasList;

	Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

	@Autowired
	SMSService smsService;

	/*@Scheduled(fixedDelay = 6000) // 1 minute; add another zero to make it 10minutes
	public void senddummy() throws Exception {
		SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm");
		log.info("Current Time - " + localDateFormat.format(new Date()));
	}*/

	@Scheduled(cron = "0 0 9,18 * * *")
	public void sendStockNotificationEmailInEvening() throws Exception 
	{
		log.info("Sending Stock Notification Email in evening");
		String[] tenants = schemasList.split(",");
		for(String tenantName:tenants)
		{
			com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
			stockService.sendStockNotificationEmail();
			com.ec.application.multitenant.ThreadLocalStorage.setTenantName(null);
		}
	}
	
	@Scheduled(cron = "0 0 18 * * ?")
	public void sendStockValidationEmail() throws Exception 
	{
		log.info("Sending Stock Notification Email in evening");
		String[] tenants = schemasList.split(",");
		for(String tenantName:tenants) {
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
