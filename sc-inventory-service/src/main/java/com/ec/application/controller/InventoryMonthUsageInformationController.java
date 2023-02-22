package com.ec.application.controller;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.data.InventoryLocationUsageDTO;
import com.ec.application.data.InventoryUsagePayload;
import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.service.InventoryMonthUsageInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventoryusage")
public class InventoryMonthUsageInformationController {

    @Autowired
    InventoryMonthUsageInformationService imiService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryLocationUsageDTO> findAllInventoryUsage(@RequestBody InventoryUsagePayload payload, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        return imiService.fetchMonthlyUsageInformation(payload,pageable);
    }

    @GetMapping("/{locationId}")
    @ResponseStatus(HttpStatus.OK)
    public List<String> findDatesByLocation(@PathVariable Long locationId) {
        return imiService.findDatesByLocation(locationId);
    }

    @GetMapping("/dropdown")
    @ResponseStatus(HttpStatus.OK)
    public NameAndProjectionDataForDropDown findLocationDropdown() {
        return imiService.getDropdown();
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }
}
