package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

@Entity
@Subselect("select * from contractor")
@Immutable
@Audited
public class Contractor extends Contact
{

	@Id
	@Column(name="contact_id")
	Long contactId;
	
	public Long getContact_id() {
		return contactId;
	}
	public void setContact_id(Long contact_id) {
		this.contactId = contact_id;
	}
}
