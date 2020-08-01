package com.ec.crm.Service;

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

import com.ec.crm.Data.LeadActivityCreate;
import com.ec.crm.Data.RescheduleActivityData;
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
		setFields(leadActivity,payload,"create");
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

	private void setFields(LeadActivity leadActivity, LeadActivityCreate payload, String type) 
	{
		log.info("Invoked setFields");
		leadActivity.setActivityDateTime(payload.getActivityDateTime());
		leadActivity.setActivityType(payload.getActivityType());
		leadActivity.setCreatorId(currentUserId);
		leadActivity.setDescription(payload.getDescription());
		leadActivity.setTitle(payload.getTitle());
		leadActivity.setTags(payload.getTags());
		leadActivity.setLead(lRepo.findById(payload.getLeadId()).get());
		leadActivity.setDuration(payload.getDuration()==null?0:payload.getDuration());
		
		switch(type)
		{
		case "create":
			leadActivity.setIsOpen(true);
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
		
		Optional<Lead> leadOpt = lRepo.findById(payload.getLeadId());
		if(!leadOpt.isPresent())
		{
			log.error("Lead with ID not found");
			throw new Exception("Lead with ID not found");
		}
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
		currentUserId = userDetailsService.getCurrentUser().getId();
		log.info("Invoked rescheduleActivity");
		if(rescheduleActivityData.getRescheduleDateTime()==null)
		{
			log.info("Invalid or Null date and time");
			throw new Exception("Enter valid date and time for rescheduling");
		}
		
		Optional<LeadActivity> latype = laRepo.findById(id);
		if(!latype.isPresent())
			throw new Exception("LeadActivity ID not found");
		LeadActivity leadActivity =  latype.get();
		
		//Delete old activity
		log.info("Deleting old Activity");
		deleteLeadActivity(leadActivity.getLeadActivityId(),"Rescheduled by User - "+userDetailsService.getCurrentUser().getUsername(),currentUserId);
		log.info("Deleted old Activity - success");
		//Create new Activity
		log.info("creating new Activity - success");
		LeadActivity newActivity  = new LeadActivity();
		setFieldsForReschedule(rescheduleActivityData,newActivity,leadActivity);
		laRepo.save(newActivity);
	}

	private void setFieldsForReschedule(RescheduleActivityData rescheduleActivityData,LeadActivity newActivity, LeadActivity leadActivity) 
	{
		newActivity.setActivityDateTime(rescheduleActivityData.getRescheduleDateTime());
		newActivity.setActivityType(leadActivity.getActivityType());
		newActivity.setCreatorId(currentUserId);
		newActivity.setDescription(leadActivity.getDescription());
		newActivity.setDuration(leadActivity.getDuration());
		newActivity.setIsOpen(true);
		newActivity.setLead(leadActivity.getLead());
		newActivity.setTags(leadActivity.getTags());
		newActivity.setTitle(leadActivity.getTitle());
	}
}
