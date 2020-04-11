package com.ec.application.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.SoftDelete.SoftDeletableEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "contact_info")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class ContactInfo extends SoftDeletableEntity
{
	@Id
	Long contactId;
	
	@Column(name = "gst_number")
	String gstNumber;
	String contactPerson;
	String contactPersonMobileNo;
	
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

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
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
