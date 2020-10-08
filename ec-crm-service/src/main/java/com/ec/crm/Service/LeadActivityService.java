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
import com.ec.crm.Data.FileInformationDAO;
import com.ec.crm.Data.LeadActivityCreate;
import com.ec.crm.Data.LeadActivityListWithTypeAheadData;
import com.ec.crm.Data.LeadPageData;
import com.ec.crm.Data.NoteCreateData;
import com.ec.crm.Data.RescheduleActivityData;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Enums.StagnantDropdownValues;
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
public class LeadActivityService
{
	@Autowired
	NoteService noteService;

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

	Logger log = LoggerFactory.getLogger(LeadService.class);

	@Autowired
	private LeadActivityMapper mapper;

	@Transactional
	public LeadActivity createLeadActivity(LeadActivityCreate payload) throws Exception
	{
		log.info("Invoked createLeadActivity");
		Lead lead = validatePayloadAndReturnLead(payload);
		log.info("Checking ifexit condition exists");
		exitCreateIfExitConditionsExists(lead, payload);
		log.info("Fetching current user from gateway");
		LeadActivity leadActivity = new LeadActivity();
		setFields(leadActivity, payload, "user");
		log.info("Closed createLeadActivity");
		laRepo.save(leadActivity);
		ExecuteBusinessLogicWhileCreation(leadActivity);
		return leadActivity;

	}

	@Transactional
	public void revertLeadActivity(Long id) throws Exception
	{

		LeadActivity la = getSingleLeadActivity(id);
		exitIfRevertNotAllowed(la);

		if (la.getIsOpen())
		{
			if (la.getActivityType().equals(ActivityTypeEnum.Property_Visit))
			{
				addNoteBeforeRevert(la);
				la.getLead().setStatus(LeadStatusEnum.New_Lead);
				laRepo.save(la);
				laRepo.softDelete(la);
			}
		} else
		{
			if (la.getActivityType().equals(ActivityTypeEnum.Property_Visit))
			{
				addNoteBeforeRevert(la);
				la.getLead().setStatus(LeadStatusEnum.Property_Visit);
				la.setClosedBy(null);
				la.setClosingComment(null);
				la.setIsOpen(true);
				laRepo.save(la);
			} else if (la.getActivityType().equals(ActivityTypeEnum.Deal_Close))
			{
				addNoteBeforeRevert(la);
				la.getLead().setStatus(fetchPreviousStatusFromHistory(la.getLead()));
				laRepo.save(la);
				laRepo.softDelete(la);

			} else if (la.getActivityType().equals(ActivityTypeEnum.Deal_Lost))
			{
				addNoteBeforeRevert(la);
				la.getLead().setStatus(fetchPreviousStatusFromHistory(la.getLead()));
				laRepo.save(la);
				laRepo.softDelete(la);
			}
		}
		// TO DO - Add logic to revert the activity
	}

	@Transactional
	private void addNoteBeforeRevert(LeadActivity la) throws Exception
	{
		NoteCreateData noteCreateData = new NoteCreateData();
		List<FileInformationDAO> fileInformations = new ArrayList<FileInformationDAO>();
		String content;
		content = "ACTIVITY REVERTED - " + System.lineSeparator() + "Type - " + la.getActivityType()
				+ System.lineSeparator() + "Reverted By - " + userDetailsService.getCurrentUser().getUsername()
				+ System.lineSeparator() + "Title - " + la.getTitle();
		noteCreateData.setLeadId(la.getLead().getLeadId());
		noteCreateData.setContent(content);
		noteCreateData.setPinned(false);
		noteCreateData.setFileInformations(fileInformations);
		noteService.createNote(noteCreateData);
	}

	@Transactional
	private void exitIfRevertNotAllowed(LeadActivity la) throws Exception
	{
		List<LeadActivity> activities = laRepo.fetchMostRecentLeadActivity(la.getLead().getLeadId());
		if (activities.size() < 1)
			throw new Exception("No activities found for lead");

		LeadActivity latestActivity = activities.get(0);

		if (!latestActivity.getLeadActivityId().equals(la.getLeadActivityId()))
			throw new Exception("Revert not allowed for this activity.");

		else
		{
			if (latestActivity.getIsOpen())
			{
				if (!latestActivity.getActivityType().equals(ActivityTypeEnum.Property_Visit))
					throw new Exception("Revert not allowed for this activity.");
			} else if (!latestActivity.getActivityType().equals(ActivityTypeEnum.Deal_Lost)
					&& !latestActivity.getActivityType().equals(ActivityTypeEnum.Deal_Close)
					&& !latestActivity.getActivityType().equals(ActivityTypeEnum.Property_Visit))

				throw new Exception("Revert not allowed for this activity.");
		}
	}

	@Transactional
	private void ExecuteBusinessLogicWhileCreation(LeadActivity leadActivity) throws Exception
	{

		log.info("Invoked ExecuteBusinessLogicWhileCreation");
		LeadStatusEnum status = leadActivity.getLead().getStatus();
		Long currentUserId = userDetailsService.getCurrentUser().getId();
		if (status.equals(LeadStatusEnum.New_Lead))
		{
			if (leadActivity.getActivityType().equals(ActivityTypeEnum.Property_Visit))
				leadActivity.getLead().setStatus(LeadStatusEnum.Property_Visit);
			else if (leadActivity.getActivityType().equals(ActivityTypeEnum.Deal_Lost))
			{
				leadActivity.getLead().setStatus(LeadStatusEnum.Deal_Lost);
				leadActivity.setClosedBy(currentUserId);
				leadActivity.setClosingComment("Deal Lost");
				closeAllOpenActivitiesForLead(leadActivity.getLead());
			}
		}

		if (status.equals(LeadStatusEnum.Property_Visit))
		{
			if (leadActivity.getActivityType().equals(ActivityTypeEnum.Deal_Lost))
			{
				leadActivity.getLead().setStatus(LeadStatusEnum.Deal_Lost);
				leadActivity.setClosedBy(currentUserId);
				leadActivity.setClosingComment("Deal Lost");
				closeAllOpenActivitiesForLead(leadActivity.getLead());
			}
		}

		if (status.equals(LeadStatusEnum.Negotiation))
		{
			if (leadActivity.getActivityType().equals(ActivityTypeEnum.Deal_Lost))
			{
				leadActivity.getLead().setStatus(LeadStatusEnum.Deal_Lost);
				leadActivity.setClosedBy(currentUserId);
				leadActivity.setClosingComment("Deal Lost");
				closeAllOpenActivitiesForLead(leadActivity.getLead());
			} else if (leadActivity.getActivityType().equals(ActivityTypeEnum.Deal_Close))
			{
				leadActivity.getLead().setStatus(LeadStatusEnum.Deal_Closed);
				leadActivity.setClosedBy(currentUserId);
				leadActivity.setClosingComment("Deal Closed");
				closeAllOpenActivitiesForLead(leadActivity.getLead());
			}
		}

		if (status.equals(LeadStatusEnum.Deal_Closed) || status.equals(LeadStatusEnum.Deal_Lost))
			if (leadActivity.getActivityType().equals(ActivityTypeEnum.Meeting))
				leadActivity.getLead().setStatus(fetchPreviousStatusFromHistory(leadActivity.getLead()));
		laRepo.save(leadActivity);
	}

	@Transactional
	private void ExecuteBusinessLogicWhileClosure(LeadActivity leadActivity, Boolean isReschedule) throws Exception
	{
		log.info("Invoked ExecuteBusinessLogicWhileClosure");
		LeadStatusEnum status = leadActivity.getLead().getStatus();

		if (status.equals(LeadStatusEnum.Property_Visit) && isReschedule.equals(false))
		{
			if (leadActivity.getActivityType().equals(ActivityTypeEnum.Property_Visit))
			{
				log.info("Changing status of lead from Property_Visit - Negotiation");
				leadActivity.getLead().setStatus(LeadStatusEnum.Negotiation);
			}
		}
		laRepo.save(leadActivity);
	}

	private LeadStatusEnum fetchPreviousStatusFromHistory(Lead lead) throws Exception
	{
		log.info("Fetching previous status for lead -" + lead.getLeadId());
		List<Lead> leadHistory = lService.history(lead.getLeadId());
		LeadStatusEnum previousStatus = lead.getStatus();

		if (leadHistory.size() < 1)
			throw new Exception("No history found for lead");

		for (int ctr = leadHistory.size() - 1; ctr >= 0; ctr--)
		{
			Lead currentLead = leadHistory.get(ctr);
			if (currentLead.getStatus().equals(lead.getStatus()) == false)
			{
				previousStatus = currentLead.getStatus();
				log.info("Previous status found for lead - " + currentLead.getLeadId() + " - " + previousStatus);
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
		for (LeadActivity activity : activities)
		{
			activity.setClosedBy((long) 404);
			activity.setClosingComment("Auto-Closed due to business logic");
			activity.setIsOpen(false);
			laRepo.save(activity);
		}
	}

	private void exitCreateIfExitConditionsExists(Lead lead, LeadActivityCreate payload) throws Exception
	{
		log.info("Invoked exitIfExitConditionsExists");
		if (lead.getStatus().equals(LeadStatusEnum.New_Lead) || lead.getStatus().equals(LeadStatusEnum.Property_Visit))
		{
			if (payload.getActivityType().equals(ActivityTypeEnum.Deal_Close))
				throw new Exception("Deal close activity not allowed for lead in stage " + lead.getStatus());
		} else if (lead.getStatus().equals(LeadStatusEnum.Deal_Closed)
				|| lead.getStatus().equals(LeadStatusEnum.Deal_Lost))
		{
			if (!payload.getActivityType().equals(ActivityTypeEnum.Meeting))
				throw new Exception(
						"Only activity of type Meeting can be created if a lead is in stage " + lead.getStatus());
		}

		List<LeadActivity> existingActivities = laRepo.findByLeadActivityTypeOpen(payload.getLeadId(),
				payload.getActivityType());
		if (existingActivities.size() > 0)
			throw new Exception("Open activity of type " + payload.getActivityType() + " already exist for lead.");

		if (payload.getActivityDateTime().before(ReusableMethods.atStartOfDay(new Date())))
			throw new Exception("Cannot create activity for past date.");

	}

	private void setFields(LeadActivity leadActivity, LeadActivityCreate payload, String creatorType)
	{
		Long currentUserId = userDetailsService.getCurrentUser().getId();
		log.info("Invoked setFields");
		leadActivity.setActivityDateTime(payload.getActivityDateTime());
		leadActivity.setActivityType(payload.getActivityType());
		leadActivity.setCreatorId(creatorType == "user" ? currentUserId : 404);
		leadActivity.setDescription(payload.getDescription());
		leadActivity.setTitle(payload.getTitle());
		leadActivity.setTags(payload.getTags());
		leadActivity.setLead(lRepo.findById(payload.getLeadId()).get());
		leadActivity.setDuration(payload.getDuration() == null ? 0 : payload.getDuration());

		if (payload.getActivityType().equals(ActivityTypeEnum.Deal_Lost)
				|| payload.getActivityType().equals(ActivityTypeEnum.Deal_Close))
			leadActivity.setIsOpen(false);
		else
			leadActivity.setIsOpen(true);
	}

	private Lead validatePayloadAndReturnLead(LeadActivityCreate payload) throws Exception
	{
		log.info("Validating LeadId from payload - " + payload.getLeadId());
		String errorMessage = "";
		if (payload.getActivityType() == null)
			errorMessage = errorMessage == "" ? " Activity Type, " : errorMessage + " Activity Type, ";
		if (payload.getLeadId() == null)
			errorMessage = errorMessage == "" ? " Lead ID, " : errorMessage + " Lead ID, ";
		if (payload.getActivityDateTime() == null)
			errorMessage = errorMessage == "" ? " Activity Date & Time, " : errorMessage + " Activity Date & Time, ";
		if (payload.getTitle() == null || payload.getTitle() == "")
			errorMessage = errorMessage == "" ? " Title, " : errorMessage + " itle, ";

		if (errorMessage != "")
			throw new Exception("Fields Missing - " + errorMessage);

		Optional<Lead> leadOpt = lRepo.findById(payload.getLeadId());

		if (!leadOpt.isPresent())
			throw new Exception("Lead not found by lead ID -" + payload.getLeadId());
		else
			return leadOpt.get();
		/*
		 * else if((leadOpt.get().getStatus().equals(LeadStatusEnum.New_Lead) ||
		 * leadOpt.get().getStatus().equals(LeadStatusEnum.Deal_Lost)) &&
		 * payload.getActivityType().equals(ActivityTypeEnum.Deal_Close)) throw new
		 * Exception("You cannot close a new lead or a lost lead. Lead should be closed only after property visit"
		 * );
		 */
	}

	public Page<LeadActivity> fetchAll(Pageable pageable)
	{
		return laRepo.findAll(pageable);
	}

	public LeadActivity getSingleLeadActivity(long id) throws Exception
	{
		log.info("Invoked activityType");
		Optional<LeadActivity> latype = laRepo.findById(id);
		if (latype.isPresent())
			return latype.get();
		else
			throw new Exception("LeadActivity ID not found");
	}

	@Transactional
	public void deleteLeadActivity(Long id, String closingComment, Long closedBy, Boolean isReschedule) throws Exception
	{
		log.info("Invoked deleteLeadActivity");
		Optional<LeadActivity> latype = laRepo.findById(id);
		if (!latype.isPresent())
			throw new Exception("LeadActivity ID not found");

		LeadActivity leadActivity = latype.get();

		if (leadActivity.getIsOpen() == false)
			throw new Exception("Activity alread closed with comment - " + leadActivity.getClosingComment());

		leadActivity.setIsOpen(false);
		leadActivity.setClosingComment(closingComment);
		leadActivity.setClosedBy(closedBy);
		laRepo.save(leadActivity);
		ExecuteBusinessLogicWhileClosure(leadActivity, isReschedule);
	}

	@Transactional
	public void rescheduleActivity(Long id, RescheduleActivityData rescheduleActivityData) throws Exception
	{
		log.info("Invoked rescheduleActivity");
		Long currentUserId = userDetailsService.getCurrentUser().getId();
		LeadActivity leadActivity = validateReschedulePayloadAndReturnLeadActivity(rescheduleActivityData, id);
		currentUserId = userDetailsService.getCurrentUser().getId();

		// Delete old activity
		log.info("Deleting old Activity");
		deleteLeadActivity(leadActivity.getLeadActivityId(), rescheduleActivityData.getClosingComment(), currentUserId,
				true);
		log.info("Deleted old Activity - success");

		// Create new Activity
		log.info("creating new Activity - success");
		LeadActivity newActivity = new LeadActivity();
		setFieldsForReschedule(rescheduleActivityData, newActivity, leadActivity);
		laRepo.save(newActivity);
	}

	private LeadActivity validateReschedulePayloadAndReturnLeadActivity(RescheduleActivityData rescheduleActivityData,
			Long id) throws Exception
	{
		log.info("Invoked validateReschedulePayloadAndReturnLeadActivity");
		if (rescheduleActivityData.getRescheduleDateTime() == null)
		{
			log.info("Invalid or Null date and time");
			throw new Exception("Enter valid date and time for rescheduling");
		}

		if (rescheduleActivityData.getRescheduleDateTime().before(new Date()))
		{
			log.info("Reschedule date is past date and time");
			throw new Exception("Cannot reschedule activity to past date and time.");
		}

		if (rescheduleActivityData.getClosingComment() == null || rescheduleActivityData.getClosingComment().equals(""))
		{
			log.info("Invalid or Null closing comment");
			throw new Exception("Enter valid closing comment");
		}

		Optional<LeadActivity> latype = laRepo.findById(id);
		if (!latype.isPresent())
		{
			log.info("Lead Activity with ID - " + id + " not found");
			throw new Exception("LeadActivity ID not found");
		}

		if (latype.get().getIsOpen() == false)
			throw new Exception(
					"Lead Activity already closed with comment -" + latype.get().getClosingComment() == null ? ""
							: latype.get().getClosingComment());
		log.info("Payload validation passed. Returning leacactvity back");
		return latype.get();
	}

	private void setFieldsForReschedule(RescheduleActivityData rescheduleActivityData, LeadActivity newActivity,
			LeadActivity leadActivity)
	{
		Long currentUserId = userDetailsService.getCurrentUser().getId();
		log.info("Invoked setFieldsForReschedule");
		List<String> newTags = new ArrayList<String>();
		for (String tag : leadActivity.getTags())
			newTags.add(tag);

		newActivity.setActivityDateTime(rescheduleActivityData.getRescheduleDateTime());
		newActivity.setActivityType(leadActivity.getActivityType());
		newActivity.setCreatorId(currentUserId);
		if (leadActivity.getDescription() != null)
		{
			if (leadActivity.getDescription().equals("Default activity created for new Lead"))
				newActivity.setDescription("Call activity rescheduled from default call activity");
			else
				newActivity.setDescription(leadActivity.getDescription());
		}
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
		allActivitesForLeadDAO.setPendingActivities(
				laMapper.mapLeadActivitiesToDTOs(laRepo.fetchPendingActivitiesForLead(lead.getLeadId())));
		allActivitesForLeadDAO.setPastActivities(
				laMapper.mapLeadActivitiesToDTOs(laRepo.fetchPastActivitiesForLead(lead.getLeadId())));
		return (allActivitesForLeadDAO);
	}

	public void createDefaultActivity(Long leadId) throws Exception
	{
		log.info("Invoked createDefaultActivity for leadId -" + leadId);
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
		if (!leadOpt.isPresent())
		{
			log.error("Lead with ID not found");
			throw new Exception("Lead with ID not found");
		}
		return leadOpt.get();
	}

	/*
	 * public LeadActivityListWithTypeAheadData getLeadActivityPage(FilterDataList
	 * leadFilterDataList, Pageable pageable) throws ParseException {
	 * log.info("Invoked findFilteredList with payload - " +
	 * leadFilterDataList.toString()); LeadActivityListWithTypeAheadData
	 * leadActivityListWithTypeAheadData = new LeadActivityListWithTypeAheadData();
	 * 
	 * log.info("Fetching filteration based on filter data received");
	 * Specification<Lead> spec =
	 * LeadSpecifications.getSpecification(leadFilterDataList);
	 * 
	 * Page<Lead> leadList = spec!=null?lRepo.findAll(spec,
	 * pageable):lRepo.findAll(pageable);
	 * 
	 * //Page<LeadPageData> pagedata=leadToLeadActivityModelMapper.map(leadList,
	 * LeadPageData.class); //Page<LeadPageData> pagedata =
	 * ObjectMapperUtils.mapEntityPageIntoDtoPage(leadList, LeadPageData.class);
	 * 
	 * Page<LeadPageData> pagedata = leadList.map(objectEntity ->
	 * leadToLeadActivityModelMapper.map(objectEntity, LeadPageData.class));
	 * 
	 * leadActivityListWithTypeAheadData.setLeadPageDetails(pagedata);
	 * log.info("Setting dropdown data");
	 * leadActivityListWithTypeAheadData.setDropdownData(populateDropdownService.
	 * fetchData("lead")); log.info("Setting typeahead data");
	 * leadActivityListWithTypeAheadData.setTypeAheadDataForGlobalSearch(lService.
	 * fetchTypeAheadForLeadGlobalSearch()); return
	 * leadActivityListWithTypeAheadData; }
	 */
	public LeadActivityListWithTypeAheadData getLeadActivityPage(FilterDataList leadFilterDataList, Pageable pageable)
			throws ParseException
	{
		log.info("Invoked findFilteredList with payload - " + leadFilterDataList.toString());
		LeadActivityListWithTypeAheadData leadActivityListWithTypeAheadData = new LeadActivityListWithTypeAheadData();

		log.info("Fetching filteration based on filter data received");
		Specification<LeadActivity> spec = ActivitySpecifications.getSpecification(leadFilterDataList);

		Page<LeadActivity> leadActivityList = spec != null ? laRepo.findAll(spec, pageable) : laRepo.findAll(pageable);

		Page<LeadPageData> pagedata = leadActivityList
				.map(objectEntity -> leadToLeadActivityModelMapper.map(objectEntity, LeadPageData.class));

		leadActivityListWithTypeAheadData.setLeadPageDetails(pagedata);
		log.info("Setting dropdown data");
		leadActivityListWithTypeAheadData.setDropdownData(populateDropdownService.fetchData("lead"));
		log.info("Setting typeahead data");
		leadActivityListWithTypeAheadData.setTypeAheadDataForGlobalSearch(lService.fetchTypeAheadForLeadGlobalSearch());
		leadActivityListWithTypeAheadData.setKeyValueForStagnantDropdown(StagnantDropdownValues.getKeyValue());
		return leadActivityListWithTypeAheadData;
	}

	public LeadActivity getRecentActivityByLead(Lead lead)
	{
		List<LeadActivity> activities = laRepo.findAllActivitiesForLead(lead.getLeadId());
		log.info("Get all the Activity");
		System.out.println(lead.getCustomerName());
		System.out.println(activities);
		LeadActivity activity = new LeadActivity();
		try
		{
			activity = getDisplayActivityForLeadFromAllActivities(activities);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return activity;
	}

	public LeadActivity getRecentActivityByLeadId(Long leadId)
	{
		LeadActivity activity = new LeadActivity();
		List<LeadActivity> activities = laRepo.findAllActivitiesForLead(leadId);
		log.info("Get all the Activity");

		try
		{
			activity = getDisplayActivityForLeadFromAllActivities(activities);
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}

		return activity;
	}

	public LeadActivity getDisplayActivityForLeadFromAllActivities(List<LeadActivity> activities) throws Exception
	{
		boolean isPendingExists = false;
		// boolean isClosedExists=false;
		boolean isUpcomingExists = false;
		boolean pastExists = false;
		List<LeadActivity> returndata = new ArrayList<LeadActivity>();
		Long leadId = activities.get(0).getLead().getLeadId();
		for (LeadActivity activity : activities)
		{
			log.info("check if any activity is open");
			System.out.println("Start Of Day - " + ReusableMethods.atStartOfDay(new Date()));
			System.out.println("End Of Day - " + ReusableMethods.atEndOfDay(new Date()));

			System.out.println(activity.getActivityDateTime().after(ReusableMethods.atStartOfDay(new Date())));

			if (activity.getIsOpen() == true
					&& activity.getActivityDateTime().after(ReusableMethods.atStartOfDay(new Date()))
					&& activity.getActivityDateTime().before(ReusableMethods.atEndOfDay(new Date()))) // Pass time zone
																										// to
																										// constructor.)
				isPendingExists = true;

			if (activity.getIsOpen() == true
					&& activity.getActivityDateTime().after(ReusableMethods.atEndOfDay(new Date()))) // Pass time zone
																										// to
																										// constructor.)
				isUpcomingExists = true;

			if (activity.getIsOpen() == true
					&& activity.getActivityDateTime().before(ReusableMethods.atStartOfDay(new Date()))) // Pass time
																										// zone to
																										// constructor.)
				pastExists = true;
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

		if (isPendingExists)
			returndata = laRepo.getRecentPendingActivity(leadId, ReusableMethods.atStartOfDay(new Date()),
					ReusableMethods.atEndOfDay(new Date()));

		else if (!isUpcomingExists && !isPendingExists && pastExists)
			returndata = laRepo.getRecentClosedActivity(leadId);

		else if (!pastExists && !isPendingExists && !isUpcomingExists)
			returndata = laRepo.getRecentActivityIrrespectiveOfStatus(leadId);

		else if (!isPendingExists)
		{
			if (!pastExists)
				returndata = laRepo.getRecentUpcomingActivity(leadId, ReusableMethods.atEndOfDay(new Date()));

			else if (isUpcomingExists && !pastExists)
				returndata = laRepo.getRecentUpcomingActivity(leadId, ReusableMethods.atEndOfDay(new Date()));

			else if (isUpcomingExists && pastExists)
				returndata = laRepo.getRecentUpcomingActivity(leadId, ReusableMethods.atEndOfDay(new Date()));

			else if (!isUpcomingExists && pastExists)
				returndata = laRepo.getRecentPastActivity(leadId, ReusableMethods.atStartOfDay(new Date()));
		}
		if (returndata.size() == 0)
			throw new Exception("No Activity present for lead - " + leadId);

		return returndata.get(0);
	}

	public Long getStagnantDaysByLeadId(Long leadId)
	{
		Date lastModified = laRepo.fetchLastModified(leadId);
		try
		{
			long noOfDaysBetween = ReusableMethods.daysBetweenTwoDates(lastModified, new Date());
			return noOfDaysBetween;
		} catch (Exception ch)
		{
			System.out.println("No modified date found. Lead must be closed");
			return (long) 0;

		}

	}

	public Boolean getRevertable(Long leadActivityId, Long leadId) throws Exception
	{
		List<LeadActivity> activities = laRepo.fetchMostRecentLeadActivity(leadId);
		if (activities.size() < 1)
			throw new Exception("No activities found for lead");
		LeadActivity activity = activities.get(0);
		if (leadActivityId.equals(activity.getLeadActivityId()))
		{
			if (activity.getIsOpen())
			{
				if (activity.getActivityType().equals(ActivityTypeEnum.Property_Visit))
					return true;
			} else if (activity.getActivityType().equals(ActivityTypeEnum.Deal_Lost)
					|| activity.getActivityType().equals(ActivityTypeEnum.Deal_Close)
					|| activity.getActivityType().equals(ActivityTypeEnum.Property_Visit))

				return true;
		}
		return false;
	}
}