package com.ec.crm.SubClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import com.ec.crm.Data.MapForDashboardStatsDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.LeadActivity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SetLeadGenerated implements Runnable
{
	private CyclicBarrier barrier;
	private PipelineAndActivitiesForDashboard dashboardPipelineReturnData;
	private List<LeadActivity> data;
	Map<Long, String> idNameMap;

	Logger log = LoggerFactory.getLogger(SetLeadGenerated.class);

	public SetLeadGenerated(CyclicBarrier barrier, PipelineAndActivitiesForDashboard dashboardPipelineReturnData,
			List<LeadActivity> data, Map<Long, String> idNameMap)
	{

		this.dashboardPipelineReturnData = dashboardPipelineReturnData;
		this.barrier = barrier;
		this.data = data;
		this.idNameMap = idNameMap;
	}

	@Override
	public void run()
	{

		log.info("Fetching stats for Lead Generated");
		/*MapForPipelineAndActivities obj = new MapForPipelineAndActivities();
		Long total;
		ArrayList<MapForDashboardStatsDAO> detailed;
		HashMap<Long, Integer> idCountMapping = new HashMap<Long, int>();
		for(LeadActivity la : data)
		{
			if(idCountMapping.containsKey(la.getLead().getAsigneeId()))
			{
				Map.Entry<idCountMapping.get(la.getLead().getAsigneeId())
			}

		}*/
		dashboardPipelineReturnData.setLeadGenerated(
				new MapForPipelineAndActivities(data.stream().filter(c -> c.getCreatorId() == 404).count(),
						data.stream().filter(c -> c.getCreatorId() == 404).collect(Collectors.groupingBy(c ->
						{

							try
							{

								return idNameMap.get(c.getLead().getAsigneeId());
							} catch (Exception e)
							{
								// TODO Auto-generated catch block
								log.error(e.getMessage());
								e.printStackTrace();
								return null;
							}

						}, Collectors.counting()))));
		log.info("Completed stats for Lead Generated");
		try
		{
			barrier.await();
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
