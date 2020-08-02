package com.ec.crm.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.AllActivitesForLeadDAO;
import com.ec.crm.Data.LeadActivityCreate;
import com.ec.crm.Data.RescheduleActivityData;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.ReusableClasses.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LeadActivityService {
	@Autowired
	LeadActivityRepo laRepo;
	
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	UserDetailsService userDetailsService;
	
	@Autowired
	HttpServletRequest request;
	
	@Value("${common.serverurl}")
	private String reqUrl;
	
	CommonUtils utilObj = new CommonUtils();
	
	Long currentUserId;
	Logger log = LoggerFactory.getLogger(LeadService.class);
	
	@Transactional
	public LeadActivity createLeadActivity(LeadActivityCreate payload) throws Exception 
	{
		log.info("Invoked createLeadActivity");
		validatePayload(payload);
		log.info("Checking if open activity exists");
		exitIfOpenActivityExists(payload);
		log.info("Fetching current user from gateway");
		currentUserId = userDetailsService.getCurrentUser().getId();
		LeadActivity leadActivity=new LeadActivity();
		setFields(leadActivity,payload,"create","user");
		log.info("Closed createLeadActivity");
		return laRepo.save(leadActivity);
	}
	
	private void exitIfOpenActivityExists(LeadActivityCreate payload) throws Exception 
	{
		log.info("Invoked exitIfOpenActivityExists");
		List<LeadActivity> existingActivities = laRepo.findByLeadActivityTypeOpen(payload.getLeadId(),payload.getActivityType());
		if(existingActivities.size()>0)
			throw new Exception("Open activity of type "+payload.getActivityType()+" already exist for lead.");
		log.info("No open activity found with same type");
	}

	private void setFields(LeadActivity leadActivity, LeadActivityCreate payload, String type,String creatorType) 
	{
		log.info("Invoked setFields");
		leadActivity.setActivityDateTime(payload.getActivityDateTime());
		leadActivity.setActivityType(payload.getActivityType());
		leadActivity.setCreatorId(creatorType=="user"?currentUserId:404);
		leadActivity.setDescription(payload.getDescription());
		leadActivity.setTitle(payload.getTitle());
		leadActivity.setTags(payload.getTags());
		leadActivity.setLead(lRepo.findById(payload.getLeadId()).get());
		leadActivity.setDuration(payload.getDuration()==null?0:payload.getDuration());
		
		switch(type)
		{
		case "create":
			leadActivity.setIsOpen(payload.getActivityType().equals(ActivityTypeEnum.Deal_Lost)?false:true);
			break;
		case "update":
			
			break;
		}
	}

	private void validatePayload(LeadActivityCreate payload) throws Exception 
	{
		log.info("Validating LeadId from payload - "+ payload.getLeadId());
		String errorMessage="";
		if(payload.getActivityType()==null)
			errorMessage=errorMessage==""?" Activity Type, ":errorMessage+" Activity Type, ";
		if(payload.getLeadId()==null)
			errorMessage=errorMessage==""?" Lead ID, ":errorMessage+" Lead ID, ";
		if(payload.getActivityDateTime()==null)
			errorMessage=errorMessage==""?" Activity Date & Time, ":errorMessage+" Activity Date & Time, ";
		if(payload.getTitle()==null || payload.getTitle()=="")
			errorMessage=errorMessage==""?" Title, ":errorMessage+" itle, ";
		
		if(errorMessage!="")
			throw new Exception("Fields Missing - "+errorMessage);
		
		
	}

	public Page<LeadActivity> fetchAll(Pageable pageable) 
	{
		return laRepo.findAll(pageable);
	}

	
	
	public LeadActivity getSingleLeadActivity(long id) throws Exception 
	{
		log.info("Invoked activityType");
		Optional<LeadActivity> latype = laRepo.findById(id);
		if(latype.isPresent())
			return latype.get();
		else
			throw new Exception("LeadActivity ID not found");
	}
	
	public void deleteLeadActivity(Long id,String closingComment,Long closedBy) throws Exception 
	{
		log.info("Invoked deleteLeadActivity");
		Optional<LeadActivity> latype = laRepo.findById(id);
		if(!latype.isPresent())
			throw new Exception("LeadActivity ID not found");
		
		LeadActivity leadActivity = latype.get();
		leadActivity.setIsOpen(false);
		leadActivity.setClosingComment(closingComment);
		leadActivity.setClosedBy(closedBy);
		laRepo.save(leadActivity);
	}
	
	@Transactional
	public void rescheduleActivity(Long id,RescheduleActivityData rescheduleActivityData) throws Exception
	{
		log.info("Invoked rescheduleActivity");
		LeadActivity leadActivity = validateReschedulePayloadAndReturnLeadActivity(rescheduleActivityData,id);
		currentUserId = userDetailsService.getCurrentUser().getId();
		
		//Delete old activity
		log.info("Deleting old Activity");
		deleteLeadActivity(leadActivity.getLeadActivityId(),rescheduleActivityData.getClosingComment(),currentUserId);
		log.info("Deleted old Activity - success");
		
		//Create new Activity
		log.info("creating new Activity - success");
		LeadActivity newActivity  = new LeadActivity();
		setFieldsForReschedule(rescheduleActivityData,newActivity,leadActivity);
		laRepo.save(newActivity);
	}

	private  LeadActivity validateReschedulePayloadAndReturnLeadActivity(RescheduleActivityData rescheduleActivityData,Long id) throws Exception 
	{
		log.info("Invoked validateReschedulePayloadAndReturnLeadActivity");
		if(rescheduleActivityData.getRescheduleDateTime()==null)
		{
			log.info("Invalid or Null date and time");
			throw new Exception("Enter valid date and time for rescheduling");
		}
		
		if(rescheduleActivityData.getClosingComment()==null || rescheduleActivityData.getClosingComment().equals(""))
		{
			log.info("Invalid or Null closing comment");
			throw new Exception("Enter valid closing comment");
		}
		
		Optional<LeadActivity> latype = laRepo.findById(id);
		if(!latype.isPresent())
		{
			log.info("Lead Activity with ID - "+id+" not found");
			throw new Exception("LeadActivity ID not found");
		}
		log.info("Payload validation passed. Returning leacactvity back");
		return latype.get();
	}

	private void setFieldsForReschedule(RescheduleActivityData rescheduleActivityData,LeadActivity newActivity, LeadActivity leadActivity) 
	{
		log.info("Invoked setFieldsForReschedule");
		List<String> newTags = new ArrayList<String>();
		for(String tag : leadActivity.getTags())
			newTags.add(tag);

		newActivity.setActivityDateTime(rescheduleActivityData.getRescheduleDateTime());
		newActivity.setActivityType(leadActivity.getActivityType());
		newActivity.setCreatorId(currentUserId);
		newActivity.setDescription(leadActivity.getDescription());
		newActivity.setDuration(leadActivity.getDuration());
		newActivity.setIsOpen(true);
		newActivity.setLead(leadActivity.getLead());
		newActivity.setTags(newTags);
		newActivity.setTitle(leadActivity.getTitle());
	}
	
	public AllActivitesForLeadDAO getAllActivitiesForLead(Long leadId) throws Exception
	{
		log.info("Invoked getAllActivitiesForLead");
		Lead lead = getLeadFromLeadId(leadId);
		AllActivitesForLeadDAO allActivitesForLeadDAO = new AllActivitesForLeadDAO();
		allActivitesForLeadDAO.setPendingActivities(laRepo.fetchPendingActivitiesForLead(lead.getLeadId()));
		allActivitesForLeadDAO.setPastActivities(laRepo.fetchPastActivitiesForLead(lead.getLeadId()));
		return allActivitesForLeadDAO;
	}
	
	public void createDefaultActivity(Long leadId) throws Exception
	{
		log.info("Invoked createDefaultActivity for leadId -" +leadId );
		LocalDate date = LocalDate.now();
		Lead lead = getLeadFromLeadId(leadId);
		LeadActivity newActivity = new LeadActivity();
		newActivity.setActivityDateTime(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		newActivity.setActivityDateTime(new Date());
		newActivity.setActivityType(ActivityTypeEnum.Call);
		newActivity.setCreatorId((long) 404);
		newActivity.setDescription("Default activity created for new Lead");
		newActivity.setDuration((long) 0);
		newActivity.setIsOpen(true);
		newActivity.setLead(lead);
		newActivity.setTitle("New Lead - Call");
		
		
		
	    laRepo.save(newActivity);
	}
	
	private Lead getLeadFromLeadId(Long id) throws Exception
	{
		Optional<Lead> leadOpt = lRepo.findById(id);
		if(!leadOpt.isPresent())
		{
			log.error("Lead with ID not found");
			throw new Exception("Lead with ID not found");
		}
		return leadOpt.get();
	}
}
