package com.ec.common.Model;

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
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.springframework.lang.NonNull;

import com.ec.ReusableClasses.ReusableFields;
import com.ec.common.Configuration.ToLowerCaseDeserializer;
import com.ec.common.Data.CustomerTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Entity
@Table(name = "Contact")
//@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class ContactBasicInfo extends ReusableFields
{

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "contactId", updatable = false, nullable = false)
	Long contactId;
	
	@NonNull
	@Column(nullable=false)
	@JsonDeserialize(using = ToLowerCaseDeserializer.class)
	String name;
	
	@NonNull
	@Column(nullable=false)
	String mobileNo;
	
	String emailId;
	
	@NonNull
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	CustomerTypeEnum contactType;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="addrId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Address address;
	
	
	public Address getAddress() {
		return address;
	}


	public void setAddress(Address address) {
		this.address = address;
	}


	public Long getContactId() {
		return contactId;
	}


	public void setContactId(Long contactId) {
		this.contactId = contactId;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getMobileNo() {
		return mobileNo;
	}


	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}


	public String getEmailId() {
		return emailId;
	}


	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public CustomerTypeEnum getContactType() {
		return contactType;
	}


	public void setContactType(CustomerTypeEnum contactType) {
		this.contactType = contactType;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
