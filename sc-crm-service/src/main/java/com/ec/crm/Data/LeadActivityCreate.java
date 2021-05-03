package com.ec.crm.Data;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.ec.crm.Enums.DealLostReasonEnum;
import org.springframework.lang.NonNull;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
@Data
public class LeadActivityCreate 
{
	@NotNull
	Long leadId;
	
	@NonNull
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	ActivityTypeEnum activityType;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="dd-MM-yyyy HH:mm")
	@NotNull
	Date activityDateTime;
	
	@NotBlank
	String title;
	
	Long duration;
	String description;
	List<String> tags;
	@Enumerated(EnumType.STRING)
	DealLostReasonEnum dealLostReason;
	public LeadActivityCreate() {
		super();
		// TODO Auto-generated constructor stub
	}
	
}
