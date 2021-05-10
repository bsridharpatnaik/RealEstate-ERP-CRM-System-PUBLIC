package com.ec.crm.Data;

import java.util.ArrayList;
import java.util.Map;

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
}
