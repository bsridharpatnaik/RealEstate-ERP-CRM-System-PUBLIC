package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Entity
@Table(name = "contact_info")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class ContactInfo extends ReusableFields
{
	@Id
	Long contactId;
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	@Column(name = "gst_number")
	String gstNumber;
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	String contactPerson;
	String contactPersonMobileNo;
	
	/*
	@CreationTimestamp
	@Column(name = "created_at")
	@JsonProperty("created_at")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd hh:mm:ss")
	private Date created;
	
	
	@Column(name = "updated_at")
	@JsonProperty("updated_at")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd hh:mm:ss")
	@UpdateTimestamp
	private Date modified;
	
	*/
	public ContactInfo()
	{}
	
	public ContactInfo(Long contactId, String GSTNumber, String contactPerson, String contactPersonMobileNo) {
		super();
		this.contactId = contactId;
		this.gstNumber = GSTNumber;
		this.contactPerson = contactPerson;
		this.contactPersonMobileNo = contactPersonMobileNo;
	}
	public Long getContactId() {
		return contactId;
	}
	public void setContactId(Long contactId) {
		this.contactId = contactId;
	}
	

	public String getGstNumber() {
		return gstNumber;
	}

	public void setGstNumber(String gstNumber) {
		this.gstNumber = gstNumber;
	}
	
	public String getContactPerson() {
		return contactPerson;
	}
	public void setContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
	}
	public String getContactPersonMobileNo() {
		return contactPersonMobileNo;
	}
	public void setContactPersonMobileNo(String contactPersonMobileNo) {
		this.contactPersonMobileNo = contactPersonMobileNo;
	}
	
	
}
