package com.ec.application.data;

import java.util.Date;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.ec.application.model.MORRentModeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class DashboardMachineOnRentDAO
{
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date date;
	String machineryName;
	@Enumerated(EnumType.STRING)
	MORRentModeEnum mode;

	public DashboardMachineOnRentDAO(Date date, String machineryName, MORRentModeEnum mode)
	{
		super();
		this.date = date;
		this.machineryName = machineryName;
		this.mode = mode;
	}
}
