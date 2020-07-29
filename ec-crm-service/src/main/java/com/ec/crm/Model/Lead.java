package com.ec.crm.Model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;

@Entity
@Table(name = "customer_lead")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited(withModifiedFlag = true)
public class Lead extends ReusableFields implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "lead_id", updatable = false, nullable = false)
	Long leadId;
	
	@Column(name="name")
	String customerName;
	
	@Column(name="primary_mobile")
	String primaryMobile;
	
	@Column(name="secondary_mobile")
	String secondaryMobile;
	
	@Column(name="email_id")
	String emailId;
	
	@Column(name="purpose")
	String purpose;
	
	@Column(name="occupation")
	String occupation;
	
	@Column(name="dateofbirth")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	Date dateOfBirth;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="broker_id",nullable=true)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Broker broker;
	
	@OneToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="address_id",nullable=true)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Address address;
	
	@OneToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="source_id",nullable=true)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Source source;
	
	@NonNull
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	PropertyTypeEnum propertyType;
	
	@OneToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="sentiment_id",nullable=true)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Sentiment sentiment;
	
	@Column(name="user_id")
	@JsonSerialize(using=ToUsernameSerializer.class)
	Long asigneeId;

	@Column(name="created_by")
	@JsonSerialize(using=ToUsernameSerializer.class)
	Long creatorId;
	
	@NonNull
	@Column(name="status",nullable=false)
	@Enumerated(EnumType.STRING)
	LeadStatusEnum status; 
	
	public LeadStatusEnum getStatus() {
		return status;
	}

	public void setStatus(LeadStatusEnum status) {
		this.status = status;
	}

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

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Broker getBroker() {
		return broker;
	}

	public void setBroker(Broker broker) {
		this.broker = broker;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}


	public PropertyTypeEnum getPropertyType() {
		return propertyType;
	}

	public void setPropertyType(PropertyTypeEnum propertyType) {
		this.propertyType = propertyType;
	}

	public Sentiment getSentiment() {
		return sentiment;
	}

	public void setSentiment(Sentiment sentiment) {
		this.sentiment = sentiment;
	}

	public Long getAsigneeId() {
		return asigneeId;
	}

	public void setAsigneeId(Long asigneeId) {
		this.asigneeId = asigneeId;
	}

	public Long getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(Long creatorId) {
		this.creatorId = creatorId;
	}
}
