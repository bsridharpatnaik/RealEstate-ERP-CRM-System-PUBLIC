package com.ec.crm.Data;

import java.util.Date;

import com.ec.crm.Enums.SentimentEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.Data;

@Data
public class PipelineSingleReturnDTO
{
	Long leadId;
	String name;
	String mobileNumber;
	StagnatedEnum stagnantStatus;
	SentimentEnum sentiment;
	Boolean isOpen;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
	Date activityDateTime;
	@JsonSerialize(using = ToUsernameSerializer.class)
	Long assignee;

	public PipelineSingleReturnDTO()
	{
		super();
	}

	public PipelineSingleReturnDTO(Long leadId, String name, String mobileNumber, StagnatedEnum stagnantStatus,
			SentimentEnum sentiment, Date activityDateTime, Boolean isOpen, Long assignee)
	{
		super();
		this.leadId = leadId;
		this.name = name;
		this.mobileNumber = mobileNumber;
		this.stagnantStatus = stagnantStatus;
		this.sentiment = sentiment;
		this.activityDateTime = activityDateTime;
		this.isOpen = isOpen;
		this.assignee = assignee;
	}
}
