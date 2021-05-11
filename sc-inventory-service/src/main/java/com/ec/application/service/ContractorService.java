package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Contractor;
import com.ec.application.repository.ContractorRepo;

@Service
@Transactional
public class ContractorService {
    @Autowired
    ContractorRepo contractorRepo;

    @Autowired
    CheckBeforeDeleteService checkBeforeDeleteService;

    Logger log = LoggerFactory.getLogger(ContractorService.class);

    public List<IdNameProjections> getContractorNames() {
        List<IdNameProjections> contractorNames = new ArrayList<IdNameProjections>();
        contractorNames = contractorRepo.findIdAndNames();
        return contractorNames;
    }

    public Page<Contractor> findAll(Pageable pageable) {
        return contractorRepo.findAll(pageable);
    }

    public boolean isContactUsedAsContractor(Long id) {
        boolean isContactUsed = false;
        if (checkBeforeDeleteService.isContractorUsed(id))
            isContactUsed = true;
        return isContactUsed;
    }
}
