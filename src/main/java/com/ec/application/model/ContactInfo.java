package com.ec.application.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.SoftDelete.SoftDeletableEntity;

@Entity
@Table(name = "contact_info")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class ContactInfo extends SoftDeletableEntity
{
	@Id
	Long contactId;
	String GSTDetails;
	String contactPerson;
	String contactPersonMobileNo;
	
	public ContactInfo()
	{}
	
	public ContactInfo(Long contactId, String gSTDetails, String contactPerson, String contactPersonMobileNo) {
		super();
		this.contactId = contactId;
		GSTDetails = gSTDetails;
		this.contactPerson = contactPerson;
		this.contactPersonMobileNo = contactPersonMobileNo;
	}
	public Long getContactId() {
		return contactId;
	}
	public void setContactId(Long contactId) {
		this.contactId = contactId;
	}
	public String getGSTDetails() {
		return GSTDetails;
	}
	public void setGSTDetails(String gSTDetails) {
		GSTDetails = gSTDetails;
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
