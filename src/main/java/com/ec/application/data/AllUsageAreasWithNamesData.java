package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.UsageArea;

public class AllUsageAreasWithNamesData 
{
	Page<UsageArea> usageAreas;
	List<String> names;
	public Page<UsageArea> getUsageAreas() {
		return usageAreas;
	}
	public void setUsageAreas(Page<UsageArea> usageAreas) {
		this.usageAreas = usageAreas;
	}
	public List<String> getNames() {
		return names;
	}
	public void setNames(List<String> names) {
		this.names = names;
	}
}
