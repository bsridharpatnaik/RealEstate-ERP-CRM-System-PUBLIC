package com.ec.crm.Data;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.ec.crm.Model.Address;
import com.ec.crm.Model.Broker;
import com.ec.crm.Model.PropertyType;
import com.ec.crm.Model.Sentiment;
import com.ec.crm.Model.Source;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class LeadCreateData {
	Long leadId;
		
	String customerName;
		
	String primaryMobile;
		
	String secondaryMobile;
		
	String emailId;
		
	Long purposeId;
		
	String occupation;
		
	String dateOfBirth;
	
	Long brokerId;
	
	Long addressId;
	
	Long sourceId;
	
	Long propertyTypeId;
	
	Long sentimentId;
	
	Long UserId;

	public Long getLeadId() {
		return leadId;
	}

	public void setLeadId(Long leadId) {
		this.leadId = leadId;
	}

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

	public Long getPurposeId() {
		return purposeId;
	}

	public void setPurposeId(Long purposeId) {
		this.purposeId = purposeId;
	}

	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Long getBrokerId() {
		return brokerId;
	}

	public void setBrokerId(Long brokerId) {
		this.brokerId = brokerId;
	}

	public Long getAddressId() {
		return addressId;
	}

	public void setAddressId(Long addressId) {
		this.addressId = addressId;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}

	public Long getPropertyTypeId() {
		return propertyTypeId;
	}

	public void setPropertyTypeId(Long propertyTypeId) {
		this.propertyTypeId = propertyTypeId;
	}

	public Long getSentimentId() {
		return sentimentId;
	}

	public void setSentimentId(Long sentimentId) {
		this.sentimentId = sentimentId;
	}

	public Long getUserId() {
		return UserId;
	}

	public void setUserId(Long userId) {
		UserId = userId;
	}
	
	
	
}
