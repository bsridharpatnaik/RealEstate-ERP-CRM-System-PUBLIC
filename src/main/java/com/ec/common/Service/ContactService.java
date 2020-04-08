package com.ec.common.Service;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import com.ec.common.Data.ContactClubbedData;
import com.ec.common.Data.ContactInventoryData;
import com.ec.common.Data.CreateContactData;
import com.ec.common.Data.CustomerTypeEnum;
import com.ec.common.Model.Contact;
import com.ec.common.Repository.ContactRepo;

@Service
public class ContactService 
{

	@Autowired
	ContactRepo contactRepo;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Value("${inven.serverurl}")
	private String reqUrl;
	
	@Transactional
	public ContactClubbedData createContact(CreateContactData payload) throws Exception
	{
		ContactClubbedData contactClubbedData = new ContactClubbedData();
		Contact contact=new Contact();
		validatePayload(payload);
		formatMobileNo(payload);
		populateContactFields(contact,payload);
		checkIfExists("create",contact);
		contactClubbedData.setContact(contactRepo.save(contact));
		
		
		if(payload.getContactType()!=CustomerTypeEnum.CUSTOMER.toString())
		{
			ContactInventoryData contactInventoryData = new ContactInventoryData(
					contact.getContactId(),
					payload.getGSTDetails(),
					payload.getContactPerson(),
					payload.getContactPersonMobileNo());
			contactClubbedData.setContactInventoryData(passContactToInventory(contactInventoryData));
		}
		else
			passContactToCRM(contact.getContactId(),payload); 
		return contactClubbedData;
	}

	private void passContactToCRM(Long contactId, CreateContactData payload) 
	{
		// TODO Auto-generated method stub
		
	}

	private ContactInventoryData passContactToInventory(ContactInventoryData payload) 
	{
		String url = reqUrl + "/contact/create";
		ContactInventoryData response = restTemplate.postForObject(url, payload, ContactInventoryData.class);
		return response;
		
	}

	private void populateContactFields(Contact contact, CreateContactData payload) 
	{
		contact.setAddress(payload.getAddress());
		System.out.println();
		contact.setContactType(CustomerTypeEnum.valueOf(payload.getContactType()));
		contact.setEmailId(payload.getEmailId());
		contact.setMobileNo(payload.getMobileNo());
		contact.setName(payload.getName());
	}

	
	private void checkIfExists(String type, Contact contact) throws Exception 
	{
		if(type.equalsIgnoreCase("create"))
		if(checkIfExistsWithSameNamePhone(contact.getName(),contact.getMobileNo()))
		{
			throw new Exception("Contact with same name and mobileno exits");
		}
		else if(type.equals("update"))
		{
			Optional<Contact> contactOpt = contactRepo.findById(contact.getContactId());
			if(!contactOpt.isPresent())
				throw new Exception("Contact not found with ID");
			Contact contactForUpdate = contactOpt.get();
			if(contactForUpdate.getName()!=contact.getName() || contactForUpdate.getMobileNo()!=contact.getMobileNo())
			{
				Boolean isExist = checkIfExistsWithSameNamePhone(contact.getName(),contact.getMobileNo());
				if(isExist)
					throw new Exception("Contact with same name and mobile no exists");
			}
		}
		
	}

	private void validatePayload(CreateContactData payload) throws Exception 
	{
		
	}

	private void formatMobileNo(CreateContactData payload)
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

	public Contact updateContact(Long id, CreateContactData payload) throws Exception 
	{
		Contact contact = findSingleContact(id);
		validatePayload(payload);
		formatMobileNo(payload);
		Boolean isNameMobileChanged = checkIfNameMobilenoChanged(contact,payload);
		populateContactFields(contact,payload);
		if(isNameMobileChanged)
			checkIfExists("update",contact);
		return contactRepo.save(contact);
	}
	private Boolean checkIfNameMobilenoChanged(Contact contact, CreateContactData payload) 
	{
		if(!contact.getName().equalsIgnoreCase(payload.getName()) 
				|| !contact.getMobileNo().equals(payload.getMobileNo()))
			return true;
		else
			return false;
	}

	public String normalizePhoneNumber(String number) 
	{

	    number = number.replaceAll("[^+0-9]", ""); // All weird characters such as /, -, ...

	    String country_code = "0";

	    if (number.substring(0, 1).compareTo("0") == 0 && number.substring(1, 2).compareTo("0") != 0) {
	        number = "+" + country_code + number.substring(1); // e.g. 0172 12 34 567 -> + (country_code) 172 12 34 567
	    }

	    number = number.replaceAll("^[0]{1,4}", "+"); // e.g. 004912345678 -> +4912345678

	    return number;
	}
}
