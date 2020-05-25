package com.ec.common.Filters;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Machinery;
import com.ec.application.model.Machinery_;

public class MachinerySpecifications 
{
	static SpecificationsBuilder<Machinery> specbldr = new SpecificationsBuilder<Machinery>();
	
	public static Specification<Machinery> getSpecification(FilterDataList filterDataList)
	{
		List<String> machineryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"name");
		Specification<Machinery> finalSpec = null;
		if(machineryNames != null && machineryNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Machinery_.MACHINERY_NAME, machineryNames));	
		return finalSpec;
	}
}
