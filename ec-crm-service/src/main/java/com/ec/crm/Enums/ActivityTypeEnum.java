package com.ec.crm.Enums;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ActivityTypeEnum 
{

	Call,
	Meeting,
	Property_Visit,
	Deal_Close,
	Reminder,
	Task,
	Message,
	Email,
	Deal_Lost;
	
	public ActivityTypeEnum setFromString(String name)
	{
		return ActivityTypeEnum.valueOf(name);
	}
	
	public static List<String> getValidActivityTypes()
	{
		List<String> validActivityType = new ArrayList<String>();
		EnumSet.allOf(ActivityTypeEnum.class)
        .forEach(type -> validActivityType.add(type.toString()));
		return validActivityType;
	}
}

