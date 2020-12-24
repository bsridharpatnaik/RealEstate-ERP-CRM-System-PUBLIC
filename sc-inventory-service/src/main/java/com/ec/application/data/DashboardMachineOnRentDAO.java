package com.ec.application.data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DashboardMachineOnRentDAO
{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date date;
	String machineryName;
	String mode;

	public DashboardMachineOnRentDAO(Date date, String machineryName, String mode)
	{
		super();
		this.date = date;
		this.machineryName = machineryName;
		this.mode = mode;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date date)
	{
		this.date = date;
	}

	public String getMachineryName()
	{
		return machineryName;
	}

	public void setMachineryName(String machineryName)
	{
		this.machineryName = machineryName;
	}

	public String getMode()
	{
		return mode;
	}

	public void setMode(String mode)
	{
		this.mode = mode;
	}
}
