package com.ec.crm.Data;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.lang.NonNull;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
@Data
public class LeadPageData 
{
	String name;
	
	String mobileNumber;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm")
	Date activityDateTime;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss ")
	Date created;
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	ActivityTypeEnum activityType;
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	LeadStatusEnum leadStatus;
	
	Boolean isOpen;
	
	public LeadPageData() {
		super();
		// TODO Auto-generated constructor stub
	}




	public LeadPageData(String name, String mobileNumber, ActivityTypeEnum activityType,  Date activityDateTime, Date created , LeadStatusEnum leadStatus, Boolean isOpen) {
		super();
		this.name = name;
		this.mobileNumber = mobileNumber;
		this.activityType=activityType;
		this.activityDateTime=activityDateTime;
		this.created=created;
		this.leadStatus=leadStatus;
		this.isOpen=isOpen;
	}
	
}
