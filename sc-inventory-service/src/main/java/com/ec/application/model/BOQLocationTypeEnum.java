package com.ec.application.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum BOQLocationTypeEnum implements Serializable
{
	BuildingType, BuildingUnit;

	public BOQLocationTypeEnum setFromString(String name)
	{
		return BOQLocationTypeEnum.valueOf(name);
	}

	public static List<String> getValidBOQTypes()
	{
		List<String> EnumValues = new ArrayList<String>();
		EnumSet.allOf(BOQLocationTypeEnum.class).forEach(type -> EnumValues.add(type.toString()));
		return EnumValues;
	}
}
