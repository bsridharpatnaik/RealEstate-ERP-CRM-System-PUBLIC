package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.UpComingActivitiesNotifDto;
import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.NotificationSent;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.NotificationSentRepository;

@Service
public class SendCRMNotificationsService
{

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	LeadActivityRepo laRepo;
	
	@Autowired
	NotificationSentRepository nsRepo;
	
	@Autowired
	WebClient.Builder webClientBuilder;
	
	
	//@Autowired
//	RequestContextHolder requestContextHolder;
	
	@Value("${common.serverurl}")
	private String reqUrl;
	
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

		log.info("CRM sendNotificationForUpcomingActivities begin----------------------------------");
		
		List<Long> upcomingActivityList = new  ArrayList<Long>();
     	  upcomingActivityList = laRepo.findUpcomingActivities();
     	  log.info("upcomingActivityList IDs --------------" + upcomingActivityList);
			for(Long activityId : upcomingActivityList) {
				log.info("for-----------" + activityId);
				if(nsRepo.findByLeadActivityId(activityId) == null) { //If notification is not sent...
					
				log.info("Send mail and update the NotificationsSent table for the activity id - " + activityId);
				NotificationSent ns = new NotificationSent();
				ns.setLeadActivityId(activityId);
				ns.setStatus("Sent");
				//call the gateway method using web client
			
				nsRepo.save(ns);
				//Get the target from user ID. How do I get user ID?!!
				String target = "fg0vkrfbTLuYmE7va1XDHu:APA91bGQGwilPNj7UQyRg_7m9BaMtHirWXtyXI_RKfDdkPjqvqMdA7QZZEYEXlm4wbYM_FOJrSLHDTxtRrUxRlgPWm5oylBzcV-7H4M-VufYdqdLvlcMWW9SpfXMbBptCEK3cVh2gxuw";
				String title = "from the method";
				String body = "web client - target hardcoded";
				
				String firebaseMessageId = sendNotification(target,title,body);
				log.info("The firebase message ID returned  - " + firebaseMessageId);
				}
			}

		
	}
	public String sendNotification(String target, String title ,String body)
    {
		String fireBaseMessageId = "response todo";
		log.info(" sendNotification  begin----------------------------------" + request);

		UpComingActivitiesNotifDto dto = new UpComingActivitiesNotifDto();
		dto.setTarget(target);
		dto.setTitle(title);
		dto.setBody(body);
		
		webClientBuilder.build()
					    	.post()
					    	.uri(reqUrl+"lanotification/token")
					    	.accept(MediaType.APPLICATION_JSON)
					    	.bodyValue(dto)
					    	.header("Authorization",
					    			"Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzcmVlIiwiZXhwIjo3MjQxNTI5Mzg0Mzk5MjU3LCJpYXQiOjE2MDU1Mjc5MTJ9.sV6K78UtAHqqCpW-Vm1680WlXuIsonlEqD9hRjBCwQlPsHBfuNq0RDL87KagRnYotFvagFn71U7z5nE84Pt0pw")
					    	.retrieve()
					    	.bodyToMono(String.class)
					    	.block();
    	
    	return fireBaseMessageId;
    }
	


//	@Bean
//	private HttpServletRequest getRequest() {
//	       // return new RequestContextListener();
//	    }

}
