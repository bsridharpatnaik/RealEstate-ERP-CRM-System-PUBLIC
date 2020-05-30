package com.ec.common.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ec.ReusableClasses.CommonUtils;
import com.ec.ReusableClasses.URLRepository;
import com.ec.common.Data.ContactNonBasicData;
import com.ec.common.Data.ContactsWithTypeAhead;
import com.ec.common.Data.CustomerTypeEnum;
import com.ec.common.Filters.ContactSpecifications;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.JWTUtils.ReusableMethods;
import com.ec.common.Model.Address;
import com.ec.common.Model.ContactAllInfo;
import com.ec.common.Model.ContactBasicInfo;
import com.ec.common.Repository.AddressRepo;
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
    AddressRepo addressRepo;

    @Autowired
    HttpServletRequest httpRequest;
    
    @PersistenceContext
    private EntityManager em;

    @Value("${inven.serverurl}")
    private String reqServer;
    
	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;
	
    CommonUtils utilObj = new CommonUtils();
    
    @Transactional
    public ContactAllInfo createContact(ContactAllInfo payload) throws Exception 
    {
    	ContactAllInfo returnContactAllInfo = new ContactAllInfo();
    	ContactNonBasicData contactNonBasicData = new ContactNonBasicData();
    	Address address = new Address();
    	validatePayload(payload);
    	ContactBasicInfo contact = new ContactBasicInfo();
        formatMobileNo(payload);
        exitIfMobileNoExists(payload);
        PopulateBasicFields(payload, contact, address);
        contactRepo.save(contact);
        
        try
        {
        	if (!payload.getContactType().equalsIgnoreCase(CustomerTypeEnum.CUSTOMER.toString())) 
        		contactNonBasicData = fetchNonBasicData(payload,contact);
        	else
        	{
        	
        	}
        }
        catch(Exception e)
        {
        	throw new Exception("Error pushing Supplier/Contractor details to  Inventory system"); 
        }
        
        populateContactForReturn(returnContactAllInfo,contact,contactNonBasicData);
        return returnContactAllInfo;
    }

    private void validatePayload(ContactAllInfo payload) throws Exception 
    {
    	if(!validateRequiredFields(payload).equals(""))
    		throw new Exception("Required fields missing - " + validateRequiredFields(payload));
		
    	if(!ReusableMethods.isValidMobileNumber(payload.getMobileNo()))
    		throw new Exception("Please enter valid mobile number.");
		
    	if(payload.getEmailId()!=null && payload.getEmailId()!="")
    		if(!ReusableMethods.isValidEmail(payload.getEmailId()))
    				throw new Exception("Please enter valid EmailId.");
    	
    	if(payload.getContactPersonMobileNo()!=null && payload.getContactPersonMobileNo()!="")
    		if(!ReusableMethods.isValidMobileNumber(payload.getContactPersonMobileNo()))
    				throw new Exception("Please enter valid Office/Contact Person Mobile Number");
	}

	private String validateRequiredFields(ContactAllInfo payload) 
	{
		String message = "";
		if(payload.getMobileNo()==null || payload.getMobileNo().equals(""))
			message = message==""?"Mobile No.":message+", Mobile No.";
		
		if(payload.getContactType()==null || payload.getContactType().equals(""))
			message = message==""?"Contact Type":message+", Contact Type";
		
		if(payload.getName()==null || payload.getName().equals(""))
			message = message==""?"Contact Name":message+", Contact Name";
		return message;
	}

	private void populateContactForReturn(ContactAllInfo returnContactAllInfo, ContactBasicInfo contact, ContactNonBasicData contactNonBasicData) 
    {
    	returnContactAllInfo.setAddr_line1(contact.getAddress().getAddr_line1());
    	returnContactAllInfo.setAddr_line2(contact.getAddress().getAddr_line2());
    	returnContactAllInfo.setCity(contact.getAddress().getCity());
    	returnContactAllInfo.setContactId(contact.getContactId());
    	returnContactAllInfo.setContactPerson(contactNonBasicData.getContactPerson());
    	returnContactAllInfo.setContactPersonMobileNo(contactNonBasicData.getContactPersonMobileNo());
    	returnContactAllInfo.setContactType(contact.getContactType().toString());
    	returnContactAllInfo.setEmailId(contact.getEmailId());
    	returnContactAllInfo.setGstNumber(contactNonBasicData.getGstNumber());
    	returnContactAllInfo.setMobileNo(contact.getMobileNo());
    	returnContactAllInfo.setName(contact.getName());
    	returnContactAllInfo.setState(contact.getAddress().getState());
    	returnContactAllInfo.setZip(contact.getAddress().getZip());
	}

    @Transactional
	public ContactAllInfo updateContact(Long id, ContactAllInfo payload) throws Exception 
    {
		ContactAllInfo returnContactAllInfo = new ContactAllInfo();
		ContactBasicInfo contact = findContactById(id);
		Address address = contact.getAddress();
		formatMobileNo(payload);
		if(!contact.getMobileNo().equals(payload.getMobileNo()))
				exitIfMobileNoExists(payload);
		checkIfContactTypeModified(contact,payload);
		PopulateBasicFields(payload, contact, address);
		contactRepo.save(contact);
		ContactNonBasicData contactNonBasicData  = fetchNonBasicData(payload,contact);
		
		populateContactForReturn(returnContactAllInfo,contact,contactNonBasicData);
		return returnContactAllInfo;
    }
    
    private void checkIfContactTypeModified(ContactBasicInfo contact, ContactAllInfo payload) throws Exception 
    {
		String oldType = contact.getContactType().toString();
		System.out.println("Existing Contact Type - " + oldType);
		String newType = payload.getContactType();
		System.out.println("Existing Contact Type - " + newType);
		
		if(oldType.equalsIgnoreCase("SUPPLIER") && !newType.equalsIgnoreCase("SUPPLIER"))
		{
			if(checkIfSupplierUsed(contact.getContactId()))
					throw new Exception("Cannot change contact to non-supplier. Supplier already in use in the system");
		}
		
		if(oldType.equalsIgnoreCase("CONTRACTOR") && !newType.equalsIgnoreCase("CONTRACTOR"))
		{
			if(checkIfContractorUsed(contact.getContactId()))
					throw new Exception("Cannot change contact to non-contractor. Contractor already in use in the system");
		}
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
 
	private void PopulateBasicFields(ContactAllInfo payload, ContactBasicInfo contact,Address address) 
    {
		contact.setContactType(CustomerTypeEnum.valueOf(payload.getContactType()));
		contact.setEmailId(payload.getEmailId());
		contact.setMobileNo(payload.getMobileNo());
		contact.setName(payload.getName());
		populateAddressFields(payload,address);
		contact.setAddress(address);
	}

	private void populateAddressFields(ContactAllInfo payload, Address address) 
	{
		address.setAddr_line1(payload.getAddr_line1());
		address.setAddr_line2(payload.getAddr_line2());
		address.setCity(payload.getCity());
		address.setState(payload.getState());
		address.setZip(payload.getZip());
		
		addressRepo.save(address);
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
    	//System.out.println(payload.getContactPersonMobileNo());
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

	public ContactsWithTypeAhead findAllWithTypeAhead(Pageable pageable) 
    {
    	ContactsWithTypeAhead contactsWithTypeAhead = new ContactsWithTypeAhead();
    	contactsWithTypeAhead.setContactNames(fetchContactNamesAndNumbers());
    	contactsWithTypeAhead.setContacts(allContactsRepo.findAll(pageable));
        return contactsWithTypeAhead;
    }
    
	
	public ContactsWithTypeAhead findFilteredContactsWithTA(FilterDataList contactFilterDataList, Pageable pageable) 
	{
		ContactsWithTypeAhead contactsWithTypeAhead = new ContactsWithTypeAhead();
		Specification<ContactAllInfo> spec = ContactSpecifications.getSpecification(contactFilterDataList);
		
		if(spec!=null)
			contactsWithTypeAhead.setContacts(allContactsRepo.findAll(spec, pageable));
		else 		
			contactsWithTypeAhead.setContacts(allContactsRepo.findAll(pageable));

		contactsWithTypeAhead.setContactNames(fetchContactNamesAndNumbers());
		return contactsWithTypeAhead;
	}

	private List<String> fetchContactNamesAndNumbers()
	{
		List<String> namesAndNumbers = allContactsRepo.findContactNames();
    	namesAndNumbers.addAll(allContactsRepo.findContactNumbers());
    	return namesAndNumbers;
	}
	private List<String> checkIfContainsAll(List<String> attrValues) 
	{
		if(attrValues.contains("All"))
		{
			attrValues.clear();
			attrValues.add("CUSTOMER");
			attrValues.add("SUPPLIER");
			attrValues.add("CONTRACTOR");
		}
		return attrValues;
	}

	public Boolean checkIfContactUsed(Long id) throws Exception 
	{
		
		Boolean isUsed = false;
		String url = reqServer + URLRepository.checkContactUsed+id;
		HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", httpRequest.getHeader("Authorization"));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity request = new HttpEntity(headers);
        ResponseEntity<Boolean> response = restTemplate.exchange(url,HttpMethod.GET,request, Boolean.class,1);
        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Request Successful.");
            isUsed = Boolean.valueOf(response.getBody());
        } else {
            throw new Exception("Error occured while checking if contact is used");
        }
        return isUsed;
	}
	
	public Boolean checkIfSupplierUsed(Long id) throws Exception 
	{
		
		Boolean isUsed = false;
		String url = reqServer + URLRepository.isSupplierUsed+id;
		ResponseEntity<Boolean> response = restTemplate.exchange(url,HttpMethod.GET,getRequest(), Boolean.class,1);
        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Request Successful.");
            isUsed = Boolean.valueOf(response.getBody());
        } else {
            throw new Exception("Error occured while checking if contact is used");
        }
        return isUsed;
	}

	public Boolean checkIfContractorUsed(Long id) throws Exception 
	{
		
		Boolean isUsed = false;
		String url = reqServer + URLRepository.isContractorUsed+id;
		ResponseEntity<Boolean> response = restTemplate.exchange(url,HttpMethod.GET,getRequest(), Boolean.class,1);
        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Request Successful.");
            isUsed = Boolean.valueOf(response.getBody());
        } else {
            throw new Exception("Error occured while checking if contact is used");
        }
        return isUsed;
	}
	
	private HttpEntity getRequest() {
		HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", httpRequest.getHeader("Authorization"));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity request  = new HttpEntity(headers);
        return request;
	}

	public void deleteContact(Long id) throws Exception 
	{
		if(checkBeforeDeleteService.checkIfContactIsUsed(id))
			throw new Exception("Cannot delete. Contact already being used in system.");
		else
			contactRepo.softDeleteById(id);
	}
}