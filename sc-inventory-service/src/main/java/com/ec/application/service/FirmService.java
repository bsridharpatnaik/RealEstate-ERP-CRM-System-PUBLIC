package com.ec.application.service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.Firm;
import com.ec.application.repository.FirmRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
@UseDefaultTenant
public class FirmService {

    @Autowired
    private FirmRepo firmRepo;

    private final Logger log = LoggerFactory.getLogger(FirmService.class);

    private static final Pattern PAN_PATTERN =
            Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    private static final Pattern GST_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /* ============================================================
       CREATE
       ============================================================ */

    public Firm createFirm(Firm payload) throws Exception {

        log.info("Invoked - createFirm");

        normalize(payload);
        validatePayload(payload);
        validateGstAndPan(payload);

        if (firmRepo.existsByFirmNameIgnoreCase(payload.getFirmName())) {
            throw new Exception("Firm with same name already exists!");
        }

        return firmRepo.save(payload);
    }

    /* ============================================================
       UPDATE
       ============================================================ */

    public Firm updateFirm(Long id, Firm payload) throws Exception {

        log.info("Invoked - updateFirm");

        if (id == null)
            throw new Exception("Firm id cannot be null");

        normalize(payload);
        validatePayload(payload);
        validateGstAndPan(payload);

        Firm existing = firmRepo.findById(id)
                .orElseThrow(() -> new Exception("Firm not found for the given id"));

        if (!existing.getFirmName().equalsIgnoreCase(payload.getFirmName())) {
            if (firmRepo.existsByFirmNameIgnoreCase(payload.getFirmName())) {
                throw new Exception("Firm with same name already exists!");
            }
        }

        copyFields(existing, payload);

        return firmRepo.save(existing);
    }

    /* ============================================================
       VALIDATION
       ============================================================ */

    private void validatePayload(Firm payload) throws Exception {

        if (payload == null)
            throw new Exception("Firm payload cannot be null");

        List<String> missing = new ArrayList<>();

        if (isBlank(payload.getFirmName()))
            missing.add("Firm Name");

        if (isBlank(payload.getFirmContactNumber()))
            missing.add("Firm Contact Number");

        if (isBlank(payload.getFirmEmail()))
            missing.add("Firm Email");

        if (isBlank(payload.getAddr_line1()))
            missing.add("Address Line 1");

        if (isBlank(payload.getCity()))
            missing.add("City");

        if (isBlank(payload.getState()))
            missing.add("State");

        if (isBlank(payload.getZip()))
            missing.add("Zip");

        if (!missing.isEmpty())
            throw new Exception("Missing required fields: " + String.join(", ", missing));

        // Email validation
        if (!EMAIL_PATTERN.matcher(payload.getFirmEmail()).matches())
            throw new Exception("Invalid Firm Email format");

        validateContactNumbers(payload.getFirmContactNumber());
    }

    private void validateContactNumbers(String contactNumbers) throws Exception {

        String value = contactNumbers.trim();

        // Check number of commas
        long commaCount = value.chars().filter(ch -> ch == ',').count();

        if (commaCount > 1) {
            throw new Exception("Maximum two contact numbers are allowed");
        }

        if (commaCount == 0) {
            // Single number
            if (!ReusableMethods.isValidMobileNumber(value)) {
                throw new Exception("Invalid contact number format");
            }
        } else {
            // Two numbers
            String[] numbers = value.split(",");

            if (numbers.length != 2) {
                throw new Exception("Invalid contact number format");
            }

            String first = numbers[0].trim();
            String second = numbers[1].trim();

            if (first.equals(second)) {
                throw new Exception("Duplicate contact numbers are not allowed");
            }

            if (!ReusableMethods.isValidMobileNumber(first)) {
                throw new Exception("Invalid contact number: " + first);
            }

            if (!ReusableMethods.isValidMobileNumber(second)) {
                throw new Exception("Invalid contact number: " + second);
            }
        }
    }

    private void validateGstAndPan(Firm firm) {

        if (StringUtils.hasText(firm.getFirmPanNumber())
                && !PAN_PATTERN.matcher(firm.getFirmPanNumber()).matches()) {
            throw new RuntimeException("Invalid PAN number format");
        }

        if (StringUtils.hasText(firm.getFirmGstNumber())
                && !GST_PATTERN.matcher(firm.getFirmGstNumber()).matches()) {
            throw new RuntimeException("Invalid GST number format");
        }
    }

    /* ============================================================
       HELPERS
       ============================================================ */

    private void normalize(Firm firm) {

        if (firm == null) return;

        if (firm.getFirmName() != null)
            firm.setFirmName(firm.getFirmName().trim());

        if (firm.getFirmEmail() != null)
            firm.setFirmEmail(firm.getFirmEmail().trim().toLowerCase());

        if (firm.getFirmGstNumber() != null)
            firm.setFirmGstNumber(firm.getFirmGstNumber().trim().toUpperCase());

        if (firm.getFirmPanNumber() != null)
            firm.setFirmPanNumber(firm.getFirmPanNumber().trim().toUpperCase());

        if (firm.getFirmContactNumber() != null)
            firm.setFirmContactNumber(firm.getFirmContactNumber().trim());

        if (firm.getFirmDescription() != null)
            firm.setFirmDescription(firm.getFirmDescription().trim());

        if (firm.getAddr_line1() != null)
            firm.setAddr_line1(firm.getAddr_line1().trim());

        if (firm.getAddr_line2() != null)
            firm.setAddr_line2(firm.getAddr_line2().trim());

        if (firm.getCity() != null)
            firm.setCity(firm.getCity().trim());

        if (firm.getState() != null)
            firm.setState(firm.getState().trim());

        if (firm.getZip() != null)
            firm.setZip(firm.getZip().trim());

        if (firm.getContactPerson() != null)
            firm.setContactPerson(firm.getContactPerson().trim());

        if (firm.getContactPersonMobileNo() != null)
            firm.setContactPersonMobileNo(firm.getContactPersonMobileNo().trim());
    }

    private void copyFields(Firm target, Firm source) {

        target.setFirmName(source.getFirmName());
        target.setFirmDescription(source.getFirmDescription());
        target.setFirmGstNumber(source.getFirmGstNumber());
        target.setFirmPanNumber(source.getFirmPanNumber());
        target.setFirmContactNumber(source.getFirmContactNumber());
        target.setFirmEmail(source.getFirmEmail());
        target.setContactPerson(source.getContactPerson());
        target.setContactPersonMobileNo(source.getContactPersonMobileNo());
        target.setAddr_line1(source.getAddr_line1());
        target.setAddr_line2(source.getAddr_line2());
        target.setCity(source.getCity());
        target.setState(source.getState());
        target.setZip(source.getZip());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /* ============================================================
       READ
       ============================================================ */

    public Firm findSingleFirm(Long id) throws Exception {

        if (id == null)
            throw new Exception("Firm id cannot be null");

        return firmRepo.findById(id)
                .orElseThrow(() -> new Exception("Firm not found for the given id"));
    }

    public List<Firm> getAllFirms() {
        return firmRepo.findAll();
    }

    public List<IdNameProjections> findIdAndNames() {
        return firmRepo.findIdAndNames();
    }
}