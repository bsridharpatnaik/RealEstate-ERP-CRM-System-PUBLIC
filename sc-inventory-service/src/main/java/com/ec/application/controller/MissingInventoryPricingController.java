package com.ec.application.controller;

import com.ec.application.model.MissingInventoryPricing;
import com.ec.application.repository.MissingInventoryPricingRepo;
import com.ec.application.service.MissingInventoryPricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/missingpricing")
public class MissingInventoryPricingController {

    @Autowired
    MissingInventoryPricingService missingInventoryPricingService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<MissingInventoryPricing> findAllMissingInventoryPricing(@PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return missingInventoryPricingService.findAllMissingInventoryPricing(pageable);
    }
}
