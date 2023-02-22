package com.ec.application.service;

import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.model.Machinery;
import com.ec.application.model.MissingInventoryPricing;
import com.ec.application.repository.MissingInventoryPricingRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.MachinerySpecifications;
import com.ec.common.Filters.MissingInventoryPricingSpecifications;
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

    @Autowired
    PopulateDropdownService populateDropdownService;

    Logger log = LoggerFactory.getLogger(MissingInventoryPricingService.class);

    public NameAndProjectionDataForDropDown getDropDownValues() {
        return populateDropdownService.fetchData("inventoryPricing");
    }

    public Page<MissingInventoryPricing> findAllMissingInventoryPricing(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Specification<MissingInventoryPricing> spec = MissingInventoryPricingSpecifications.getSpecification(filterDataList);
        if (spec != null)
            return missingInventoryPricingRepo.findAll(spec, pageable);
        return missingInventoryPricingRepo.findAll(pageable);
    }
}
