package com.ec.crm.Data;

import java.util.Date;

import org.springframework.lang.NonNull;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class LeadCreateData 
{
	@NonNull
	String customerName;
	@NonNull
	String primaryMobile;	
	String secondaryMobile;	
	String emailId;	
	String purpose;	
	String occupation;	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date dateOfBirth;
	Long brokerId;
	String addressLine1;
	String addressLine2;
	String city;
	String pincode;
	Long sourceId;
	String propertyType;
	Long sentimentId;
	Long assigneeId;

	@Override
	public String toString() {
		return "LeadCreateData [customerName=" + customerName + ", primaryMobile=" + primaryMobile
				+ ", secondaryMobile=" + secondaryMobile + ", emailId=" + emailId + ", purpose=" + purpose
				+ ", occupation=" + occupation + ", dateOfBirth=" + dateOfBirth + ", brokerId=" + brokerId
				+ ", addressLine1=" + addressLine1 + ", addressLine2=" + addressLine2 + ", city=" + city + ", pincode="
				+ pincode + ", sourceId=" + sourceId + ", propertyType=" + propertyType + ", sentimentId=" + sentimentId
				+ ", assigneeId=" + assigneeId + "]";
	}
}
