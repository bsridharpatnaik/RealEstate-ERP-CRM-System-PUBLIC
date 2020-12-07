package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.AndroidTokenDetails;
import com.ec.crm.Data.UpComingActivitiesNotifDto;
import com.ec.crm.Model.NotificationSent;
import com.ec.crm.Repository.AndroidTokenDetailsRepo;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.NotificationSentRepository;

import reactor.core.publisher.Mono;

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

			if (nsRepo.findByLeadActivityIdAndStatus(activityId, "Sent") == null) { // If notification is not sent...

				try {

					Long leadId = laRepo.findLeadIdByLeadActivityId(activityId);
					Long userId = leadRepo.findUserIdByLeadId(leadId);
					List<AndroidTokenDetails> tokenDetails = androidRepo.findByUserId(userId);

					for (AndroidTokenDetails token : tokenDetails) {

						log.info("Sending mail for all the devices of a given assignee. Activity ID - " + activityId
								+ "/ Assignee ID - " + userId + "/ Token ID - " + token.getTokenId());

						String target = token.getToken();
						String title = "Notification"; // To do
						String body = "Meeting Scheduled."; // To do

						sendNotification(target, title, body);
					}

					ns.setLeadActivityId(activityId);
					ns.setStatus("Sent");
					nsRepo.save(ns);

				} catch (Exception e) {
					ns.setLeadActivityId(activityId);
					ns.setStatus("Error");
					nsRepo.save(ns);
					e.printStackTrace();
				}
			}
		}

	}

	public void sendNotification(String target, String title, String body) throws Exception {

		UpComingActivitiesNotifDto dto = new UpComingActivitiesNotifDto();
		dto.setTarget(target);
		dto.setTitle(title);
		dto.setBody(body);

		WebClient webClient = WebClient.create(reqUrl + "lanotification/token");
		Mono<String> res = webClient.post().bodyValue(dto).retrieve()
				.onStatus(HttpStatus::is4xxClientError,
						error -> Mono.error(new RuntimeException("Exception in sendNotification method.")))
				.bodyToMono(String.class);

		res.subscribe(val -> log.info("Async firebase response : " + val));

	}

}
