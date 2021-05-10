package com.ec.application.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum MORRentModeEnum
{

	Hourly, Daily, MeterReading, TripCount;

	public MORRentModeEnum setFromString(String name)
	{
		return MORRentModeEnum.valueOf(name);
	}

	public static List<String> getValidModes()
	{
		List<String> validModes = new ArrayList<String>();
		EnumSet.allOf(MORRentModeEnum.class).forEach(type -> validModes.add(type.toString()));
		return validModes;
	}
}
