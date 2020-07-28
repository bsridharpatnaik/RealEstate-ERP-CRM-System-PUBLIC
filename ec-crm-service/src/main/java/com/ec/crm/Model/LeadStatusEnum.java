package com.ec.crm.Model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum LeadStatusEnum 
{
	New_Lead,
	Property_Visit,
	Negotiation,
	Deal_Closed, 
	Deal_Lost;

	public PropertyTypeEnum setFromString(String name)
	{
		return PropertyTypeEnum.valueOf(name);
	}
	
	public static List<String> getValidLeadStatus()
	{
		List<String> leadStatuses = new ArrayList<String>();
		EnumSet.allOf(LeadStatusEnum.class)
        .forEach(type -> leadStatuses.add(type.toString()));
		return leadStatuses;
	}
	
}
