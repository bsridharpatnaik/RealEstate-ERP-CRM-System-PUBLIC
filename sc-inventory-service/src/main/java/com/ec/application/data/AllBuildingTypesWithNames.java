package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.BuildingType;

public class AllBuildingTypesWithNames
{
	List<String> names;
	Page<BuildingType> buildingTypes;

	public List<String> getNames()
	{
		return names;
	}

	public void setNames(List<String> names)
	{
		this.names = names;
	}

	public Page<BuildingType> getBuildingTypes()
	{
		return buildingTypes;
	}

	public void setBuildingTypes(Page<BuildingType> buildingTypes)
	{
		this.buildingTypes = buildingTypes;
	}

}
