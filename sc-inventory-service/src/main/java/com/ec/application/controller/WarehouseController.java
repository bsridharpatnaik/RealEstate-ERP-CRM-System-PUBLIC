package com.ec.application.controller;

import java.text.ParseException;
import java.util.List;

import com.ec.application.aspects.CheckAuthority;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Warehouse;
import com.ec.application.service.WarehouseService;

@RestController
@RequestMapping(value = "/warehouse", produces =
        {"application/json", "text/json"})
public class WarehouseController {

    @Autowired
    WarehouseService warehouseService;

    @Autowired
    TenantService tenantService;

    @GetMapping
    public Page<Warehouse> returnAllWarehouses(
            @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Direction.DESC) Pageable pageable)
            throws ParseException {

        return warehouseService.findAll(pageable);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthority
    public Warehouse createWarehouse(@RequestBody Warehouse payload) throws Exception {

        return warehouseService.createWarehouse(payload);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @CheckAuthority
    public Warehouse updateWarehouses(@PathVariable Long id, @RequestBody Warehouse payload) throws Exception {

        return warehouseService.updateWarehouse(id, payload);
    }

    @GetMapping("/idandnames")
    public List<IdNameProjections> returnIdandNames(@RequestParam(required = false) String tenantName) {
        if (tenantName != null && !tenantName.isEmpty()) {
            ThreadLocalStorage.setTenantName(tenantName);
        }
        return warehouseService.findIdAndNames();
    }

    @ExceptionHandler(
            {JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }
}
