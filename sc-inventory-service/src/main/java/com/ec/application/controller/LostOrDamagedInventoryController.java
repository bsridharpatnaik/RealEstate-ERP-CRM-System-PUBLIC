package com.ec.application.controller;

import java.text.ParseException;

import com.ec.application.aspects.CheckAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.data.CreateLostOrDamagedInventoryData;
import com.ec.application.data.LostDamagedReturnData;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.service.LostDamagedInventoryService;
import com.ec.application.service.PopulateDropdownService;
import com.ec.application.Filters.FilterDataList;

@RestController
@RequestMapping("/lostdamaged")
public class LostOrDamagedInventoryController {
    @Autowired
    LostDamagedInventoryService lostDamagedInventoryService;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public LostDamagedReturnData returnAllLostDamaged(@RequestBody FilterDataList filterDataList,
                                                      @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Direction.DESC) Pageable pageable)
            throws ParseException {
        return lostDamagedInventoryService.findFiilteredostDamagedList(filterDataList, pageable);
    }

    @PostMapping("/create")
    @CheckAuthority
    @ResponseStatus(HttpStatus.CREATED)
    public LostDamagedInventory createLDInventory(@RequestBody CreateLostOrDamagedInventoryData payload)
            throws Exception {

        return lostDamagedInventoryService.createData(payload);
    }

    @GetMapping("/{id}")
    public LostDamagedInventory findLDInventoryByID(@PathVariable long id) throws Exception {
        return lostDamagedInventoryService.findById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLDInventoryByID(@PathVariable long id) {
        // Edit is intentionally removed. Correct a mistake by adding the opposite entry
        // (EXCESS_FOUND corrects a LOST_DAMAGED and vice-versa).
        return ResponseEntity.status(org.springframework.http.HttpStatus.GONE)
                .body("Edit is not supported for Lost/Damaged entries. Add an opposite entry to correct a mistake.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLDInventoryByID(@PathVariable long id) {
        // Delete is intentionally removed. Correct a mistake by adding the opposite entry.
        return ResponseEntity.status(org.springframework.http.HttpStatus.GONE)
                .body("Delete is not supported for Lost/Damaged entries. Add an opposite entry to correct a mistake.");
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
