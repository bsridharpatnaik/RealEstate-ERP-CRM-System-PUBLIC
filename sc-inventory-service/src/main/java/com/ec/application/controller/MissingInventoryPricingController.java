package com.ec.application.controller;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.data.NameAndProjectionDataForDropDown;
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
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/missingpricing")
public class MissingInventoryPricingController {

    @Autowired
    MissingInventoryPricingService missingInventoryPricingService;

    @GetMapping("/dropdown")
    public NameAndProjectionDataForDropDown getDropdownValues() {
        return missingInventoryPricingService.getDropDownValues();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<MissingInventoryPricing> findAllMissingInventoryPricing(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return missingInventoryPricingService.findAllMissingInventoryPricing(filterDataList, pageable);
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }
}
