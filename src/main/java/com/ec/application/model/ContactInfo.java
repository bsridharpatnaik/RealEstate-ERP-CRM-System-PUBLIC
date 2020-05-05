package com.ec.application.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "contact_info")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
//@NamedQuery(name = "ContactInfo.findAll", query="select u from ContactInfo u order by u.created_at desc")
public class ContactInfo extends ReusableFields
{
	@Id
	Long contactId;
	
	@Column(name = "gst_number")
	String gstNumber;
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
