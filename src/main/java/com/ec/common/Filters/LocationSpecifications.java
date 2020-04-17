package com.ec.common.Filters;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.UsageLocation;
import com.ec.application.model.UsageLocation_;

public class LocationSpecifications 
{

static SpecificationsBuilder<UsageLocation> specbldr = new SpecificationsBuilder<UsageLocation>();
	
	public static Specification<UsageLocation> getSpecification(FilterDataList filterDataList)
	{
		List<String> locationNames = specbldr.fetchValueFromFilterList(filterDataList,"name");
		Specification<UsageLocation> finalSpec = null;
		if(locationNames != null && locationNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(UsageLocation_.LOCATION_NAME, locationNames));	
		return finalSpec;
	}
}
