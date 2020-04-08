package com.ec.application.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "supplier")
public class Supplier 
{

	@Id
	Long contact_id;
	String name;
	String mobile_no;
	String email_id;
	String address;
	String contact_type;
	String contact_person;
	String contact_person_mobile_no;
	String gstdetails;
	public Long getContact_id() {
		return contact_id;
	}
	public void setContact_id(Long contact_id) {
		this.contact_id = contact_id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getMobile_no() {
		return mobile_no;
	}
	public void setMobile_no(String mobile_no) {
		this.mobile_no = mobile_no;
	}
	public String getEmail_id() {
		return email_id;
	}
	public void setEmail_id(String email_id) {
		this.email_id = email_id;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getContact_type() {
		return contact_type;
	}
	public void setContact_type(String contact_type) {
		this.contact_type = contact_type;
	}
	public String getContact_person() {
		return contact_person;
	}
	public void setContact_person(String contact_person) {
		this.contact_person = contact_person;
	}
	public String getContact_person_mobile_no() {
		return contact_person_mobile_no;
	}
	public void setContact_person_mobile_no(String contact_person_mobile_no) {
		this.contact_person_mobile_no = contact_person_mobile_no;
	}
	public String getGstdetails() {
		return gstdetails;
	}
	public void setGstdetails(String gstdetails) {
		this.gstdetails = gstdetails;
	}
	
	
}
