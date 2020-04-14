package com.ec.common.Data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.common.Model.ContactAllInfo;

public class ContactsWithTypeAhead 
{
	List<String> contactNamesAndNumbers;
	Page<ContactAllInfo> contacts;
	public List<String> getContactNames() {
		return contactNamesAndNumbers;
	}
	public void setContactNames(List<String> contactNames) {
		this.contactNamesAndNumbers = contactNames;
	}
	public Page<ContactAllInfo> getContacts() {
		return contacts;
	}
	public void setContacts(Page<ContactAllInfo> contacts) {
		this.contacts = contacts;
	}
	
	
}
