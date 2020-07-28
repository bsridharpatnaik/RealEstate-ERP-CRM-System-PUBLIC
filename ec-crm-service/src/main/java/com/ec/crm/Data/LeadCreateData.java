package com.ec.crm.Data;

import java.util.Date;

import org.springframework.lang.NonNull;

import com.ec.crm.Model.PropertyTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

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

	public String getCustomerName() {
		return customerName;
	}
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
	public String getPrimaryMobile() {
		return primaryMobile;
	}
	public void setPrimaryMobile(String primaryMobile) {
		this.primaryMobile = primaryMobile;
	}
	public String getSecondaryMobile() {
		return secondaryMobile;
	}
	public void setSecondaryMobile(String secondaryMobile) {
		this.secondaryMobile = secondaryMobile;
	}
	public String getEmailId() {
		return emailId;
	}
	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public String getOccupation() {
		return occupation;
	}
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}
	public Date getDateOfBirth() {
		return dateOfBirth;
	}
	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}
	public Long getBrokerId() {
		return brokerId;
	}
	public void setBrokerId(Long brokerId) {
		this.brokerId = brokerId;
	}
	public String getAddressLine1() {
		return addressLine1;
	}
	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}
	public String getAddressLine2() {
		return addressLine2;
	}
	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getPincode() {
		return pincode;
	}
	public void setPincode(String pincode) {
		this.pincode = pincode;
	}
	public Long getSourceId() {
		return sourceId;
	}
	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}
	public String getPropertyType() {
		return propertyType;
	}
	public void setPropertyType(String propertyType) {
		this.propertyType = propertyType;
	}
	public Long getSentimentId() {
		return sentimentId;
	}
	public void setSentimentId(Long sentimentId) {
		this.sentimentId = sentimentId;
	}
	public Long getAssigneeId() {
		return assigneeId;
	}
	public void setAssigneeId(Long assigneeId) {
		this.assigneeId = assigneeId;
	}
}
