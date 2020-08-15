package com.ec.crm.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.PipelineAllReturnDAO;
import com.ec.crm.Data.PipelineSingleReturnDTO;
import com.ec.crm.Data.PipelineWithTotalReturnDAO;
import com.ec.crm.Data.PlannerAllReturnDAO;
import com.ec.crm.Data.PlannerSingleReturnDAO;
import com.ec.crm.Data.PlannerWithTotalReturnDAO;
import com.ec.crm.Data.StagnatedEnum;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Filters.ActivitySpecifications;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.LeadSpecifications;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.ReusableClasses.ObjectMapperUtils;
import com.ec.crm.ReusableClasses.ReusableMethods;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AllActivitiesService 
{
	@Autowired
	LeadActivityRepo laRepo;
	
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	@Autowired
	LeadService leadService;
	Logger log = LoggerFactory.getLogger(LeadService.class);
	
	@Autowired
	LeadActivityService leadActivityService;
	
	public PlannerAllReturnDAO findFilteredDataForPlanner(FilterDataList leadFilterDataList, Pageable pageable) throws ParseException 
	{
		log.info("Invoked - findFilteredData");
		List<LeadActivity> activities = new ArrayList<LeadActivity>();
		
		log.info("Fetching filteration based on filter data received");
		Specification<LeadActivity> spec = ActivitySpecifications.getSpecification(leadFilterDataList);
		
		log.info("Specification Fetch Complete");
		if(spec!=null)
			activities = laRepo.findAll(spec);
		else
			activities = laRepo.findAll();
		
		log.info("Transforming Data for return");
		PlannerAllReturnDAO returnData = transformDataToPlannerMode(activities);
		return returnData;
	}

	private PlannerAllReturnDAO transformDataToPlannerMode(List<LeadActivity> activities) 
	{
		log.info("Invoked transformDataToPlannerMode");
		PlannerAllReturnDAO activitiesList = new PlannerAllReturnDAO();
		activitiesList.setCall(fetchPlannerDataFromActivityList(activities,"Call"));
		activitiesList.setMeeting(fetchPlannerDataFromActivityList(activities,"Meeting"));
		activitiesList.setProperty_visit(fetchPlannerDataFromActivityList(activities,"Property_Visit"));
		activitiesList.setDeal_close(fetchPlannerDataFromActivityList(activities,"Deal_Close"));
		activitiesList.setReminder(fetchPlannerDataFromActivityList(activities,"Reminder"));
		activitiesList.setTask(fetchPlannerDataFromActivityList(activities,"Task"));
		activitiesList.setMessage(fetchPlannerDataFromActivityList(activities,"Message"));
		activitiesList.setEmail(fetchPlannerDataFromActivityList(activities,"Email"));
		activitiesList.setDeal_lost(fetchPlannerDataFromActivityList(activities,"Deal_Lost"));
		activitiesList.setDropdownData(populateDropdownService.fetchData("lead"));
		log.info("Setting tyoeahead data");
		activitiesList.setTypeAheadDataForGlobalSearch(leadService.fetchTypeAheadForLeadGlobalSearch());
		return activitiesList;
	}

	private PlannerWithTotalReturnDAO fetchPlannerDataFromActivityList(List<LeadActivity> activities, String acivityType) 
	{
		log.info("Invoked PlannerWithTotalReturnDAO");
		List<LeadActivity> filteredActivities = activities.stream().filter(
				LeadActivity -> LeadActivity.getActivityType().equals
				(ActivityTypeEnum.valueOf(acivityType)))
				.sorted(Comparator.comparing(LeadActivity::getActivityDateTime)).collect(Collectors.toList());
		return transformToPlannerWithTotalReturnDAO(filteredActivities);
	}

	private PlannerWithTotalReturnDAO transformToPlannerWithTotalReturnDAO(List<LeadActivity> filteredActivities) 
	{
		log.info("Invoked transformToPlannerWithTotalReturnDAO");
		PlannerWithTotalReturnDAO plannerWithTotalReturnDAO = new PlannerWithTotalReturnDAO();
		List<PlannerSingleReturnDAO> activities = new ArrayList<PlannerSingleReturnDAO>();
		for(LeadActivity leadActivity:filteredActivities)
			activities.add(ObjectMapperUtils.map(leadActivity, new PlannerSingleReturnDAO()));
		
		plannerWithTotalReturnDAO.setActivities(activities);
		plannerWithTotalReturnDAO.setTotalActivities(activities.size());
		return plannerWithTotalReturnDAO;
	}

		public PipelineAllReturnDAO findFilteredDataForPipeline(FilterDataList leadFilterDataList, Pageable pageable) throws Exception 
		{
			log.info("Invoked - findFilteredDataForPlanner");
			List<Lead> leads = new ArrayList<Lead>();
			
			log.info("Fetching filteration based on filter data received");
			Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);
			
			log.info("Specification Fetch Complete");
			
    	    if(spec!=null)
    	    	leads = lRepo.findAll(spec);
			else
				leads = lRepo.findAll();
			
			log.info("Transforming Data for return");
			PipelineAllReturnDAO returnData = transformDataToPipelineMode(leads);
			return returnData;
		}

		private PipelineAllReturnDAO transformDataToPipelineMode(List<Lead> leads) throws Exception 
		{
			PipelineAllReturnDAO pipelineAllReturnDAO = new PipelineAllReturnDAO();
			pipelineAllReturnDAO.setDropdownData(populateDropdownService.fetchData("lead"));
			pipelineAllReturnDAO.setTypeAheadDataForGlobalSearch(leadService.fetchTypeAheadForLeadGlobalSearch());
			pipelineAllReturnDAO.setLeadGeneration(fetchPipelineDataFromActivityList(leads,LeadStatusEnum.New_Lead));
			pipelineAllReturnDAO.setNegotiation(fetchPipelineDataFromActivityList(leads,LeadStatusEnum.Negotiation));
			pipelineAllReturnDAO.setPropertyVisit(fetchPipelineDataFromActivityList(leads,LeadStatusEnum.Property_Visit));
			pipelineAllReturnDAO.setDeal_close(fetchPipelineDataFromActivityList(leads,LeadStatusEnum.Deal_Closed));
			return pipelineAllReturnDAO;
		}

		private PipelineWithTotalReturnDAO fetchPipelineDataFromActivityList(List<Lead> leads,
				LeadStatusEnum leadStatus) throws Exception 
		{
			List<Lead> filteredLeads = leads.stream().filter(
					Lead -> Lead.getStatus().equals(leadStatus))
					.collect(Collectors.toList());
			return transformToPipelineWithTotalReturnDAO(filteredLeads);
		}

		private PipelineWithTotalReturnDAO transformToPipelineWithTotalReturnDAO(List<Lead> filteredLeads) throws Exception 
		{
			PipelineWithTotalReturnDAO PipelineWithTotalReturnDAO = new PipelineWithTotalReturnDAO();
			List<PipelineSingleReturnDTO> pipelineSingleReturnDTOList = new ArrayList<PipelineSingleReturnDTO>();
			
			for(Lead l : filteredLeads)
			{
				PipelineSingleReturnDTO pipelineSingleReturnDTO = new PipelineSingleReturnDTO();
				pipelineSingleReturnDTO.setLeadId(l.getLeadId());
				pipelineSingleReturnDTO.setMobileNumber(l.getPrimaryMobile());
				pipelineSingleReturnDTO.setName(l.getCustomerName());
				pipelineSingleReturnDTO.setSentiment(l.getSentiment());
				pipelineSingleReturnDTO.setActivityDateTime(leadActivityService.getRecentActivityByLeadID(l).getActivityDateTime());
				pipelineSingleReturnDTO.setStagnantStatus(getStagnantStatus(l.getLeadId()));
				pipelineSingleReturnDTOList.add(pipelineSingleReturnDTO);
			}
			PipelineWithTotalReturnDAO.setLeads(pipelineSingleReturnDTOList);
			PipelineWithTotalReturnDAO.setTotalCount(filteredLeads.size());
			return PipelineWithTotalReturnDAO;
		}

		private StagnatedEnum getStagnantStatus(Long leadId) throws Exception 
		{
			log.info("Invoked getStagnantStatus");
			StagnatedEnum stagnatedStatus = StagnatedEnum.NoColor;
			Map<Long,Date> stagnantDays=leadActivityService.getStagnantDayCount();
			Date lastActivityDate = stagnantDays.get(leadId);
			if(lastActivityDate==null)
				throw new Exception("last updated tome for lead not found");
			
			long noOfDay = ReusableMethods.daysBetweenTwoDates(lastActivityDate, new Date());
			if (noOfDay>=10 && noOfDay<20)
				stagnatedStatus = StagnatedEnum.GREEN;
			else if (noOfDay >=20 && noOfDay <30)
				stagnatedStatus = StagnatedEnum.ORANGE;
			else if (noOfDay >=30)
				stagnatedStatus = StagnatedEnum.RED;
			return stagnatedStatus;
		}
}

