package com.ec.application.service;

import java.util.Optional;

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
	
	@Autowired
	SupplierService supplierService;
	
	@Autowired
	ContractorService contractorService;
	
	public ContactInfo createContactInfo(ContactInfo payload) throws Exception 
	{
		if(contactInfoRepo.conactUsageCount(payload.getContactId())==0)
			return contactInfoRepo.save(payload);
		else
		{
			Optional<ContactInfo> contactInfo = contactInfoRepo.findById(payload.getContactId());
			if(contactInfo.isPresent())
			{
				populateFields(contactInfo.get(),payload);
				contactInfoRepo.save(contactInfo.get());
				return contactInfo.get();
			}
			else 
				throw new Exception("Contact ID not found");
		}
    }

	private void populateFields(ContactInfo contactInfo, ContactInfo payload) 
	{
		contactInfo.setContactId(payload.getContactId());
		contactInfo.setContactPerson(payload.getContactPerson());
		contactInfo.setContactPersonMobileNo(payload.getContactPerson());
		contactInfo.setGstNumber(payload.getGstNumber());;
	}

	public Boolean checkIfContactIsUsed(Long id) 
	{
		Boolean  isUsed = false;
		
		if(supplierService.isContactUsed(id) || contractorService.isContactUsed(id))
			isUsed = true;
		return isUsed;
	}
}
