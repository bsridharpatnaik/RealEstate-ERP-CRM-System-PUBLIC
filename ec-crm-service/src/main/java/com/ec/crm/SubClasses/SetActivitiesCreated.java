package com.ec.crm.SubClasses;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.LeadActivity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SetActivitiesCreated implements Runnable
{
	private CyclicBarrier barrier;
	private PipelineAndActivitiesForDashboard dashboardPipelineReturnData;
	private List<LeadActivity> data;
	Map<Long, String> idNameMap;

	public SetActivitiesCreated(CyclicBarrier barrier, PipelineAndActivitiesForDashboard dashboardPipelineReturnData,
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

		try
		{
			// do your task here
			dashboardPipelineReturnData.setActivitiesCreated(
					new MapForPipelineAndActivities(data.stream().filter(c -> c.getCreatorId() != 404).count(),
							data.stream().filter(c -> c.getCreatorId() != 404).collect(Collectors.groupingBy(c ->
							{
								try
								{
									return idNameMap.get(c.getLead().getAsigneeId());
								} catch (Exception e)
								{ // TODO Auto-generated catch block e.printStackTrace();
								}
								return null;
							}, Collectors.counting()))));
		} finally
		{
			try
			{
				barrier.await();
			} catch (InterruptedException | BrokenBarrierException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
