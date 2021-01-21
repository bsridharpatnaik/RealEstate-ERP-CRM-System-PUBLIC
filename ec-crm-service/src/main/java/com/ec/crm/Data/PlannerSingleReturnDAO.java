package com.ec.crm.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.Data;

@Data
public class PlannerSingleReturnDAO
{
	Long leadId;
	String name;
	String mobileNumber;
	boolean activityStatus;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss a")
	Date activityDateTime;
	@JsonSerialize(using = ToUsernameSerializer.class)
	Long assignee;

	public PlannerSingleReturnDAO()
	{
		super();
	}

	public PlannerSingleReturnDAO(Long leadId, String name, String mobileNumber, boolean activityStatus,
			Date activityDateTime, Long assignee)
	{
		super();
		this.leadId = leadId;
		this.name = name;
		this.mobileNumber = mobileNumber;
		this.activityStatus = activityStatus;
		this.activityDateTime = activityDateTime;
		this.assignee = assignee;
	}
}
