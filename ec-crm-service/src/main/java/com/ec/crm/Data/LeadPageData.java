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
import com.ec.crm.Model.LeadActivity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.Data;
@Data
public class LeadPageData 
{
	Long leadId;
	
	String name;
	
	String mobileNumber;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm")
	Date activityDateTime;
	
	//@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss ")
	//Date created;
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	ActivityTypeEnum activityType;
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	LeadStatusEnum leadStatus;
	
	Boolean isOpen;
	
	@JsonSerialize(using=ToUsernameSerializer.class)
	Long assigneeId;
	
	public LeadPageData() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LeadPageData(LeadActivity la) 
	{
		this.leadId = la.getLead().getLeadId();
		this.name = la.getLead().getCustomerName();
		this.mobileNumber = la.getLead().getPrimaryMobile();
		this.activityType=la.getActivityType();
		this.activityDateTime=la.getActivityDateTime();
		this.leadStatus=la.getLead().getStatus();
		this.isOpen=la.getIsOpen();
		this.assigneeId = la.getLead().getAsigneeId();
	}
	
}
