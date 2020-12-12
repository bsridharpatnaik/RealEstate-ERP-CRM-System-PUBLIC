package com.ec.application.data;

public class ContactNonBasicData 
{

	Long contactId;
	String gstNumber;
	String contactPerson;
	String contactPersonMobileNo;
	
	
	public ContactNonBasicData(Long contactId, String GSTNumber, String contactPerson, String contactPersonMobileNo) {
		super();
		this.contactId = contactId;
		this.gstNumber = GSTNumber;
		this.contactPerson = contactPerson;
		this.contactPersonMobileNo = contactPersonMobileNo;
	}
	public ContactNonBasicData() {
		// TODO Auto-generated constructor stub
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
