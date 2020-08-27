package com.ec.crm.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

import com.ec.crm.Model.LeadActivity;

import lombok.Data;

@Data
public class MapForPipelineAndActivities 
{
	Long total;
	Map<Object, Long> detailed;
	
	public MapForPipelineAndActivities(long count, Map<Object, Long> collect) 
	{
		this.total = count;
		this.detailed=collect;
	}

	public MapForPipelineAndActivities(long count, Map<Object, List<LeadActivity>> collect,
			Collector<Object, ?, Long> counting) {
		// TODO Auto-generated constructor stub
	}
}
