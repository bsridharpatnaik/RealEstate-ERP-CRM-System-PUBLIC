package com.ec.application.service;

import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import com.ec.application.aspects.UseDefaultTenant;
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
import com.ec.application.Filters.ContactSpecifications;
import com.ec.application.Filters.FilterDataList;

@Service
@Transactional
@UseDefaultTenant
public class ContactService {

    @Autowired ContactInfoRepo contactRepo;
    @Autowired CheckBeforeDeleteService checkBeforeDeleteService;

    private final CommonUtils utilObj = new CommonUtils();
    private static final Set<String> ALLOWED_CONTACT_TYPES = new HashSet<>(Arrays.asList("SUPPLIER", "CONTRACTOR"));
    private static final String OPENING_STOCK_CONTACT_NAME = "OPENING STOCK";
    private final Logger log = LoggerFactory.getLogger(ContactService.class);

    public Contact createContact(Contact payload) throws Exception {
        validatePayload(payload);
        formatMobileNo(payload);
        exitIfMobileNoExists(payload);
        if (payload.getMobileNo() == null)
            exitIfNameExists(payload);
        if (OPENING_STOCK_CONTACT_NAME.equalsIgnoreCase(payload.getName().trim())) {
            payload.setSystemContact(true);
        }
        contactRepo.save(payload);
        return payload;
    }

    public Contact updateContact(Long id, Contact payload) throws Exception {
        Contact existing = findContactById(id);
        validatePayload(payload);
        formatMobileNo(payload);
        if (existing.getMobileNo() != null && !existing.getMobileNo().equals(payload.getMobileNo()))
            exitIfMobileNoExists(payload);
        else if (existing.getMobileNo() == null && !existing.getName().equals(payload.getName()))
            exitIfNameExists(payload);
        checkIfContactTypeModified(existing, payload);
        applyUpdates(payload, existing);
        contactRepo.save(existing);
        return existing;
    }

    public Contact findContactById(long id) throws Exception {
        return contactRepo.findById(id)
                .orElseThrow(() -> new Exception("Contact not found with ID " + id));
    }

    public Page<Contact> findFilteredContactsWithTA(FilterDataList contactFilterDataList, Pageable pageable) {
        Specification<Contact> spec = ContactSpecifications.getSpecification(contactFilterDataList);
        if (spec != null)
            return contactRepo.findAll(spec, pageable);
        else
            return contactRepo.findAll(pageable);
    }

    public void deleteContact(Long id) throws Exception {
        if (checkBeforeDeleteService.isContactUsed(id))
            throw new Exception("Cannot delete. Contact already being used in system.");
        contactRepo.softDeleteById(id);
    }

    public List<String> typeAheadForSearch(String str) {
        return ListUtils.union(
                contactRepo.getAllNamesMatchingName(str),
                contactRepo.getAllNamesMatchingMobile(str)
        );
    }

    public List<String> typeAheadForName(String name) {
        return contactRepo.getAllNamesMatchingName(name);
    }

    private void applyUpdates(Contact payload, Contact existing) {
        existing.setContactType(payload.getContactType());
        existing.setEmailId(payload.getEmailId());
        existing.setMobileNo(payload.getMobileNo());
        existing.setName(payload.getName());
        existing.setAddr_line1(payload.getAddr_line1());
        existing.setAddr_line2(payload.getAddr_line2());
        existing.setCity(payload.getCity());
        existing.setContactPerson(payload.getContactPerson());
        existing.setContactPersonMobileNo(payload.getContactPersonMobileNo());
        existing.setGstNumber(payload.getGstNumber());
        existing.setState(payload.getState());
        existing.setZip(payload.getZip());
        existing.setAccountName(payload.getAccountName());
        existing.setAccountNumber(payload.getAccountNumber());
        existing.setBankName(payload.getBankName());
        existing.setBranchName(payload.getBranchName());
        existing.setIfscCode(payload.getIfscCode());
    }

    private void checkIfContactTypeModified(Contact existing, Contact payload) throws Exception {
        String oldType = existing.getContactType();
        String newType = payload.getContactType();

        if (oldType.equalsIgnoreCase("SUPPLIER") && !newType.equalsIgnoreCase("SUPPLIER"))
            if (checkBeforeDeleteService.isContactUsed(existing.getContactId()))
                throw new Exception("Cannot change contact to non-supplier. Supplier already in use in the system");

        if (oldType.equalsIgnoreCase("CONTRACTOR") && !newType.equalsIgnoreCase("CONTRACTOR"))
            if (checkBeforeDeleteService.isContactUsed(existing.getContactId()))
                throw new Exception("Cannot change contact to non-contractor. Contractor already in use in the system");
    }

    private void validatePayload(Contact payload) throws Exception {
        String missingFields = validateRequiredFields(payload);
        if (!missingFields.isEmpty())
            throw new Exception("Required fields missing - " + missingFields);

        if (payload.getName().length() > 20)
            throw new Exception("Contact name should not be more than 20 characters.");

        if (payload.getMobileNo() != null && !payload.getMobileNo().isEmpty())
            if (!ReusableMethods.isValidMobileNumber(payload.getMobileNo()))
                throw new Exception("Please enter valid mobile number.");

        if (payload.getEmailId() != null && !payload.getEmailId().isEmpty())
            if (!ReusableMethods.isValidEmail(payload.getEmailId()))
                throw new Exception("Please enter valid EmailId.");

        if (payload.getContactPersonMobileNo() != null && !payload.getContactPersonMobileNo().isEmpty())
            if (!ReusableMethods.isValidMobileNumber(payload.getContactPersonMobileNo()))
                throw new Exception("Please enter valid Office/Contact Person Mobile Number");

        if (payload.getZip() != null && !payload.getZip().isEmpty())
            if (!payload.getZip().matches("\\d{6}"))
                throw new Exception("Enter a valid pin code (6 Digits numeric)");

        if (!ALLOWED_CONTACT_TYPES.contains(payload.getContactType().toUpperCase()))
            throw new IllegalArgumentException("contactType must be SUPPLIER or CONTRACTOR");
    }

    private String validateRequiredFields(Contact payload) {
        List<String> missing = new ArrayList<>();
        if (payload.getContactType() == null || payload.getContactType().isEmpty())
            missing.add("Contact Type");
        if (payload.getName() == null || payload.getName().isEmpty())
            missing.add("Contact Name");
        return String.join(", ", missing);
    }

    private void formatMobileNo(Contact payload) {
        if (payload.getContactPersonMobileNo() != null && !payload.getContactPersonMobileNo().isEmpty())
            payload.setContactPersonMobileNo(utilObj.normalizePhoneNumber(payload.getContactPersonMobileNo()));
        if (payload.getMobileNo() != null && !payload.getMobileNo().isEmpty())
            payload.setMobileNo(utilObj.normalizePhoneNumber(payload.getMobileNo()));
    }

    private void exitIfNameExists(Contact payload) throws Exception {
        if (contactRepo.getCountByName(payload.getName()) > 0)
            throw new Exception("Contact already exists by same name without mobile number");
    }

    private void exitIfMobileNoExists(Contact payload) throws Exception {
        if (payload.getMobileNo() != null && contactRepo.getCountByMobileNo(payload.getMobileNo()) > 0)
            throw new Exception("Contact already exists by Mobile Number.");
    }
}