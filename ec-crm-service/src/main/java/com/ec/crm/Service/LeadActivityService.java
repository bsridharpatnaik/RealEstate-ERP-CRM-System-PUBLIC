package com.ec.crm.Service;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.AllActivitesForLeadDAO;
import com.ec.crm.Data.LeadActivityCreate;
import com.ec.crm.Data.LeadListWithTypeAheadData;
import com.ec.crm.Data.LeadPageData;
import com.ec.crm.Data.RescheduleActivityData;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.LeadSpecifications;
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
		setFields(leadActivity,payload,"user");
		log.info("Closed createLeadActivity");
		laRepo.save(leadActivity);
		ExecuteBusinessLogicWhileCreation(leadActivity);
		return leadActivity;
		
	}
	
	@Transactional
	private void ExecuteBusinessLogicWhileCreation(LeadActivity leadActivity) 
	{
		log.info("Invoked ExecuteBusinessLogicWhileCreation");
		LeadStatusEnum status = leadActivity.getLead().getStatus();
		switch(leadActivity.getActivityType())
		{
		case Deal_Close:
			log.info("Inside Case - Deal_Close");
			leadActivity.getLead().setStatus(LeadStatusEnum.Deal_Closed);
			closeAllOpenActivitiesForLead(leadActivity.getLead());
			break;
		case Deal_Lost:
			log.info("Inside Case - Deal_Lost");
			leadActivity.getLead().setStatus(LeadStatusEnum.Deal_Lost);
			closeAllOpenActivitiesForLead(leadActivity.getLead());
			break;
		case Property_Visit:
			log.info("Inside Case - Property_Visit");
			leadActivity.getLead().setStatus(LeadStatusEnum.Property_Visit);
			break;
		}
		laRepo.save(leadActivity);
	}
	
	@Transactional
	private void ExecuteBusinessLogicWhileClosure(LeadActivity leadActivity) 
	{
		log.info("Invoked ExecuteBusinessLogicWhileClosure");
		if(leadActivity.getActivityType().equals(ActivityTypeEnum.Property_Visit))
		{
			log.info("Changing status of lead from Property_Visit - Negotiation");
			leadActivity.getLead().setStatus(LeadStatusEnum.Negotiation);
		}
		laRepo.save(leadActivity);
	}
	
	@Transactional
	private void closeAllOpenActivitiesForLead(Lead lead) 
	{
		log.info("Invoked closeAllOpenActivitiesForLead");
		Long leadId = lead.getLeadId();
		List<LeadActivity> activities = laRepo.findAllByOpenActivitiesByLeadId(leadId);
		for(LeadActivity activity:activities)
		{
			activity.setClosedBy((long) 404);
			activity.setClosingComment("Auto-Closed due to business logic");
			activity.setIsOpen(false);
			laRepo.save(activity);
		}
	}
	
	private void exitIfOpenActivityExists(LeadActivityCreate payload) throws Exception 
	{
		log.info("Invoked exitIfOpenActivityExists");
		List<LeadActivity> existingActivities = laRepo.findByLeadActivityTypeOpen(payload.getLeadId(),payload.getActivityType());
		if(existingActivities.size()>0)
			throw new Exception("Open activity of type "+payload.getActivityType()+" already exist for lead.");
		log.info("No open activity found with same type");
	}

	private void setFields(LeadActivity leadActivity, LeadActivityCreate payload, String creatorType) 
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
		
		if(payload.getActivityType().equals(ActivityTypeEnum.Deal_Lost) || payload.getActivityType().equals(ActivityTypeEnum.Deal_Close))
			leadActivity.setIsOpen(false);
		else 
			leadActivity.setIsOpen(true);
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
		ExecuteBusinessLogicWhileClosure(leadActivity);
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
	public List<LeadPageData> getLeadActivityPage(FilterDataList leadFilterDataList, Pageable pageable) throws ParseException 
	{
		//Page<Lead> leads=lRepo.findAll(pageable);   change code here to get list of leads based on leads filters selected
		log.info("Invoked findFilteredList with payload - " + leadFilterDataList.toString());
		LeadListWithTypeAheadData leadListWithTypeAheadData = new LeadListWithTypeAheadData();
		
		log.info("Fetching filteration based on filter data received");
		Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);
		
		log.info("Fetching records based on specification");
		Page<Lead> leads;
		if(spec!=null)
			leads=lRepo.findAll(spec, pageable);
		else 		
			leads=lRepo.findAll(pageable);
		
		log.info("Get all the leads");
		System.out.println(leads);
		List<LeadPageData> pagedata=new ArrayList<>();

		//
		//Change this code - Bad code - hardocded value 
		//
		for(Lead lead:leads) 
		{
				List<LeadActivity> activities=laRepo.findAllActivitiesForLead(lead.getLeadId());
				log.info("Get all the Activity");
				System.out.println(lead.getCustomerName());
				System.out.println(activities);
				LeadPageData activity=new LeadPageData();
				try {
					activity = getDisplayActivityForLead(activities);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				pagedata.add(activity);
		}
		//Add filter logic using streams to filter based on activity filter selected
		return pagedata;
		
	}
	public LeadPageData getDisplayActivityForLead(List<LeadActivity> activities) throws Exception
	{
		boolean isPendingExists=false;
		//boolean isClosedExists=false;
		boolean isUpcomingExists=false;
		boolean pastExists=false;
		LeadPageData returndata=new LeadPageData();
		Long leadId = activities.get(0).getLead().getLeadId();
		for(LeadActivity activity:activities) 
		{
			log.info("check if any activity is open");
			System.out.println("Start Of Day - "+atStartOfDay(new Date()));
			System.out.println("End Of Day - "+atEndOfDay(new Date()));
			
			System.out.println(activity.getActivityDateTime().after(atStartOfDay(new Date())));
			
			
			if(activity.getIsOpen()==true && activity.getActivityDateTime().after(atStartOfDay(new Date())) && activity.getActivityDateTime().before(atEndOfDay(new Date()))) // Pass time zone to constructor.) 
				isPendingExists=true;
				
			if(activity.getIsOpen()==true && activity.getActivityDateTime().after(atEndOfDay(new Date()))) // Pass time zone to constructor.) 
				isUpcomingExists=true;
			
			if(activity.getIsOpen()==true && activity.getActivityDateTime().before(atStartOfDay(new Date()))) // Pass time zone to constructor.) 
				pastExists=true;
		}
		
		System.out.println("isPendingExists - " + isPendingExists);
		System.out.println("isUpcomingExists - " + isUpcomingExists);
		System.out.println("pastExists - " + pastExists);
		/*
		 * Only Pending Only past Only upcoming
		 * 
		 * If pending ignore others
		 * 
		 * !past && !up past * up past & !up !past & up
		 */

		if(isPendingExists)
		{
			returndata = transformDataFromActivity(laRepo.getRecentPendingActivity(leadId,atStartOfDay(new Date()),atEndOfDay(new Date())));
		}
		
		else if(!isUpcomingExists && !isPendingExists && pastExists) 
		{
			returndata = transformDataFromActivity(laRepo.getRecentClosedActivity(leadId));
		}
		 
		else if(!pastExists && !isPendingExists && !isUpcomingExists)
		{
			returndata = transformDataFromActivity(laRepo.getRecentActivityIrrespectiveOfStatus(leadId));
		}
				
		else if (!isPendingExists)
		{
			if(!pastExists)
			{
				returndata = transformDataFromActivity(laRepo.getRecentUpcomingActivity(leadId,atEndOfDay(new Date())));
			}
			
			else if(isUpcomingExists && !pastExists)
			{
				returndata = transformDataFromActivity(laRepo.getRecentUpcomingActivity(leadId,atEndOfDay(new Date())));
			}
			else if(isUpcomingExists && pastExists)
			{
				returndata = transformDataFromActivity(laRepo.getRecentUpcomingActivity(leadId,atEndOfDay(new Date())));
			}
			else if(!isUpcomingExists && pastExists)
			{
				returndata = transformDataFromActivity(laRepo.getRecentPastActivity(leadId,atStartOfDay(new Date())));
			}
		}
		return returndata;
	}
	

	private LeadPageData transformDataFromActivity(List<LeadActivity> recentPendingActivity) throws Exception 
	{
		if(recentPendingActivity.size()==0)
			throw new Exception("No Pending ");
		LeadPageData leadPageData = new LeadPageData(recentPendingActivity.get(0));
		return leadPageData;
	}

	public static Date atStartOfDay(Date date) {
	    LocalDateTime localDateTime = dateToLocalDateTime(date);
	    LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
	    return localDateTimeToDate(startOfDay);
	}
	
	public static Date atEndOfDay(Date date) {
	    LocalDateTime localDateTime = dateToLocalDateTime(date);
	    LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
	    return localDateTimeToDate(endOfDay);
	}
	
	private static LocalDateTime dateToLocalDateTime(Date date) {
	    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
	}

	private static Date localDateTimeToDate(LocalDateTime localDateTime) {
	    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}
}


