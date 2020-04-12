package com.ec.common.Service;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ec.common.Data.ContactNonBasicData;
import com.ec.common.Data.CustomerTypeEnum;
import com.ec.common.Data.URLRepository;
import com.ec.common.Model.ContactAllInfo;
import com.ec.common.Model.ContactBasicInfo;
import com.ec.common.Repository.ContactAllInfoRepo;
import com.ec.common.Repository.ContactBasicInfoRepo;
import com.ec.utils.CommonUtils;

@Service
public class ContactService {

    @Autowired
    ContactBasicInfoRepo contactRepo;
    
    @Autowired
    ContactAllInfoRepo allContactsRepo;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    HttpServletRequest httpRequest;

    @Value("${inven.serverurl}")
    private String reqServer;

    CommonUtils utilObj = new CommonUtils();
    
    @Transactional
    public ContactAllInfo createContact(ContactAllInfo payload) throws Exception 
    {
    	ContactBasicInfo contact = new ContactBasicInfo();
        formatMobileNo(payload);
        exitIfMobileNoExists(payload);
        PopulateBasicFields(payload, contact);
        contactRepo.save(contact);
        payload.setContactId(contact.getContactId());
        if (payload.getContactType() != CustomerTypeEnum.CUSTOMER.toString()) 
        {
	        ContactNonBasicData contactNonBasicData = fetchNonBasicData(payload,contact);
	        PopulateNonBasicFields(payload,contactNonBasicData);
        }
        else
        {
        	
        }
        return payload;
    }

    public ContactAllInfo updateContact(Long id, ContactAllInfo payload) throws Exception 
    {
		ContactBasicInfo contact = findContactById(id);
		formatMobileNo(payload);
		if(!contact.getMobileNo().equals(payload.getMobileNo()))
				exitIfMobileNoExists(payload);
		if(payload.getContactType().toString().equals(CustomerTypeEnum.CUSTOMER) && 
				(!contact.getContactType().toString().equals(CustomerTypeEnum.CUSTOMER)))
			throw new Exception("Cannot convert contact from non-customer to customer");		
		PopulateBasicFields(payload, contact);
		contactRepo.save(contact);
		payload.setContactId(contact.getContactId());
		return payload;
    }
    
    public Page <ContactAllInfo> findAll(Pageable pageable) 
    {
        return allContactsRepo.findAll(pageable);
    }
    
    public ContactBasicInfo findContactById(long id) throws Exception 
    {
        Optional <ContactBasicInfo> ContactOpt = contactRepo.findById(id);
        if (!ContactOpt.isPresent())
            throw new Exception("Contact not found with ID " + id);
        return ContactOpt.get();
    }
    
    private ContactNonBasicData fetchNonBasicData(ContactAllInfo payload,ContactBasicInfo contact)
    {
    	 ContactNonBasicData contactInventoryData = new ContactNonBasicData(
                contact.getContactId(),
                payload.getGstNumber(),
                payload.getContactPerson(),
                payload.getContactPersonMobileNo());
          return passContactToInventory(contactInventoryData);    
    }
    private void PopulateNonBasicFields(ContactAllInfo contactAllInfo, ContactNonBasicData contactNonBasicData) 
    {
    	contactAllInfo.setGstNumber(contactNonBasicData.getGstNumber());
    	contactAllInfo.setContactPerson(contactNonBasicData.getContactPerson());
    	contactAllInfo.setContactPersonMobileNo(contactNonBasicData.getContactPersonMobileNo());
	}

	private void PopulateBasicFields(ContactAllInfo payload, ContactBasicInfo contact) 
    {
		contact.setAddress(payload.getAddress());
		contact.setContactType(CustomerTypeEnum.valueOf(payload.getContactType()));
		contact.setEmailId(payload.getEmailId());
		contact.setMobileNo(payload.getMobileNo());
		contact.setName(payload.getName());
	}

	private void passContactToCRM(Long contactId, ContactAllInfo payload) 
	{
        // TODO Auto-generated method stub

    }

    private ContactNonBasicData passContactToInventory(ContactNonBasicData payload) 
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", httpRequest.getHeader("Authorization"));
        HttpEntity < ContactNonBasicData > request = new HttpEntity < > (payload, headers);
        ContactNonBasicData response = restTemplate.postForObject(reqServer + URLRepository.addCustomerInfo, request, ContactNonBasicData.class);
        return response;
    }


    private void formatMobileNo(ContactAllInfo payload) 
    {
    	System.out.println(payload.getContactPersonMobileNo());
        if (!(payload.getContactPersonMobileNo() == null) && !payload.getContactPersonMobileNo().equals(""))
            payload.setContactPersonMobileNo(utilObj.normalizePhoneNumber(payload.getContactPersonMobileNo()));
        if(!payload.getMobileNo().equals(""))
        	payload.setMobileNo(utilObj.normalizePhoneNumber(payload.getMobileNo()));
    }
   
    private void exitIfMobileNoExists(ContactAllInfo payload) throws Exception 
    {
		if(allContactsRepo.findCountByMobileNo(payload.getMobileNo())>0)
			throw new Exception("Contact already exists by Mobile Number.");
	}



	public ContactAllInfo findSingleContactFromAll(long id) throws Exception 
	{
		Optional<ContactAllInfo> contactAllInfo = allContactsRepo.findById(id);
		if(!contactAllInfo.isPresent())
			throw new Exception("Contact not found by ID");
		return contactAllInfo.get();
	}
    
}