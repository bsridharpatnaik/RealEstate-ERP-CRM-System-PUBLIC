package com.ec.crm.Data;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.lang.NonNull;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
@Data
public class LeadPageData 
{
	String name;
	
	String mobileNumber;
	
	@NonNull
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	ActivityTypeEnum activityType;
	
	@NonNull
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	LeadStatusEnum leadStatus;
	
	
	
	
	public LeadPageData() {
		super();
		// TODO Auto-generated constructor stub
	}




	public LeadPageData(String name, String mobileNumber, ActivityTypeEnum activityType, LeadStatusEnum leadStatus) {
		super();
		this.name = name;
		this.mobileNumber = mobileNumber;
		
	}
	
}
