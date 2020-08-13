package com.ec.crm.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.PlannerAllReturnDAO;
import com.ec.crm.Data.PlannerSingleReturnDAO;
import com.ec.crm.Data.PlannerWithTotalReturnDAO;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Filters.ActivitySpecifications;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Repository.LeadActivityRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AllActivitiesService 
{
	@Autowired
	LeadActivityRepo laRepo;
	
	Logger log = LoggerFactory.getLogger(LeadService.class);

	public PlannerAllReturnDAO findFilteredData(FilterDataList leadFilterDataList, Pageable pageable) throws ParseException 
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
		activitiesList.setCall(fetchDataFromList(activities,"Call"));
		activitiesList.setMeeting(fetchDataFromList(activities,"Meeting"));
		activitiesList.setProperty_visit(fetchDataFromList(activities,"Property_Visit"));
		activitiesList.setDeal_close(fetchDataFromList(activities,"Deal_Close"));
		activitiesList.setReminder(fetchDataFromList(activities,"Reminder"));
		activitiesList.setTask(fetchDataFromList(activities,"Task"));
		activitiesList.setMessage(fetchDataFromList(activities,"Message"));
		activitiesList.setEmail(fetchDataFromList(activities,"Email"));
		activitiesList.setDeal_lost(fetchDataFromList(activities,"Deal_Lost"));
		return activitiesList;
	}

	private PlannerWithTotalReturnDAO fetchDataFromList(List<LeadActivity> activities, String acivityType) 
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
			activities.add(new PlannerSingleReturnDAO(
					leadActivity.getLead().getLeadId(),
					leadActivity.getLead().getCustomerName(),
					leadActivity.getLead().getPrimaryMobile(),
					leadActivity.getIsOpen(),
					leadActivity.getActivityDateTime()));
		
		plannerWithTotalReturnDAO.setActivities(activities);
		plannerWithTotalReturnDAO.setTotalActivities(activities.size());
		return plannerWithTotalReturnDAO;
	}
}

