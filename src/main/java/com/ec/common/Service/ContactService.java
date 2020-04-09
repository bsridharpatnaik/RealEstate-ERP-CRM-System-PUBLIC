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
import com.ec.common.Data.CreateContactData;
import com.ec.common.Data.CustomerTypeEnum;
import com.ec.common.Data.URLRepository;
import com.ec.common.Model.ContactAllInfo;
import com.ec.common.Model.ContactBasicInfo;
import com.ec.common.Repository.ContactAllInfoRepo;
import com.ec.common.Repository.ContactBasicInfoRepo;

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

    @Transactional
    public ContactAllInfo createContact(CreateContactData payload) throws Exception {
    	ContactAllInfo contactAllInfo = new ContactAllInfo();
        ContactBasicInfo contact = new ContactBasicInfo();
        formatMobileNo(payload);
        populateContactFields(contact, payload);
        checkIfExists("create", contact);
        contactRepo.save(contact);
        PopulateBasicFields(contactAllInfo,contact);
        
        if (payload.getContactType() != CustomerTypeEnum.CUSTOMER.toString()) 
        {
            ContactNonBasicData contactInventoryData = new ContactNonBasicData(
                contact.getContactId(),
                payload.getGSTDetails(),
                payload.getContactPerson(),
                payload.getContactPersonMobileNo());
            ContactNonBasicData contactNonBasicData = passContactToInventory(contactInventoryData);
            PopulateNonBasicFields(contactAllInfo,contactNonBasicData);
        } 
        else
            passContactToCRM(contact.getContactId(), payload);
        
        
        return contactAllInfo;
    }

    private void PopulateNonBasicFields(ContactAllInfo contactAllInfo, ContactNonBasicData contactNonBasicData) 
    {
    	contactAllInfo.setGSTDetails(contactNonBasicData.getGSTDetails());
    	contactAllInfo.setContactPerson(contactNonBasicData.getContactPerson());
    	contactAllInfo.setContactPersonMobileNo(contactNonBasicData.getContactPersonMobileNo());
		
	}

	private void PopulateBasicFields(ContactAllInfo contactAllInfo, ContactBasicInfo contact) 
    {
		contactAllInfo.setContactId(contact.getContactId());
		contactAllInfo.setContactType(contact.getContactType().toString());
		contactAllInfo.setEmailId(contact.getEmailId());
		contactAllInfo.setMobileNo(contact.getMobileNo());
		contactAllInfo.setName(contact.getName());;
	}

	private void passContactToCRM(Long contactId, CreateContactData payload) 
	{
        // TODO Auto-generated method stub

    }

    private ContactNonBasicData passContactToInventory(ContactNonBasicData payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", httpRequest.getHeader("Authorization"));
        HttpEntity < ContactNonBasicData > request = new HttpEntity < > (payload, headers);
        ContactNonBasicData response = restTemplate.postForObject(reqServer + URLRepository.addCustomerInfo, request, ContactNonBasicData.class);
        return response;

    }

    private void populateContactFields(ContactBasicInfo contact, CreateContactData payload) {
        contact.setAddress(payload.getAddress());
        System.out.println();
        contact.setContactType(CustomerTypeEnum.valueOf(payload.getContactType()));
        contact.setEmailId(payload.getEmailId());
        contact.setMobileNo(payload.getMobileNo());
        contact.setName(payload.getName());
    }


    private void checkIfExists(String type, ContactBasicInfo contact) throws Exception {
        if (type.equalsIgnoreCase("create"))
            if (checkIfExistsWithSameNamePhone(contact.getName(), contact.getMobileNo())) {
                throw new Exception("Contact with same name and mobileno exits");
            }
        else if (type.equals("update")) {
            Optional < ContactBasicInfo > contactOpt = contactRepo.findById(contact.getContactId());
            if (!contactOpt.isPresent())
                throw new Exception("Contact not found with ID");
            ContactBasicInfo contactForUpdate = contactOpt.get();
            if (contactForUpdate.getName() != contact.getName() || contactForUpdate.getMobileNo() != contact.getMobileNo()) {
                Boolean isExist = checkIfExistsWithSameNamePhone(contact.getName(), contact.getMobileNo());
                if (isExist)
                    throw new Exception("Contact with same name and mobile no exists");
            }
        }

    }

    private void formatMobileNo(CreateContactData payload) {
        if (!(payload.getContactPersonMobileNo() == null))
            payload.setContactPersonMobileNo(normalizePhoneNumber(payload.getContactPersonMobileNo()));
        payload.setMobileNo(normalizePhoneNumber(payload.getMobileNo()));
    }
    private boolean checkIfExistsWithSameNamePhone(String name, String mobileNo) {
        int count = contactRepo.getCountByNameNo(name, mobileNo);
        if (count > 0)
            return true;
        else
            return false;
    }

    public Page <ContactAllInfo> findAll(Pageable pageable) 
    {
        return allContactsRepo.findAll(pageable);
    }

    public ContactAllInfo findSingleContactFromAll(long id) throws Exception 
    {
        Optional < ContactAllInfo > ContactOpt = allContactsRepo.findById(id);
        if (!ContactOpt.isPresent())
            throw new Exception("Contact not found with ID " + id);
        return ContactOpt.get();
    }

    public ContactBasicInfo findContactById(long id) throws Exception 
    {
        Optional <ContactBasicInfo> ContactOpt = contactRepo.findById(id);
        if (!ContactOpt.isPresent())
            throw new Exception("Contact not found with ID " + id);
        return ContactOpt.get();
    }
    public ContactBasicInfo updateContact(Long id, CreateContactData payload) throws Exception {
        ContactBasicInfo contact = findContactById(id);
        formatMobileNo(payload);
        Boolean isNameMobileChanged = checkIfNameMobilenoChanged(contact, payload);
        populateContactFields(contact, payload);
        if (isNameMobileChanged)
            checkIfExists("update", contact);
        return contactRepo.save(contact);
    }
    private Boolean checkIfNameMobilenoChanged(ContactBasicInfo contact, CreateContactData payload) {
        if (!contact.getName().equalsIgnoreCase(payload.getName()) ||
            !contact.getMobileNo().equals(payload.getMobileNo()))
            return true;
        else
            return false;
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