package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;

@MappedSuperclass
@Audited
public class Contact 
{
	@Column(name="name")
	String name;
	
	@Column(name="mobile_no")
	String mobileNo;
	
	@Column(name="email_id")
	String emailId;
	
	@Column(name="contact_type")
	String contactType;
	
	@Column(name="contact_person")
	String contactPerson;
	
	@Column(name="contact_person_mobile_no")
	String contactPersonMobileNo;
	
	@Column(name="gst_number")
	String GSTNumber;

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

	public String getContactType() {
		return contactType;
	}

	public void setContactType(String contactType) {
		this.contactType = contactType;
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

	public String getGSTNumber() {
		return GSTNumber;
	}

	public void setGSTNumber(String gSTNumber) {
		GSTNumber = gSTNumber;
	}

	
}
