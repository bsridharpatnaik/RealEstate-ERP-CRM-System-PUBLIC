package com.ec.crm.Enums;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum StagnatedEnum 
{
	NoColor,
	RED,
	GREEN,
	ORANGE;
	public StagnatedEnum setFromString(String name)
	{
		return StagnatedEnum.valueOf(name);
	}
	
	public static List<String> getValidSentiments()
	{
		List<String> stagnatedEnumList = new ArrayList<String>();
		EnumSet.allOf(StagnatedEnum.class)
        .forEach(type -> stagnatedEnumList.add(type.toString()));
		return stagnatedEnumList;
	}
}
