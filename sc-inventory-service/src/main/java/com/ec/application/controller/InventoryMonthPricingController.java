package com.ec.application.controller;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.data.ExistingInventoryPricingDTO;
import com.ec.application.data.IMPPCreateDTO;
import com.ec.application.data.IMPPDeleteDTO;
import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.model.InventoryMonthPriceMapping;
import com.ec.application.model.MissingInventoryPricing;
import com.ec.application.service.InventoryMonthPriceMappingService;
import com.ec.common.Filters.FilterDataList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventorypricing")
public class InventoryMonthPricingController {

    @Autowired
    InventoryMonthPriceMappingService inventoryMonthPriceMappingService;

    @GetMapping("/dropdown")
    public NameAndProjectionDataForDropDown getDropdownValues(){
        return inventoryMonthPriceMappingService.getDropDownValues();
    }

    @PostMapping("/create")
    @CheckAuthority
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMonthPriceMapping createInventoryMonthPriceMapping(@RequestBody IMPPCreateDTO imppCreateDTO) throws Exception {
        return inventoryMonthPriceMappingService.createInventoryMonthPriceMapping(imppCreateDTO);
    }

    @PostMapping(value = "/delete")
    @ResponseStatus(HttpStatus.OK)
    @CheckAuthority
    public ResponseEntity<?> deleteInwardInventoryById(@RequestBody IMPPDeleteDTO imppDeleteDTO) throws Exception {
        inventoryMonthPriceMappingService.deleteInventoryMonthPriceMapping(imppDeleteDTO);
        return ResponseEntity.ok("Entity deleted");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<ExistingInventoryPricingDTO> findAllInventoryMonthPriceMapping(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return inventoryMonthPriceMappingService.findAllInventoryMonthPriceMapping(filterDataList, pageable);
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }
}
