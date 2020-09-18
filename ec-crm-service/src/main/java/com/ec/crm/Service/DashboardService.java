package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.DashboardData;
import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Repository.ConvertionRatioRepo;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.StagnantStatsRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DashboardService
{

	@Autowired
	LeadActivityRepo lRepo;

	@Autowired
	StagnantStatsRepo stagnantStatsRepo;

	@Autowired
	ConvertionRatioRepo convertionRatioRepo;

	@Autowired
	UserDetailsService userDetailsService;

	public PipelineAndActivitiesForDashboard customerpipeline(DashboardData payload)
	{
		PipelineAndActivitiesForDashboard dashboardPipelineReturnData = new PipelineAndActivitiesForDashboard();
		List<LeadActivity> data = new ArrayList<LeadActivity>();
		Date fromdate = payload.getFromDate();
		Date todate = payload.getToDate();

		System.out.println("fromdate : " + fromdate + " todate: " + todate);
		data = lRepo.getActivity(fromdate, todate);

		dashboardPipelineReturnData
				.setLeadGenerated(
						new MapForPipelineAndActivities(
								data.stream().filter(
										c -> c.getCreatorId() == 404).count(),
								data.stream().filter(c -> c.getCreatorId() == 404).collect(Collectors.groupingBy(
										c -> userDetailsService.getUserFromId(c.getLead().getAsigneeId()).getUsername(),
										Collectors.counting()))));

		dashboardPipelineReturnData
				.setActivitiesCreated(
						new MapForPipelineAndActivities(
								data.stream().filter(
										c -> c.getCreatorId() != 404).count(),
								data.stream().filter(c -> c.getCreatorId() != 404).collect(Collectors.groupingBy(
										c -> userDetailsService.getUserFromId(c.getLead().getAsigneeId()).getUsername(),
										Collectors.counting()))));

		dashboardPipelineReturnData
				.setTotalPropertyVisit(
						new MapForPipelineAndActivities(
								data.stream().filter(
										c -> c.getActivityType().name() == "Property_Visit").count(),
								data.stream().filter(c -> c.getActivityType().name() == "Property_Visit")
										.collect(Collectors.groupingBy(c -> userDetailsService
												.getUserFromId(c.getLead().getAsigneeId()).getUsername(),
												Collectors.counting()))));

		dashboardPipelineReturnData
				.setDealClosed(
						new MapForPipelineAndActivities(
								data.stream().filter(
										c -> c.getActivityType().name() == "Deal_Close").count(),
								data.stream().filter(c -> c.getActivityType().name() == "Deal_Close")
										.collect(Collectors.groupingBy(c -> userDetailsService
												.getUserFromId(c.getLead().getAsigneeId()).getUsername(),
												Collectors.counting()))));

		dashboardPipelineReturnData
				.setDealLost(
						new MapForPipelineAndActivities(
								data.stream().filter(
										c -> c.getActivityType().name() == "Deal_Lost").count(),
								data.stream().filter(c -> c.getActivityType().name() == "Deal_Lost")
										.collect(Collectors.groupingBy(c -> userDetailsService
												.getUserFromId(c.getLead().getAsigneeId()).getUsername(),
												Collectors.counting()))));

		dashboardPipelineReturnData
				.setTodaysActivities(
						new MapForPipelineAndActivities(
								data.stream().filter(
										c -> c.getIsOpen() == true).count(),
								data.stream().filter(c -> c.getIsOpen() == true).collect(Collectors.groupingBy(
										c -> userDetailsService.getUserFromId(c.getLead().getAsigneeId()).getUsername(),
										Collectors.counting()))));

		dashboardPipelineReturnData
				.setPendingActivities(
						new MapForPipelineAndActivities(
								data.stream().filter(c -> c.getIsOpen() == true).filter(
										c -> c.getActivityDateTime().compareTo(new Date()) < 0).count(),
								data.stream().filter(c -> c.getIsOpen() == true)
										.filter(c -> c.getActivityDateTime().compareTo(new Date()) < 0)
										.collect(Collectors.groupingBy(c -> userDetailsService
												.getUserFromId(c.getLead().getAsigneeId()).getUsername(),
												Collectors.counting()))));

		dashboardPipelineReturnData
				.setUpcomingActivities(
						new MapForPipelineAndActivities(
								data.stream().filter(c -> c.getIsOpen() == true).filter(
										c -> c.getActivityDateTime().compareTo(new Date()) > 0).count(),
								data.stream().filter(c -> c.getIsOpen() == true)
										.filter(c -> c.getActivityDateTime().compareTo(new Date()) > 0)
										.collect(Collectors.groupingBy(c -> userDetailsService
												.getUserFromId(c.getLead().getAsigneeId()).getUsername(),
												Collectors.counting()))));

		return dashboardPipelineReturnData;

	}

	public List<ConversionRatio> conversionratio()
	{
		return convertionRatioRepo.findAll();
	}

	public List<StagnantStats> returnStagnantStats()
	{
		return stagnantStatsRepo.findAll();
	}

	public Map topperformer()
	{ // TODO Auto-generated method stub
		List<ConversionRatio> data = convertionRatioRepo.gettopperformer();
		Map returndata = new HashMap<>();
		Long id = data.get(0).getUserId();
		Long propertyvisit = lRepo.getpropertyvisit(id);
		returndata.put("username", data.get(0).getAsigneeName());
		returndata.put("lead generated", data.get(0).getTotalcount());
		returndata.put("property", propertyvisit);
		returndata.put("deal close", data.get(0).getConvertedcount());
		return returndata;
	}

}
