package com.ec.crm.SubClasses;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.LeadActivity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SetPropertyVisit implements Runnable
{
	private CyclicBarrier barrier;
	private PipelineAndActivitiesForDashboard dashboardPipelineReturnData;
	private List<LeadActivity> data;
	Map<Long, String> idNameMap;

	Logger log = LoggerFactory.getLogger(SetPropertyVisit.class);

	public SetPropertyVisit(CyclicBarrier barrier, PipelineAndActivitiesForDashboard dashboardPipelineReturnData,
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

		log.info("Fetching stats for SetPropertyVisit");
		dashboardPipelineReturnData.setTotalPropertyVisit(new MapForPipelineAndActivities(
				data.stream().filter(c -> c.getActivityType().name() == "Property_Visit").count(), data.stream()
						.filter(c -> c.getActivityType().name() == "Property_Visit").collect(Collectors.groupingBy(c ->
						{
							try
							{
								return idNameMap.get(c.getLead().getAsigneeId());
							} catch (Exception e)
							{ // TODO Auto-generated catch block
								log.error(e.getMessage());
								e.printStackTrace();
							}
							return null;
						}, Collectors.counting()))));
		log.info("Completed stats for SetPropertyVisit");
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
