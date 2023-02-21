package com.ec.application.controller;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.data.IMPPCreateDTO;
import com.ec.application.data.IMPPDeleteDTO;
import com.ec.application.model.InventoryMonthPriceMapping;
import com.ec.application.service.InventoryMonthPriceMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventorypricing")
public class InventoryMonthPricingController {

    @Autowired
    InventoryMonthPriceMappingService inventoryMonthPriceMappingService;

    @PostMapping("/create")
    @CheckAuthority
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMonthPriceMapping createInventoryMonthPriceMapping(@RequestBody IMPPCreateDTO imppCreateDTO) throws Exception {
        return inventoryMonthPriceMappingService.createInventoryMonthPriceMapping(imppCreateDTO);
    }

    @DeleteMapping(value = "/delete")
    @CheckAuthority
    public ResponseEntity<?> deleteInwardInventoryById(@RequestBody IMPPDeleteDTO imppDeleteDTO) throws Exception {
        inventoryMonthPriceMappingService.deleteInventoryMonthPriceMapping(imppDeleteDTO);
        return ResponseEntity.ok("Entity deleted");
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }
}
