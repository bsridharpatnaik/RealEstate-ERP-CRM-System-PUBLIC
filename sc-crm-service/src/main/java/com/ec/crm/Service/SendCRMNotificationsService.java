package com.ec.crm.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import com.ec.crm.Data.SMSGatewayResponse;
import com.ec.crm.Model.UserMobileNoMapping;
import com.ec.crm.Repository.UserMobileNoMappingRepo;
import com.ec.crm.ReusableClasses.ProjectConstants;
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
    LeadRepo leadRepo;

    @Autowired
    UserMobileNoMappingRepo userMobileNoMappingRepo;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    SMSService smsService;

    Logger log = LoggerFactory.getLogger(SendCRMNotificationsService.class);

    @Transactional
    public void sendNotificationForUpcomingActivities() {

        log.info("Triggerred sendNotificationForUpcomingActivities");
        List<Long> upcomingActivityList = new ArrayList<Long>();
        upcomingActivityList = laRepo.findUpcomingActivities();

        if (upcomingActivityList.isEmpty()) {

            log.info("No upcoming activities.");
        }

        for (Long activityId : upcomingActivityList) {

            UpComingActivitiesNotifDto notificationDTO = new UpComingActivitiesNotifDto();
            if (nsRepo.findByLeadActivityIdAndStatus(activityId, "Sent") == null) { // If notification is not sent...

                NotificationSent ns = new NotificationSent();
                try {

                    SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm");
                    LeadActivity leadActivity = laRepo.getOne(activityId);
                    log.info("Sending mail for all the devices of a given assignee. Activity ID - " + activityId
                            + "/ Assignee ID - " + leadActivity.getLead().getAsigneeId());
                    notificationDTO.setTitle("Upcoming Activity for - " + leadActivity.getLead().getCustomerName());
                    notificationDTO.setBody("Activity Type - " + leadActivity.getActivityType() + ". Time -"
                            + localDateFormat.format(leadActivity.getActivityDateTime()));
                    notificationDTO.setTargetUserId(leadActivity.getLead().getAsigneeId());
                    String response = sendNotification(notificationDTO);

                    if (response.toLowerCase().contains("error")) {
                        ns.setLeadActivityId(activityId);
                        ns.setStatus(response);
                        nsRepo.save(ns);
                    } else {
                        ns.setLeadActivityId(activityId);
                        ns.setStatus("Sent");
                        nsRepo.save(ns);
                    }

                } catch (Exception e) {
                    ns.setLeadActivityId(activityId);
                    ns.setStatus("Exception while sending notification");
                    nsRepo.save(ns);
                    e.printStackTrace();
                }
            }
        }

    }

    @Transactional
    private String sendNotification(UpComingActivitiesNotifDto notificationDTO) {
        String response;
        WebClient webClient = WebClient.create(reqUrl + "notification/send");
        Mono<String> res = webClient.post().bodyValue(notificationDTO).retrieve()
                .onStatus(HttpStatus::is4xxClientError,
                        error -> Mono.error(new RuntimeException("Exception in sendNotification method.")))
                .bodyToMono(String.class);

        response = res.block();
        log.info("Async firebase response : " + response);
        return response;
    }

    public void sendSMSNotificationForUpcomingActivities() {
        log.info("Triggered sendSMSNotificationForUpcomingActivities");
        List<Long> upcomingActivityList = new ArrayList<Long>();
        upcomingActivityList = laRepo.findUpcomingActivities();

        if (upcomingActivityList.isEmpty())
            log.info("No upcoming activities.");

        for (Long activityId : upcomingActivityList) {
            UpComingActivitiesNotifDto notificationDTO = new UpComingActivitiesNotifDto();
            if (nsRepo.findByLeadActivityIdAndStatus(activityId, "SmsSent") == null) { // If SMS notification is not sent...
                NotificationSent ns = new NotificationSent();
                try {
                    SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm");
                    LeadActivity leadActivity = laRepo.findById(activityId).get();
                    log.info("Sending SMS to assignee. Activity ID - " + activityId
                            + "/ Assignee ID - " + leadActivity.getLead().getAsigneeId());
                    String flow_id = ProjectConstants.flowIdForActivityNotification;
                    String mobileNo = getMobileForAssignee(leadActivity.getLead().getAsigneeId());

                    if (mobileNo != null) {
                        log.info("Valid mobile number found for assignee - " + leadActivity.getLead().getAsigneeId());
                        HashMap<String, String> payload = new HashMap<>();
                        payload.put("flow_id", flow_id);
                        payload.put("sender", "GBCONS");
                        payload.put("mobiles", mobileNo);
                        payload.put("name",leadActivity.getLead().getCustomerName());
                        payload.put("type",leadActivity.getActivityType().toString());
                        payload.put("time",localDateFormat.format(leadActivity.getActivityDateTime()));
                        payload.put("custmobile",leadActivity.getLead().getPrimaryMobile());
                        log.info("Payload to be sent - "+payload.toString());
                        SMSGatewayResponse response = smsService.sendSMStoGateway(payload);
                        log.info("SMS Sent - " + payload.toString());
                        if (response.getMessage().toLowerCase().contains("success")) {
                            ns.setLeadActivityId(activityId);
                            ns.setStatus("success");
                            nsRepo.save(ns);
                        } else {
                            ns.setLeadActivityId(activityId);
                            ns.setStatus("SmsSent");
                            nsRepo.save(ns);
                        }
                    }
                    else{
                        log.info("Valid mobile number not found for user");
                    }
                } catch (Exception e) {
                    ns.setLeadActivityId(activityId);
                    ns.setStatus("Exception while sending SMS notification");
                    nsRepo.save(ns);
                    e.printStackTrace();
                }
            }
        }
    }

    private String getMobileForAssignee(Long assigneeId) {
        Optional<UserMobileNoMapping> userDetailOpt = userMobileNoMappingRepo.findById(assigneeId);
        if(!userDetailOpt.isPresent())
            return null;
        else
            return userDetailOpt.get().getMobileNumber();
    }
}
