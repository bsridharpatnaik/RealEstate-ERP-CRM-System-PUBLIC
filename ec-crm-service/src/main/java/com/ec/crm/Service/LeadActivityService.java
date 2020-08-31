package com.ec.crm.Service;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
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
import com.ec.crm.Data.LeadActivityListWithTypeAheadData;
import com.ec.crm.Data.LeadPageData;
import com.ec.crm.Data.RescheduleActivityData;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Filters.ActivitySpecifications;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Mapper.LeadActivityMapper;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.ReusableClasses.ReusableMethods;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class LeadActivityService {
	@Autowired
	LeadActivityRepo laRepo;
	
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	UserDetailsService userDetailsService;
	
	@Autowired
	LeadService lService;
	
	@Autowired
	HttpServletRequest request;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	@Autowired
	ModelMapper leadToLeadActivityModelMapper;
	
	@Autowired
	LeadActivityMapper laMapper;
	@Value("${common.serverurl}")
	private String reqUrl;
	
	Long currentUserId;
	Logger log = LoggerFactory.getLogger(LeadService.class);
	
	@Autowired
	private LeadActivityMapper mapper;
	
	@Transactional
	public LeadActivity createLeadActivity(LeadActivityCreate payload) throws Exception 
	{
		log.info("Invoked createLeadActivity");
		validatePayload(payload);
		log.info("Checking if open activity exists");
		exitCreateIfExitConditionsExists(payload);
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
	  public void revertLeadActivity(Long id) throws Exception 
	  {
		  	log.info("Invoked revertLeadActivity"); Optional<LeadActivity> latype =
			laRepo.findById(id); if(!latype.isPresent()) throw new
			Exception("LeadActivity by ID not found");
	  
			LeadActivity leadActivity = latype.get();
	  
			if(leadActivity.getIsOpen()==true && leadActivity.getActivityType().equals(ActivityTypeEnum.Property_Visit))
			{	
				leadActivity.getLead().setStatus(LeadStatusEnum.New_Lead);
				leadActivity.setClosedBy(currentUserId);
				leadActivity.setClosingComment("Activity reverted");
				leadActivity.setIsOpen(false);
			}	
			else if(leadActivity.getIsOpen()==false && (leadActivity.getActivityType().equals(ActivityTypeEnum.Deal_Close) 
					|| leadActivity.getActivityType().equals(ActivityTypeEnum.Deal_Lost)))
			{
				leadActivity.getLead().setStatus(LeadStatusEnum.Negotiation);
				leadActivity.setClosedBy(currentUserId);
				leadActivity.setClosingComment("Activity reverted");
				leadActivity.setIsOpen(false);
			}
			else if(leadActivity.getIsOpen()==false && leadActivity.getActivityType().equals(ActivityTypeEnum.Property_Visit))
			{
				leadActivity.getLead().setStatus(LeadStatusEnum.Property_Visit);
				leadActivity.setClosedBy(null);
				leadActivity.setClosingComment("");
				leadActivity.setIsOpen(true);
			}
			else 
				throw new Exception("Revert operation for activiy not allowed");
	  }
	 
	@Transactional
	private void ExecuteBusinessLogicWhileCreation(LeadActivity leadActivity) throws Exception 
	{
		log.info("Invoked ExecuteBusinessLogicWhileCreation");
		LeadStatusEnum status = leadActivity.getLead().getStatus();
		
		//do not allow creation of any activity except meeeting
		if(leadActivity.getLead().getStatus().equals(LeadStatusEnum.Deal_Lost) && !leadActivity.getActivityType().equals(ActivityTypeEnum.Meeting))
			throw new Exception("Cannot add activity - Deal already lost! Please create a Meeting activity to reopen lead.");
	
		
		switch(leadActivity.getActivityType())
		{
			case Deal_Close:
				log.info("Inside Case - Deal_Close");
				leadActivity.getLead().setStatus(LeadStatusEnum.Deal_Closed);
				closeAllOpenActivitiesForLead(leadActivity.getLead());
				break;
			case Deal_Lost:
				log.info("Inside Case - Deal_Lost");
				leadActivity.getLead().setStatus(fetchPreviousStatusFromHistory(leadActivity.getLead()));
				closeAllOpenActivitiesForLead(leadActivity.getLead());
				break;
			case Property_Visit:
				log.info("Inside Case - Property_Visit");
				leadActivity.getLead().setStatus(LeadStatusEnum.Property_Visit);
				break;
		}
		
		
		laRepo.save(leadActivity);
	}
	
	/*
	 * @Transactional private void ExecuteBusinessLogicWhileCreation(LeadActivity
	 * leadActivity) throws Exception {
	 * log.info("Invoked ExecuteBusinessLogicWhileCreation"); LeadStatusEnum status
	 * = leadActivity.getLead().getStatus();
	 * 
	 * //do not allow creation of any activity except meeeting
	 * if(leadActivity.getLead().getStatus().equals(LeadStatusEnum.Deal_Lost) &&
	 * !leadActivity.getActivityType().equals(ActivityTypeEnum.Meeting)) throw new
	 * Exception("Cannot add activity - Deal already lost! Please create a Meeting activity to reopen lead."
	 * );
	 * 
	 * 
	 * switch(leadActivity.getActivityType()) { case Deal_Close:
	 * log.info("Inside Case - Deal_Close");
	 * leadActivity.getLead().setStatus(LeadStatusEnum.Deal_Closed);
	 * closeAllOpenActivitiesForLead(leadActivity.getLead()); break; case Deal_Lost:
	 * log.info("Inside Case - Deal_Lost");
	 * leadActivity.getLead().setStatus(fetchPreviousStatusFromHistory(leadActivity.
	 * getLead())); closeAllOpenActivitiesForLead(leadActivity.getLead()); break;
	 * case Property_Visit: log.info("Inside Case - Property_Visit");
	 * leadActivity.getLead().setStatus(LeadStatusEnum.Property_Visit); break; }
	 * 
	 * 
	 * laRepo.save(leadActivity); }
	 */
	
	@Transactional
	private void ExecuteBusinessLogicWhileClosure(LeadActivity leadActivity) throws Exception 
	{
		log.info("Invoked ExecuteBusinessLogicWhileClosure");
		if(leadActivity.getActivityType().equals(ActivityTypeEnum.Property_Visit))
		{
			log.info("Changing status of lead from Property_Visit - Negotiation");
			leadActivity.getLead().setStatus(LeadStatusEnum.Negotiation);
		}
		
		if(leadActivity.getActivityType().equals(ActivityTypeEnum.Meeting) 
				&& leadActivity.getLead().getStatus().equals(LeadStatusEnum.Deal_Lost))
		{
			log.info("Changing status of lead from Deal_Lost - Negotiation");
			LeadStatusEnum previousStatus = fetchPreviousStatusFromHistory(leadActivity.getLead());
			leadActivity.getLead().setStatus(previousStatus);
		}
		
		laRepo.save(leadActivity);
	}
	
	private LeadStatusEnum fetchPreviousStatusFromHistory(Lead lead) throws Exception 
	{
		log.info("Fetching previous status for lead -"+lead.getLeadId());
		List<Lead> leadHistory = lService.history(lead.getLeadId());
		LeadStatusEnum previousStatus = lead.getStatus();
		
		if(leadHistory.size()<1)
			throw new Exception("No history found for lead");
		
		for(int ctr=leadHistory.size()-1;ctr>=0;ctr--)
		{
			Lead currentLead = leadHistory.get(ctr);
			if(currentLead.getStatus().equals(lead.getStatus()) == false)
			{
				previousStatus = currentLead.getStatus();
				log.info("Previous status found for lead - "+currentLead.getLeadId()+" - "+previousStatus);
				break;
			}
		}
		return previousStatus;
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
	
	private void exitCreateIfExitConditionsExists(LeadActivityCreate payload) throws Exception 
	{
		log.info("Invoked exitIfExitConditionsExists");
		
		Optional<Lead> leadOpt = lRepo.findById(payload.getLeadId());
		if(!leadOpt.isPresent())
			throw new Exception("Lead with Lead ID -" + payload.getLeadId()+" not found");
		Lead lead = leadOpt.get();
		
		if(lead.getStatus().equals(LeadStatusEnum.Deal_Lost) && !payload.getActivityType().equals(ActivityTypeEnum.Meeting))
			throw new Exception("Lead is already lost. Create a meeting type activity to re-activate the lead.");
		
		List<LeadActivity> existingActivities = laRepo.findByLeadActivityTypeOpen(payload.getLeadId(),payload.getActivityType());
		if(existingActivities.size()>0)
			throw new Exception("Open activity of type "+payload.getActivityType()+" already exist for lead.");
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
		Optional<Lead> leadOpt = lRepo.findById(payload.getLeadId());
		
		if(!leadOpt.isPresent())
			throw new Exception("Lead not found by lead ID -"+payload.getLeadId());
		else if((leadOpt.get().getStatus().equals(LeadStatusEnum.New_Lead) || 
				leadOpt.get().getStatus().equals(LeadStatusEnum.Deal_Lost)) && payload.getActivityType().equals(ActivityTypeEnum.Deal_Close))
				throw new Exception("You cannot close a new lead or a lost lead. Lead should be closed only after property visit");
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
		
		if(leadActivity.getIsOpen()==false)
			throw new Exception("Activity alread closed with comment - "+ leadActivity.getClosingComment());
		
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
		
		if(latype.get().getIsOpen()==false)
			throw new Exception("Lead Activity already closed with comment -"+latype.get().getClosingComment()==null?"":latype.get().getClosingComment());
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
		allActivitesForLeadDAO.setPendingActivities(laMapper.mapLeadActivitiesToDTOs(laRepo.fetchPendingActivitiesForLead(lead.getLeadId())));
		allActivitesForLeadDAO.setPastActivities(laMapper.mapLeadActivitiesToDTOs(laRepo.fetchPastActivitiesForLead(lead.getLeadId())));
		return (allActivitesForLeadDAO);
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
	
	/*public LeadActivityListWithTypeAheadData getLeadActivityPage(FilterDataList leadFilterDataList, Pageable pageable) throws ParseException 
	{
		log.info("Invoked findFilteredList with payload - " + leadFilterDataList.toString());
		LeadActivityListWithTypeAheadData leadActivityListWithTypeAheadData = new LeadActivityListWithTypeAheadData();
		
		log.info("Fetching filteration based on filter data received");
		Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);
		
		Page<Lead> leadList = spec!=null?lRepo.findAll(spec, pageable):lRepo.findAll(pageable);
		
		//Page<LeadPageData> pagedata=leadToLeadActivityModelMapper.map(leadList, LeadPageData.class);
		//Page<LeadPageData> pagedata = ObjectMapperUtils.mapEntityPageIntoDtoPage(leadList, LeadPageData.class);
		
		Page<LeadPageData> pagedata = leadList.map(objectEntity -> leadToLeadActivityModelMapper.map(objectEntity, LeadPageData.class));
				
		leadActivityListWithTypeAheadData.setLeadPageDetails(pagedata);
		log.info("Setting dropdown data");
		leadActivityListWithTypeAheadData.setDropdownData(populateDropdownService.fetchData("lead"));
		log.info("Setting typeahead data");
		leadActivityListWithTypeAheadData.setTypeAheadDataForGlobalSearch(lService.fetchTypeAheadForLeadGlobalSearch());
		return leadActivityListWithTypeAheadData;
	}
	*/
	public LeadActivityListWithTypeAheadData getLeadActivityPage(FilterDataList leadFilterDataList, Pageable pageable) throws ParseException 
	{
		log.info("Invoked findFilteredList with payload - " + leadFilterDataList.toString());
		LeadActivityListWithTypeAheadData leadActivityListWithTypeAheadData = new LeadActivityListWithTypeAheadData();
		
		log.info("Fetching filteration based on filter data received");
		Specification<LeadActivity> spec = ActivitySpecifications.getSpecification(leadFilterDataList);
		
		Page<LeadActivity> leadActivityList = spec!=null?laRepo.findAll(spec, pageable):laRepo.findAll(pageable);
		
		Page<LeadPageData> pagedata = leadActivityList.map(objectEntity -> leadToLeadActivityModelMapper.map(objectEntity, LeadPageData.class));
				
		leadActivityListWithTypeAheadData.setLeadPageDetails(pagedata);
		log.info("Setting dropdown data");
		leadActivityListWithTypeAheadData.setDropdownData(populateDropdownService.fetchData("lead"));
		log.info("Setting typeahead data");
		leadActivityListWithTypeAheadData.setTypeAheadDataForGlobalSearch(lService.fetchTypeAheadForLeadGlobalSearch());
		return leadActivityListWithTypeAheadData;
	}
	
	public LeadActivity getRecentActivityByLead(Lead lead) {
		List<LeadActivity> activities=laRepo.findAllActivitiesForLead(lead.getLeadId());
		log.info("Get all the Activity");
		System.out.println(lead.getCustomerName());
		System.out.println(activities);
		LeadActivity activity=new LeadActivity();
		try {
			activity = getDisplayActivityForLeadFromAllActivities(activities);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return activity;
	}

	public LeadActivity getRecentActivityByLeadId(Long leadId) 
	{
		LeadActivity activity=new LeadActivity();
		List<LeadActivity> activities=laRepo.findAllActivitiesForLead(leadId);
		log.info("Get all the Activity");
		
		try 
		{activity = getDisplayActivityForLeadFromAllActivities(activities);}
		 
		catch (Exception e) {e.printStackTrace();}
		
		return activity;
	}
	
	public LeadActivity getDisplayActivityForLeadFromAllActivities(List<LeadActivity> activities) throws Exception
	{
		boolean isPendingExists=false;
		//boolean isClosedExists=false;
		boolean isUpcomingExists=false;
		boolean pastExists=false;
		List<LeadActivity> returndata=new ArrayList<LeadActivity>();
		Long leadId = activities.get(0).getLead().getLeadId();
		for(LeadActivity activity:activities) 
		{
			log.info("check if any activity is open");
			System.out.println("Start Of Day - "+ReusableMethods.atStartOfDay(new Date()));
			System.out.println("End Of Day - "+ReusableMethods.atEndOfDay(new Date()));
			
			System.out.println(activity.getActivityDateTime().after(ReusableMethods.atStartOfDay(new Date())));
			
			
			if(activity.getIsOpen()==true && activity.getActivityDateTime().after(ReusableMethods.atStartOfDay(new Date())) && activity.getActivityDateTime().before(ReusableMethods.atEndOfDay(new Date()))) // Pass time zone to constructor.) 
				isPendingExists=true;
				
			if(activity.getIsOpen()==true && activity.getActivityDateTime().after(ReusableMethods.atEndOfDay(new Date()))) // Pass time zone to constructor.) 
				isUpcomingExists=true;
			
			if(activity.getIsOpen()==true && activity.getActivityDateTime().before(ReusableMethods.atStartOfDay(new Date()))) // Pass time zone to constructor.) 
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
			returndata = laRepo.getRecentPendingActivity(leadId,ReusableMethods.atStartOfDay(new Date()),ReusableMethods.atEndOfDay(new Date()));
		
		else if(!isUpcomingExists && !isPendingExists && pastExists) 
			returndata = laRepo.getRecentClosedActivity(leadId);
		
		else if(!pastExists && !isPendingExists && !isUpcomingExists)
			returndata = laRepo.getRecentActivityIrrespectiveOfStatus(leadId);
		
		else if (!isPendingExists)
		{
			if(!pastExists)
				returndata = laRepo.getRecentUpcomingActivity(leadId,ReusableMethods.atEndOfDay(new Date()));
			
			else if(isUpcomingExists && !pastExists)
				returndata = laRepo.getRecentUpcomingActivity(leadId,ReusableMethods.atEndOfDay(new Date()));
			
			else if(isUpcomingExists && pastExists)
				returndata = laRepo.getRecentUpcomingActivity(leadId,ReusableMethods.atEndOfDay(new Date()));
			
			else if(!isUpcomingExists && pastExists)
				returndata = laRepo.getRecentPastActivity(leadId,ReusableMethods.atStartOfDay(new Date()));
		}
		if(returndata.size()==0)
			throw new Exception("No Activity present for lead - "+leadId);
		
		return returndata.get(0);
	}
	
	public Long getStagnantDaysByLeadId(Long leadId) 
	{
		Date lastModified = laRepo.fetchLastModified(leadId);
		try
		{
			long noOfDaysBetween = ReusableMethods.daysBetweenTwoDates(lastModified, new Date());
			return noOfDaysBetween;
		}
		catch(Exception ch) 
		{
			System.out.println("No modified date found. Lead must be closed");
			return (long) 0;
		}
		
		
	}


	public Boolean getRevertable(Long leadActivityId, Long leadId) throws Exception
	{
		List<LeadActivity> activities = laRepo.fetchMostRecentLeadActivity(leadId);
		if(activities.size()<1)
			throw new Exception("No activities found for lead");
		
		if(leadActivityId.equals(activities.get(0).getLeadActivityId()))
			return true;
		else
			return false;
	}
}


