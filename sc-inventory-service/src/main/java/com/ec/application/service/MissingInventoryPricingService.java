package com.ec.application.service;

import com.ec.application.model.Machinery;
import com.ec.application.model.MissingInventoryPricing;
import com.ec.application.repository.MissingInventoryPricingRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.MachinerySpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class MissingInventoryPricingService {

    @Autowired
    MissingInventoryPricingRepo missingInventoryPricingRepo;

    Logger log = LoggerFactory.getLogger(MissingInventoryPricingService.class);

    public Page<MissingInventoryPricing> findAllMissingInventoryPricing(FilterDataList filterDataList, Pageable pageable){
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Specification<MissingInventoryPricing> spec = MissingInventoryPricingSpecifications.getSpecification(filterDataList);
        return missingInventoryPricingRepo.findAll(pageable);
    }
}
