package com.ec.crm.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
@Data
public class LeadActivityCreate {
	Long leadActivityId;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	Date activityDate;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	Date creationDate;
	
	String title;
	
	String description;
	
	String tags;
	
	String status;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="HH:mm:ss")
	Date time;
	
	Long userId;
	
	Long leadId;
	
	Long activityTypeId;
}
