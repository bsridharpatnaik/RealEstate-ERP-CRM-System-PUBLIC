package com.ec.crm.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ec.crm.Model.ActivitiesStatsForDashboard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapForPipelineAndActivities
{
	public MapForPipelineAndActivities(long count, Map<Object, Long> collect)
	{
		ArrayList<MapForDashboardStatsDAO> d = new ArrayList<MapForDashboardStatsDAO>();
		for (Map.Entry<Object, Long> entry : collect.entrySet())
		{
			MapForDashboardStatsDAO mapForDashboardStatsDAO = new MapForDashboardStatsDAO();
			mapForDashboardStatsDAO.setUserName(entry.getKey().toString());
			mapForDashboardStatsDAO.setValue(entry.getValue());
			d.add(mapForDashboardStatsDAO);
		}
		this.detailed = d;
		this.total = count;
	}

	Long total;
	ArrayList<MapForDashboardStatsDAO> detailed;

	public MapForPipelineAndActivities(long count, List<ActivitiesStatsForDashboard> data) {
		ArrayList<MapForDashboardStatsDAO> d = new ArrayList<MapForDashboardStatsDAO>();
		for(ActivitiesStatsForDashboard ad : data){
			MapForDashboardStatsDAO mapForDashboardStatsDAO = new MapForDashboardStatsDAO();
			mapForDashboardStatsDAO.setUserName(ad.getUserName());
			mapForDashboardStatsDAO.setValue(ad.getCount());
			d.add(mapForDashboardStatsDAO);
		}
		this.detailed = d;
		this.total = count;
	}
}
