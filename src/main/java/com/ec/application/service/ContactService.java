package com.ec.application.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.BasicEntities.Contact;
import com.ec.application.repository.ContactRepo;

@Service
public class ContactService 
{

	@Autowired
	ContactRepo contactRepo;
	
	public Contact createContact(Contact payload) throws Exception
	{
		validatePayload(payload);
		formatMobileNo(payload);
		checkIfExists("create",payload);
		return contactRepo.save(payload);
	}

	private void checkIfExists(String type, Contact payload) throws Exception 
	{
		if(type.equalsIgnoreCase("create"))
		if(checkIfExistsWithSameNamePhone(payload.getName(),payload.getMobileNo()))
		{
			throw new Exception("Contact with same name and mobileno exits");
		}
		else if(type.equals("update"))
		{
			Optional<Contact> contactOpt = contactRepo.findById(payload.getContactId());
			if(!contactOpt.isPresent())
				throw new Exception("Contact not found with ID");
			Contact contactForUpdate = contactOpt.get();
			if(contactForUpdate.getName()!=payload.getName() || contactForUpdate.getMobileNo()!=payload.getMobileNo())
			{
				Boolean isExist = checkIfExistsWithSameNamePhone(payload.getName(),payload.getMobileNo());
				if(isExist)
					throw new Exception("Contact with same name and mobile no exists");
			}
		}
		
	}

	private void validatePayload(Contact payload) throws Exception 
	{
		if(!payload.getContactType().equalsIgnoreCase("Customer") &&
				!payload.getContactType().equalsIgnoreCase("Supplier") &&
				!payload.getContactType().equalsIgnoreCase("Contractor"))
			throw new Exception("Invalid Customer Type");
	}

	private void formatMobileNo(Contact payload)
	{
		if(!(payload.getContactPersonMobileNo()==null))
			payload.setContactPersonMobileNo(normalizePhoneNumber(payload.getContactPersonMobileNo()));
		payload.setMobileNo(normalizePhoneNumber(payload.getMobileNo()));
	}
	private boolean checkIfExistsWithSameNamePhone(String name,String mobileNo) 
	{
		int count = contactRepo.getCountByNameNo(name, mobileNo);
		if(count>0)
			return true;
		else
			return false;
	}

	public Page<Contact> findAll(Pageable pageable) 
	{
		return contactRepo.findAll(pageable);
	}

	public Contact findSingleContact(long id) throws Exception 
	{
		Optional<Contact> ContactOpt = contactRepo.findById(id);
		if(!ContactOpt.isPresent())
			throw new Exception("Contact not found with ID "+id);
		return ContactOpt.get();
		
	}

	public Contact updateContact(Long id, Contact payload) throws Exception 
	{
		validatePayload(payload);
		formatMobileNo(payload);
		checkIfExists("update",payload);
		
		Contact contact = contactRepo.findById(payload.getContactId()).get();
		populateData(payload,contact);
		return contactRepo.save(contact);
	}
	
	private void populateData(Contact payload, Contact contact) 
	{
		contact.setAddress(payload.getAddress());
		contact.setContactPerson(payload.getContactPerson());
		contact.setContactPersonMobileNo(payload.getContactPersonMobileNo());
		contact.setContactType(payload.getContactType());
		contact.setEmailId(payload.getEmailId());
		contact.setGSTDetails(payload.getGSTDetails());
		contact.setMobileNo(payload.getMobileNo());
		contact.setName(payload.getName());
	}

	public String normalizePhoneNumber(String number) {

	    number = number.replaceAll("[^+0-9]", ""); // All weird characters such as /, -, ...

	    String country_code = "0";

	    if (number.substring(0, 1).compareTo("0") == 0 && number.substring(1, 2).compareTo("0") != 0) {
	        number = "+" + country_code + number.substring(1); // e.g. 0172 12 34 567 -> + (country_code) 172 12 34 567
	    }

	    number = number.replaceAll("^[0]{1,4}", "+"); // e.g. 004912345678 -> +4912345678

	    return number;
	}
}
