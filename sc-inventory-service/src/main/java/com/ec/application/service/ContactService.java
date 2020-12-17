package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.CommonUtils;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.model.Contact;
import com.ec.application.repository.ContactInfoRepo;
import com.ec.common.Filters.ContactSpecifications;
import com.ec.common.Filters.FilterDataList;

@Service
@Transactional
public class ContactService
{

	@Autowired
	ContactInfoRepo contactRepo;

	@Autowired
	HttpServletRequest httpRequest;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;

	CommonUtils utilObj = new CommonUtils();

	Logger log = LoggerFactory.getLogger(ContactService.class);

	@Transactional
	public Contact createContact(Contact payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Contact contact = new Contact();
		validateRequiredFields(payload);
		validatePayload(payload);
		formatMobileNo(payload);
		exitIfMobileNoExists(payload);
		PopulateFields(payload, contact);
		contactRepo.save(contact);
		return contact;
	}

	@Transactional
	public Contact updateContact(Long id, Contact payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Contact contact = findContactById(id);
		if (contact == null)
			throw new Exception("Contact not found with id -" + id);
		validatePayload(payload);
		formatMobileNo(payload);
		if (!contact.getMobileNo().equals(payload.getMobileNo()))
			exitIfMobileNoExists(payload);
		checkIfContactTypeModified(contact, payload);
		PopulateFields(payload, contact);
		contactRepo.save(contact);
		return contact;
	}

	public Contact findContactById(long id) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Optional<Contact> ContactOpt = contactRepo.findById(id);
		if (!ContactOpt.isPresent())
			throw new Exception("Contact not found with ID " + id);
		return ContactOpt.get();
	}

	public Page<Contact> findFilteredContactsWithTA(FilterDataList contactFilterDataList, Pageable pageable)
	{

		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Specification<Contact> spec = ContactSpecifications.getSpecification(contactFilterDataList);
		if (spec != null)
			return contactRepo.findAll(spec, pageable);
		else
			return (contactRepo.findAll(pageable));

	}

	public void deleteContact(Long id) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (checkBeforeDeleteService.isContactUsed(id))
			throw new Exception("Cannot delete. Contact already being used in system.");
		else
			contactRepo.softDeleteById(id);
	}

	public List<String> typeAheadForSearch(String str)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<String> finalList = ListUtils.union(contactRepo.getAllNamesMatchingName(str),
				contactRepo.getAllNamesMatchingMobile(str));
		return finalList;
	}

	public List<String> typeAheadForName(String name)
	{
		return contactRepo.getAllNamesMatchingName(name);
	}

	private void checkIfContactTypeModified(Contact contact, Contact payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		String oldType = contact.getContactType().toString();
		String newType = payload.getContactType().toString();

		if (oldType.equalsIgnoreCase("SUPPLIER") && !newType.equalsIgnoreCase("SUPPLIER"))
		{
			if (checkBeforeDeleteService.isContactUsed(contact.getContactId()))
				throw new Exception("Cannot change contact to non-supplier. Supplier already in use in the system");
		}

		if (oldType.equalsIgnoreCase("CONTRACTOR") && !newType.equalsIgnoreCase("CONTRACTOR"))
		{
			if (checkBeforeDeleteService.isContactUsed(contact.getContactId()))
				throw new Exception("Cannot change contact to non-contractor. Contractor already in use in the system");
		}
	}

	private void formatMobileNo(Contact payload)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (!(payload.getContactPersonMobileNo() == null) && !payload.getContactPersonMobileNo().equals(""))
			payload.setContactPersonMobileNo(utilObj.normalizePhoneNumber(payload.getContactPersonMobileNo()));
		if (!payload.getMobileNo().equals(""))
			payload.setMobileNo(utilObj.normalizePhoneNumber(payload.getMobileNo()));
	}

	private void exitIfMobileNoExists(Contact payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (contactRepo.getCountByMobileNo(payload.getMobileNo()) > 0)
			throw new Exception("Contact already exists by Mobile Number.");
	}

	private void validatePayload(Contact payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (!validateRequiredFields(payload).equals(""))
			throw new Exception("Required fields missing - " + validateRequiredFields(payload));
		if (payload.getName().length() > 20)
			throw new Exception("Contact name should not be more than 20 characters.");
		if (!ReusableMethods.isValidMobileNumber(payload.getMobileNo()))
			throw new Exception("Please enter valid mobile number.");

		if (payload.getEmailId() != null && payload.getEmailId() != "")
			if (!ReusableMethods.isValidEmail(payload.getEmailId()))
				throw new Exception("Please enter valid EmailId.");

		if (payload.getContactPersonMobileNo() != null && payload.getContactPersonMobileNo() != "")
			if (!ReusableMethods.isValidMobileNumber(payload.getContactPersonMobileNo()))
				throw new Exception("Please enter valid Office/Contact Person Mobile Number");

		if (payload.getZip() != null && payload.getZip() != "")
		{
			if (!payload.getZip().matches("\\d{6}"))
				throw new Exception("Enter a valid pin code (6 Digits numeric)");
		}
	}

	private void PopulateFields(Contact payload, Contact contact)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		contact.setContactType(payload.getContactType());
		contact.setEmailId(payload.getEmailId());
		contact.setMobileNo(payload.getMobileNo());
		contact.setName(payload.getName());
		contact.setAddr_line1(payload.getAddr_line1());
		contact.setAddr_line2(payload.getAddr_line2());
		contact.setCity(payload.getCity());
		contact.setContactPerson(payload.getContactPerson());
		contact.setContactPersonMobileNo(payload.getContactPersonMobileNo());
		contact.setGstNumber(payload.getGstNumber());
		contact.setState(payload.getState());
		contact.setZip(payload.getZip());
	}

	private String validateRequiredFields(Contact payload)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		String message = "";
		if (payload.getMobileNo() == null || payload.getMobileNo().equals(""))
			message = message == "" ? "Mobile No." : message + ", Mobile No.";

		if (payload.getContactType() == null || payload.getContactType().equals(""))
			message = message == "" ? "Contact Type" : message + ", Contact Type";

		if (payload.getName() == null || payload.getName().equals(""))
			message = message == "" ? "Contact Name" : message + ", Contact Name";
		return message;
	}

}