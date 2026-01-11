package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.AllMachineriesWithNamesData;
import com.ec.application.model.Firm;
import com.ec.application.repository.FirmRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import reactor.util.StringUtils;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
//@Transactional
@UseDefaultTenant
public class FirmService {

    @Autowired
    FirmRepo firmRepo;

    @Autowired
    CheckBeforeDeleteService checkBeforeDeleteService;

    Logger log = LoggerFactory.getLogger(FirmService.class);

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    private static final Pattern GST_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    public Firm createFirm(Firm payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        validateGstAndPan(payload);
        if (!firmRepo.existsByFirmName(payload.getFirmName().trim())) {
            Firm newFirm = new Firm();
            newFirm.setFirmName(payload.getFirmName());
            newFirm.setFirmDescription(payload.getFirmDescription());
            newFirm.setFirmAddress(payload.getFirmAddress());
            newFirm.setFirmGstNumber(payload.getFirmGstNumber());
            newFirm.setFirmPanNumber(payload.getFirmPanNumber());
            newFirm.setFirmContactNumber(payload.getFirmContactNumber());
            return firmRepo.save(newFirm);
        } else {
            throw new Exception("Firm already exists!");
        }
    }

    private void validatePayload(Firm payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        if (payload.getFirmName() == null || payload.getFirmName().trim() == "")
            throw new Exception("Firm Name cannot be empty!");

    }

    public Firm updateFirm(Long id, Firm payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        validateGstAndPan(payload);
        Firm firmForUpdate = firmRepo.findById(id)
                .orElseThrow(() -> new Exception("Firm not found for the given id"));

        if (!payload.getFirmName().equalsIgnoreCase(firmForUpdate.getFirmName())) {
            if (firmRepo.existsByFirmName(payload.getFirmName())) {
                throw new Exception("Firm with same Name already exists");
            }
            firmForUpdate.setFirmName(payload.getFirmName());
        }

        firmForUpdate.setFirmDescription(payload.getFirmDescription());
        firmForUpdate.setFirmAddress(payload.getFirmAddress());
        firmForUpdate.setFirmGstNumber(payload.getFirmGstNumber());
        firmForUpdate.setFirmPanNumber(payload.getFirmPanNumber());
        firmForUpdate.setFirmContactNumber(payload.getFirmContactNumber());
        return firmRepo.save(firmForUpdate);
    }

    public Firm findSingleFirm(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if(id == null){
            throw new Exception("Firm id cannot be null");
        }
        Firm firm = firmRepo.findById(id)
                .orElseThrow(() -> new Exception("Firm not found for the given id"));
        return firm;
    }

    public List<IdNameProjections> findIdAndNames() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        // TODO Auto-generated method stub
        return firmRepo.findIdAndNames();
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
}
