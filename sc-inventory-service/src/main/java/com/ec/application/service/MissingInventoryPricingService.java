package com.ec.application.service;

import com.ec.application.model.MissingInventoryPricing;
import com.ec.application.repository.MissingInventoryPricingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class MissingInventoryPricingService {

    @Autowired
    MissingInventoryPricingRepo missingInventoryPricingRepo;


    public Page<MissingInventoryPricing> findAllMissingInventoryPricing(Pageable pageable){
        return missingInventoryPricingRepo.findAll(pageable);
    }
}
