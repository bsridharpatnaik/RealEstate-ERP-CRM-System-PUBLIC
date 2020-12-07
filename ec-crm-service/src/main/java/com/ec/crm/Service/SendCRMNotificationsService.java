package com.ec.crm.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.UpComingActivitiesNotifDto;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.NotificationSent;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.NotificationSentRepository;

import reactor.core.publisher.Mono;

@Service
public class SendCRMNotificationsService
{

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
	LeadRepo leadRepo;

	Logger log = LoggerFactory.getLogger(SendCRMNotificationsService.class);

	@Transactional
	public void sendNotificationForUpcomingActivities()
	{

		log.info("Triggerred sendNotificationForUpcomingActivities");
		List<Long> upcomingActivityList = new ArrayList<Long>();
		upcomingActivityList = laRepo.findUpcomingActivities();

		if (upcomingActivityList.isEmpty())
		{

			log.info("No upcoming activities.");
		}

		for (Long activityId : upcomingActivityList)
		{

			UpComingActivitiesNotifDto notificationDTO = new UpComingActivitiesNotifDto();
			if (nsRepo.findByLeadActivityIdAndStatus(activityId, "Sent") == null)
			{ // If notification is not sent...

				NotificationSent ns = new NotificationSent();
				try
				{

					SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm");
					LeadActivity leadActivity = laRepo.getOne(activityId);
					log.info("Sending mail for all the devices of a given assignee. Activity ID - " + activityId
							+ "/ Assignee ID - " + leadActivity.getLead().getAsigneeId());
					notificationDTO.setTitle("Upcoming Activity for - " + leadActivity.getLead().getCustomerName());
					notificationDTO.setBody("Activity Type - " + leadActivity.getActivityType() + ". Time -"
							+ localDateFormat.format(leadActivity.getActivityDateTime()));
					notificationDTO.setTargetUserId(leadActivity.getLead().getAsigneeId());
					sendNotification(notificationDTO);
					ns.setLeadActivityId(activityId);
					ns.setStatus("Sent");
					nsRepo.save(ns);

				} catch (Exception e)
				{
					ns.setLeadActivityId(activityId);
					ns.setStatus("Error");
					nsRepo.save(ns);
					e.printStackTrace();
				}
			}
		}

	}

	@Transactional
	private void sendNotification(UpComingActivitiesNotifDto notificationDTO)
	{
		WebClient webClient = WebClient.create(reqUrl + "notification/send");
		Mono<String> res = webClient.post().bodyValue(notificationDTO).retrieve()
				.onStatus(HttpStatus::is4xxClientError,
						error -> Mono.error(new RuntimeException("Exception in sendNotification method.")))
				.bodyToMono(String.class);

		res.subscribe(val -> log.info("Async firebase response : " + val));
	}

}
