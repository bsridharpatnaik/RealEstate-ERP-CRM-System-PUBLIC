package com.ec.crm.Enums;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum PropertyTypeEnum implements Serializable
{ 
	HOUSE_2_BHK,
	HOUSE_3_BHK,
	PLOT;
	
	public PropertyTypeEnum setFromString(String name)
	{
		return PropertyTypeEnum.valueOf(name);
	}
	
	public static List<String> getValidPropertyType()
	{
		List<String> propertyTypes = new ArrayList<String>();
		EnumSet.allOf(PropertyTypeEnum.class)
        .forEach(type -> propertyTypes.add(type.toString()));
		return propertyTypes;
	}
}
