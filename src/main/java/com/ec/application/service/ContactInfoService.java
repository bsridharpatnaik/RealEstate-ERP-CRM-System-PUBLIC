package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.model.Category;
import com.ec.application.model.ContactInfo;
import com.ec.application.repository.ContactInfoRepo;

@Service
public class ContactInfoService 
{
	@Autowired
	ContactInfoRepo contactInfoRepo;
	
	public ContactInfo createContactInfo(ContactInfo payload) throws Exception 
	{
		if(contactInfoRepo.conactUsageCount(payload.getContactId())==0)
			return contactInfoRepo.save(payload);
		else
			throw new Exception("Contact Info already exists!");
    }
}
