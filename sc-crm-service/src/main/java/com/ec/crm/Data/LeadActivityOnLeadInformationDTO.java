package com.ec.crm.Data;

import java.util.Date;
import java.util.List;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.ec.crm.Enums.DealLostReasonEnum;
import org.mapstruct.Mapping;
import org.springframework.lang.NonNull;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Model.Lead;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadActivityOnLeadInformationDTO 
{
	
	Long leadActivityId;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="MM-dd-yyyy HH:mm")
	Date activityDateTime;
	
	@NonNull
	String title;
	
	String description;
	
	List<String> tags;
	
	LeadDTOforLeadInformation lead;
	
	@NonNull
	Boolean isOpen;
	
	@JsonSerialize(using=ToUsernameSerializer.class)
	Long creatorId;
	
	@JsonSerialize(using=ToUsernameSerializer.class)
	Long closedBy;
	
	String closingComment;
	
	@Enumerated(EnumType.STRING)
	ActivityTypeEnum activityType;

	@Enumerated(EnumType.STRING)
	DealLostReasonEnum dealLostReason;

	Integer followUpCount;

	Boolean isRevertable;
}
