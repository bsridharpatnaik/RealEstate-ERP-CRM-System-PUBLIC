
package com.ec.crm.Data;

import java.util.Date;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.Data;

@Data
public class LeadDAO
{
	Long leadId;

	String customerName;

	String primaryMobile;

	String secondaryMobile;

	String emailId;

	String purpose;

	String occupation;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date dateOfBirth;

	String broker;

	String addr_line1;
	String addr_line2;
	String city;
	String pincode;

	String source;

	Boolean isProspectLead;

	@Enumerated(EnumType.STRING)
	PropertyTypeEnum propertyType;

	@Enumerated(EnumType.STRING)
	SentimentEnum sentiment;

	@JsonSerialize(using = ToUsernameSerializer.class)
	Long asigneeId;

	Long assigneeUserId;

	@JsonSerialize(using = ToUsernameSerializer.class)
	Long creatorId;

	LeadStatusEnum status;

	Long stagnantDaysCount;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date lastActivityModifiedDate;
}
