package com.ec.common.Data;

import com.ec.common.Model.Contact;

public class ContactClubbedData 
{
	Contact contact;
	ContactInventoryData ContactInventoryData;
	public Contact getContact() {
		return contact;
	}
	public void setContact(Contact contact) {
		this.contact = contact;
	}
	public ContactInventoryData getContactInventoryData() {
		return ContactInventoryData;
	}
	public void setContactInventoryData(ContactInventoryData contactInventoryData) {
		ContactInventoryData = contactInventoryData;
	}
}
