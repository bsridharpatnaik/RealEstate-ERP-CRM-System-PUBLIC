package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.DashboardData;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Repository.ConvertionRatioRepo;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.StagnantStatsRepo;
import com.ec.crm.ReusableClasses.ReusableMethods;
import com.ec.crm.SubClasses.SetActivitiesCreated;
import com.ec.crm.SubClasses.SetDealClosed;
import com.ec.crm.SubClasses.SetDealLost;
import com.ec.crm.SubClasses.SetLeadGenerated;
import com.ec.crm.SubClasses.SetPendingActivities;
import com.ec.crm.SubClasses.SetPropertyVisit;
import com.ec.crm.SubClasses.SetTodaysActivities;
import com.ec.crm.SubClasses.SetUpcomingActivities;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class DashboardService
{

	@Autowired
	LeadActivityRepo lRepo;

	@Autowired
	StagnantStatsRepo stagnantStatsRepo;

	@Autowired
	ConvertionRatioRepo convertionRatioRepo;

	@Resource
	private Map<Long, String> userIdNameMap;

	Logger log = LoggerFactory.getLogger(DashboardService.class);

	public PipelineAndActivitiesForDashboard customerpipeline(DashboardData payload) throws Exception
	{
		PipelineAndActivitiesForDashboard dashboardPipelineReturnData = new PipelineAndActivitiesForDashboard();
		List<LeadActivity> data = new ArrayList<LeadActivity>();
		Date fromdate = payload.getFromDate();
		Date todate = payload.getToDate();
		Date nextDate = DateUtils.addDays(todate, 1);
		log.info("fromdate : " + fromdate + " todate: " + nextDate);
		data = lRepo.getActivity(fromdate, nextDate);

		log.info("Fetching Stats");
		ExecutorService executors = Executors.newFixedThreadPool(8);
		CyclicBarrier barrier = new CyclicBarrier(8);

		// executors.
		executors.submit(new SetLeadGenerated(barrier, dashboardPipelineReturnData, data, userIdNameMap));
		/*executors.submit(new SetActivitiesCreated(barrier, dashboardPipelineReturnData, data, userIdNameMap));
		executors.submit(new SetPropertyVisit(barrier, dashboardPipelineReturnData, data, userIdNameMap));
		executors.submit(new SetDealClosed(barrier, dashboardPipelineReturnData, data, userIdNameMap));
		executors.submit(new SetDealLost(barrier, dashboardPipelineReturnData, data, userIdNameMap));
		executors.submit(new SetTodaysActivities(barrier, dashboardPipelineReturnData, data, userIdNameMap));
		executors.submit(new SetPendingActivities(barrier, dashboardPipelineReturnData, data, userIdNameMap));
		executors.submit(new SetUpcomingActivities(barrier, dashboardPipelineReturnData, data, userIdNameMap));*/

		boolean flag = false;
		Date returnDateTime = new Date();
		while (flag == false)
		{
			if ((dashboardPipelineReturnData.getActivitiesCreated() == null
					/*|| dashboardPipelineReturnData.getActivitiesCreated() == null
					|| dashboardPipelineReturnData.getDealClosed() == null
					|| dashboardPipelineReturnData.getDealLost() == null
					|| dashboardPipelineReturnData.getLeadGenerated() == null
					|| dashboardPipelineReturnData.getPendingActivities() == null
					|| dashboardPipelineReturnData.getTodaysActivities() == null
					|| dashboardPipelineReturnData.getTotalPropertyVisit() == null
					|| dashboardPipelineReturnData.getUpcomingActivities() == null*/)
					&& (java.lang.Math.abs(returnDateTime.getTime() - new Date().getTime()) / 1000 < 30))
			{
				log.info("Waiting for flag to be true. Current difference in time - "
						+ (returnDateTime.getTime() - new Date().getTime()) / 1000);
				flag = false;
			} else
			{
				log.info("Flag is true. Current difference in time - "
						+ (returnDateTime.getTime() - new Date().getTime()) / 1000);
				flag = true;
			}

		}
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
		if(data.size()>0) {
			Long id = data.get(0).getUserId();
			Long propertyvisit = lRepo.getpropertyvisit(id, ReusableMethods.getStartOfMonth());
			returndata.put("username", data.get(0).getAsigneeName());
			returndata.put("leadsGenerated", data.get(0).getTotalcount());
			returndata.put("propertyVisits", propertyvisit);
			returndata.put("dealsClosed", data.get(0).getConvertedcount());
			returndata.put("inNegotiation", 2);
		}
		return returndata;
	}

}