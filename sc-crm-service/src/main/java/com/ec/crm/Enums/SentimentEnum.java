package com.ec.crm.Enums;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum SentimentEnum 
{
	HOT,
	COLD,
	WARM;
	public SentimentEnum setFromString(String name)
	{
		return SentimentEnum.valueOf(name);
	}
	
	public static List<String> getValidSentiments()
	{
		List<String> sentiments = new ArrayList<String>();
		EnumSet.allOf(SentimentEnum.class)
        .forEach(type -> sentiments.add(type.toString()));
		return sentiments;
	}
}
