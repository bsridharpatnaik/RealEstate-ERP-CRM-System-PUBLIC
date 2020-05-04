package com.ec.common.Service;

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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ec.common.Data.ContactNonBasicData;
import com.ec.common.Data.ContactsWithTypeAhead;
import com.ec.common.Data.CustomerTypeEnum;
import com.ec.common.Data.URLRepository;
import com.ec.common.Filters.ContactFilterAttributeEnum;
import com.ec.common.Filters.ContactSpecifications;
import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Model.Address;
import com.ec.common.Model.ContactAllInfo;
import com.ec.common.Model.ContactBasicInfo;
import com.ec.common.Repository.AddressRepo;
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
    AddressRepo addressRepo;

    @Autowired
    HttpServletRequest httpRequest;
    
    @PersistenceContext
    private EntityManager em;

    @Value("${inven.serverurl}")
    private String reqServer;

    CommonUtils utilObj = new CommonUtils();
    
    @Transactional
    public ContactAllInfo createContact(ContactAllInfo payload) throws Exception 
    {
    	ContactAllInfo returnContactAllInfo = new ContactAllInfo();
    	ContactNonBasicData contactNonBasicData = new ContactNonBasicData();
    	Address address = new Address();
    	ContactBasicInfo contact = new ContactBasicInfo();
        formatMobileNo(payload);
        exitIfMobileNoExists(payload);
        PopulateBasicFields(payload, contact, address);
        contactRepo.save(contact);
        
        if (!payload.getContactType().equalsIgnoreCase(CustomerTypeEnum.CUSTOMER.toString())) 
        	contactNonBasicData = fetchNonBasicData(payload,contact);
        else
        {
        	
        }
        
        populateContactForReturn(returnContactAllInfo,contact,contactNonBasicData);
        return returnContactAllInfo;
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
		if(payload.getContactType().toString().equals(CustomerTypeEnum.CUSTOMER) && 
				(!contact.getContactType().toString().equals(CustomerTypeEnum.CUSTOMER)))
			throw new Exception("Cannot convert contact from non-customer to customer");		
		
		PopulateBasicFields(payload, contact, address);
		contactRepo.save(contact);
		ContactNonBasicData contactNonBasicData  = fetchNonBasicData(payload,contact);
		
		populateContactForReturn(returnContactAllInfo,contact,contactNonBasicData);
		return returnContactAllInfo;
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
		Specification<ContactAllInfo> spec = fetchSpecification(contactFilterDataList);
		
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
	private Specification<ContactAllInfo> fetchSpecification(FilterDataList contactFilterDataList) 
	{
		Specification<ContactAllInfo> specification = null;
		for(FilterAttributeData attrData:contactFilterDataList.getFilterData())
		{
			String attrName = attrData.getAttrName();
			List<String> attrValues = attrData.getAttrValue();
			
			if(attrName.toUpperCase().equals(ContactFilterAttributeEnum.NAME.toString()))
			{
				Specification<ContactAllInfo> internalSpecification = null;
				for(String attrValue : attrValues)
				{
					internalSpecification= internalSpecification==null?
							ContactSpecifications.whereNameContains(attrValue)
							:internalSpecification.or(ContactSpecifications.whereNameContains(attrValue));
				}
				specification= specification==null?internalSpecification:specification.and(internalSpecification);
			}
			if(attrName.toUpperCase().equals(ContactFilterAttributeEnum.MOBILENUMBER.toString()))
			{
				Specification<ContactAllInfo> internalSpecification = null;
				for(String attrValue : attrValues)
				{
					internalSpecification= internalSpecification==null?
							ContactSpecifications.whereMobileNoContains(attrValue):
							internalSpecification.or(ContactSpecifications.whereMobileNoContains(attrValue));
				}
				specification= specification==null?internalSpecification:specification.and(internalSpecification);
			}
			
			if(attrName.toUpperCase().equals(ContactFilterAttributeEnum.ADDRESS.toString()))
			{
				Specification<ContactAllInfo> internalSpecification = null;
				for(String attrValue : attrValues)
				{
					Specification<ContactAllInfo> childSpecification = ContactSpecifications.whereAddress1Contains(attrValue)
							.or(ContactSpecifications.whereAddress2Contains(attrValue))
							.or(ContactSpecifications.whereCityContains(attrValue))
							.or(ContactSpecifications.whereStateContains(attrValue))
							.or(ContactSpecifications.whereZipContains(attrValue));
					internalSpecification= internalSpecification==null?childSpecification
							:internalSpecification.or(childSpecification);
				}
				specification= specification==null?internalSpecification:specification.and(internalSpecification);
			}
			
			if(attrName.toUpperCase().equals(ContactFilterAttributeEnum.CONTACTTYPE.toString()))
			{
				Specification<ContactAllInfo> internalSpecification = null;
				attrValues = checkIfContainsAll(attrValues);
	
				for(String attrValue : attrValues)
				{
					internalSpecification= internalSpecification==null?
							ContactSpecifications.whereContactTypeEquals(attrValue)
							:internalSpecification.or(ContactSpecifications.whereContactTypeEquals(attrValue));
				}
				specification= specification==null?internalSpecification:specification.and(internalSpecification);
			}
			
			if(attrName.toUpperCase().equals(ContactFilterAttributeEnum.NAMEORMOBILE.toString()))
				if(specification == null)
					specification = ContactSpecifications.whereNameContains(attrValues.get(0)).or(ContactSpecifications.whereMobileNoContains(attrValues.get(0)));
				
			}
		return specification;
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
}