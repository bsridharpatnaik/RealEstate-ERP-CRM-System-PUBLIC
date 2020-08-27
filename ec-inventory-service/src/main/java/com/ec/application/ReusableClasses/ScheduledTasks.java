package com.ec.application.ReusableClasses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledTasks 
{
	@Autowired
	EmailHelper  emailHelper;
	
	Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
	
	@Scheduled(initialDelay = 1000, fixedRate = 60000)
	public void sendStockNotificationEmail() 
	{
	    log.info("Sending Stock Notification Email");
	    emailHelper.sendEmail();
	}
}
