package com.ec.common.Filters;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.BuildingType;
import com.ec.application.model.BuildingType_;

public final class BuildingTypeSpecifications
{
	static SpecificationsBuilder<BuildingType> specbldr = new SpecificationsBuilder<BuildingType>();

	public static Specification<BuildingType> getSpecification(FilterDataList filterDataList)
	{
		List<String> buildingTypeNames = specbldr.fetchValueFromFilterList(filterDataList, "name");
		Specification<BuildingType> finalSpec = null;
		if (buildingTypeNames != null && buildingTypeNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(BuildingType_.TYPE_NAME, buildingTypeNames));
		return finalSpec;
	}
}
