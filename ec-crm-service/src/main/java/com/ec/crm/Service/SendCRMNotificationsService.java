package com.ec.crm.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Repository.LeadActivityRepo;

@Service
public class SendCRMNotificationsService
{

	@Autowired
	LeadActivityRepo laRepo;

	Logger log = LoggerFactory.getLogger(SendCRMNotificationsService.class);

	public void sendNotificationForUpcomingActivities()
	{
		/*
		 * try { List<LeadActivity> upcomingActivityList = new
		 * ArrayList<LeadActivity>(); Date currentTime = new Date();
		 * upcomingActivityList = laRepo.findUpcomingActivities(currentTime);
		 * log.info("Upcoming Activities -" + upcomingActivityList.size()); } catch
		 * (Exception e) { log.error(e.getMessage()); }
		 */

		// TO DO
		// 1. Identify all records having acivitydatetime in 5 mins
		// 2. Iterate through the records and see if nitification is alreadt sent to
		// them
		// 3. If notification is not sent, send the notification and store in history
		// call gatewat service to send notification
	}

}
