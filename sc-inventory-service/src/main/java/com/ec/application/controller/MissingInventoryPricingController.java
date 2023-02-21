package com.ec.application.controller;

import com.ec.application.model.MissingInventoryPricing;
import com.ec.application.repository.MissingInventoryPricingRepo;
import com.ec.application.service.MissingInventoryPricingService;
import com.ec.common.Filters.FilterDataList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/missingpricing")
public class MissingInventoryPricingController {

    @Autowired
    MissingInventoryPricingService missingInventoryPricingService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<MissingInventoryPricing> findAllMissingInventoryPricing(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return missingInventoryPricingService.findAllMissingInventoryPricing(filterDataList, pageable);
    }
}
