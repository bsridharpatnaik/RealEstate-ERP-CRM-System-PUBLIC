package com.ec.crm.Data;

import java.util.Date;
import java.util.List;

import com.ec.crm.Enums.SentimentEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

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
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd hh:mm:ss")
	Date activityDateTime;
	
	public PipelineSingleReturnDTO() 
	{
		super();
	}

	public PipelineSingleReturnDTO(Long leadId, String name, String mobileNumber, StagnatedEnum stagnantStatus,
			SentimentEnum sentiment, Date activityDateTime,Boolean isOpen) {
		super();
		this.leadId = leadId;
		this.name = name;
		this.mobileNumber = mobileNumber;
		this.stagnantStatus = stagnantStatus;
		this.sentiment = sentiment;
		this.activityDateTime = activityDateTime;
		this.isOpen=isOpen;
	}
}
