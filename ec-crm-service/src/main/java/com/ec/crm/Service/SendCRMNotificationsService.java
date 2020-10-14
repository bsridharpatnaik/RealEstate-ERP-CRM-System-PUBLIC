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
	}

}
