package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.BuildingType;

import lombok.Data;

@Data
public class AllBuildingTypesWithNames
{
	List<String> names;
	Page<BuildingType> buildingTypes;

}
