package com.ec.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.data.SingleStringData;
import com.ec.application.model.Warehouse;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.WarehouseRepo;

@Service
public class IntersiteMovementService {
    @Autowired
    WarehouseRepo whRepo;

    public List<Warehouse> getAllWarehouses(SingleStringData data) throws Exception {
        if (data.getName() == null)
            throw new Exception("Please provide valid name for tenant");
        ThreadLocalStorage.setTenantName(data.getName());
        return whRepo.findAll();
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
