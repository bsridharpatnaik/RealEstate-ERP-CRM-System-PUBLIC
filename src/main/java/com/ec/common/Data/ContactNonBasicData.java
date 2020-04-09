package com.ec.common.Data;

public class ContactNonBasicData 
{

	Long contactId;
	String GSTDetails;
	String contactPerson;
	String contactPersonMobileNo;
	
	
	public ContactNonBasicData(Long contactId, String gSTDetails, String contactPerson, String contactPersonMobileNo) {
		super();
		this.contactId = contactId;
		GSTDetails = gSTDetails;
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
