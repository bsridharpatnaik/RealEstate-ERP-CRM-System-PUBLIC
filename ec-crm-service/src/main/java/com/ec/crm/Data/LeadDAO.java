package com.ec.crm.Data;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.springframework.lang.NonNull;

import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.ec.crm.Model.Address;
import com.ec.crm.Model.Broker;
import com.ec.crm.Model.Source;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	Date dateOfBirth;
	
	String broker;
	
	String addr_line1;
	String addr_line2;
	String city;
	String pincode; 
	
	String source;
	
	@Enumerated(EnumType.STRING)
	PropertyTypeEnum propertyType;
	

	@Enumerated(EnumType.STRING)
	SentimentEnum sentiment;
	
	@JsonSerialize(using=ToUsernameSerializer.class)
	Long asigneeId;

	@JsonSerialize(using=ToUsernameSerializer.class)
	Long creatorId;
	
	LeadStatusEnum status; 
	
	Long stagnantDays;
}
