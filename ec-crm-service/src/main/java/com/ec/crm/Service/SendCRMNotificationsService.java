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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.AndroidTokenDetails;
import com.ec.crm.Data.UpComingActivitiesNotifDto;
import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.NotificationSent;
import com.ec.crm.Repository.AndroidTokenDetailsRepo;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.NotificationSentRepository;

@Service
public class SendCRMNotificationsService {

	@Autowired
	UserDetailsService userDetailService;

	@Autowired
	HttpServletRequest request;

	@Autowired
	LeadActivityRepo laRepo;

	@Autowired
	NotificationSentRepository nsRepo;

	@Autowired
	WebClient.Builder webClientBuilder;

	@Value("${common.serverurl}")
	private String reqUrl;

	@Autowired
	AndroidTokenDetailsRepo androidRepo;
	
	@Autowired
	LeadRepo leadRepo;
	
	Logger log = LoggerFactory.getLogger(SendCRMNotificationsService.class);

	public void sendNotificationForUpcomingActivities() {
		NotificationSent ns = new NotificationSent();
		List<Long> upcomingActivityList = new ArrayList<Long>();
		upcomingActivityList = laRepo.findUpcomingActivities();
		if (upcomingActivityList.isEmpty()) {

			log.info("No upcoming activities.");
		}

		for (Long activityId : upcomingActivityList) {
			
			log.info("for loop entry-----------" + activityId);

			if (nsRepo.findByLeadActivityIdAndStatus(activityId, "Sent") == null) { // If notification is not sent...

				try {
					String firebaseMessageId = "";
					
					Long leadId = laRepo.findLeadIdByLeadActivityId(activityId);
					
					Long userId = leadRepo.findUserIdByLeadId(leadId);
					
					List<AndroidTokenDetails> tokenDetails = androidRepo.findByUserId(userId);
					
					for(AndroidTokenDetails token : tokenDetails) {
					
					log.info("Sending mail for all the devices of a given assignee. Activity ID - " + activityId + "/Assignee ID - "+ userId + "/Token ID - " + token.getTokenId());

					// Get the target from user ID. How do I get user ID?!!
					String target = token.getToken(); //should be asssignee's device token. //"fg0vkrfbTLuYmE7va1XDHu:APA91bGQGwilPNj7UQyRg_7m9BaMtHirWXtyXI_RKfDdkPjqvqMdA7QZZEYEXlm4wbYM_FOJrSLHDTxtRrUxRlgPWm5oylBzcV-7H4M-VufYdqdLvlcMWW9SpfXMbBptCEK3cVh2gxuw";
					String title = "title test";
					String body = "msg body";

					firebaseMessageId = sendNotification(target, title, body);
					}
					log.info(firebaseMessageId);

					ns.setLeadActivityId(activityId);
					ns.setStatus("Sent");
					nsRepo.save(ns);

				} catch (Exception e) {
					ns.setLeadActivityId(activityId);
					ns.setStatus("Error");
					nsRepo.save(ns);
					e.printStackTrace();
				}
				// Create a NotificationException class?
			}
		}

	}

	public String sendNotification(String target, String title, String body) throws Exception {
		String fireBaseMessageId = "Firebase default message : Notification failed.";
		log.info(" sendNotification  begin----------------------------------");

		UpComingActivitiesNotifDto dto = new UpComingActivitiesNotifDto();
		dto.setTarget(target);
		dto.setTitle(title);
		dto.setBody(body);

		
		fireBaseMessageId = webClientBuilder.build().post().uri(reqUrl + "lanotification/token")
				.accept(MediaType.APPLICATION_JSON).bodyValue(dto)
				.header("Authorization",
						"Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzcmVlIiwiZXhwIjoxNjA1ODIwMTk1LCJpYXQiOjE2MDU4MDIxOTV9.PpIll7oO4T0fuZGBAsYQhw6L6Gye-vnpRCoO3r3RgV8oqI6CKlCp30m1cedXBFCPleKZ7_IxpuG8QQunI78I7A")
						//QA-"Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzcmVlIiwiZXhwIjo3MjQxNTI5Mzg0Mzk5MjU3LCJpYXQiOjE2MDU1Mjc5MTJ9.sV6K78UtAHqqCpW-Vm1680WlXuIsonlEqD9hRjBCwQlPsHBfuNq0RDL87KagRnYotFvagFn71U7z5nE84Pt0pw")
				.retrieve().bodyToMono(String.class).block();

		return fireBaseMessageId;
	}

}
