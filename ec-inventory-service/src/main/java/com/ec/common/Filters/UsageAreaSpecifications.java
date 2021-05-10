package com.ec.common.Filters;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category;
import com.ec.application.model.Category_;
import com.ec.application.model.UsageArea;
import com.ec.application.model.UsageArea_;

public class UsageAreaSpecifications {

static SpecificationsBuilder<UsageArea> specbldr = new SpecificationsBuilder<UsageArea>();
	
	public static Specification<UsageArea> getSpecification(FilterDataList filterDataList)
	{
		List<String> usageAreaNames = specbldr.fetchValueFromFilterList(filterDataList,"name");
		Specification<UsageArea> finalSpec = null;
		if(usageAreaNames != null && usageAreaNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(UsageArea_.USAGE_AREA_NAME, usageAreaNames));	
		return finalSpec;
	}

}
