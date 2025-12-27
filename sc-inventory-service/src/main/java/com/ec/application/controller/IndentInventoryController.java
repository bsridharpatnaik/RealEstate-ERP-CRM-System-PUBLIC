package com.ec.application.controller;

import java.util.List;

import com.ec.application.aspects.CheckAuthority;
import com.ec.application.data.*;
import com.ec.application.model.IndentInventory;
import com.ec.application.service.IndentInventoryService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.model.InwardInventory;
import com.ec.application.service.InwardInventoryService;
import com.ec.application.Filters.FilterDataList;

@RestController
@RequestMapping("/indent")
public class IndentInventoryController {

    @Autowired
    IndentInventoryService iiService;

    @PostMapping("/create")
    @CheckAuthority
    @ResponseStatus(HttpStatus.CREATED)
    public IndentInventory createInwardInventory(@RequestBody IndentInventoryData payload) throws Exception {
        return iiService.createIndentInventory(payload);
    }


    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ReturnIndentInventoryData fetchAllInwardInventory(@RequestBody FilterDataList filterDataList,
                                                             @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Direction.DESC) Pageable pageable)
            throws Exception {
        return iiService.fetchIndentInventory(filterDataList, pageable);
    }

    @GetMapping("/{id}")
    public IndentInventory findInwardInventoryById(@PathVariable String id) throws Exception {
        return iiService.findById(id);
    }

    @DeleteMapping(value = "/{id}")
    @CheckAuthority
    public ResponseEntity<?> deleteIndentInventoryById(@PathVariable String id) throws Exception {

        iiService.deleteInwardInventoryById(id);
        return ResponseEntity.ok("Entity deleted");
    }

    @PutMapping("/{id}")
    @CheckAuthority
    public IndentInventory updateInwardInventoryById(@PathVariable String id, @RequestBody IndentInventoryData payload)
            throws Exception {
        return iiService.updateInwardnventory(payload, id);
    }
/*
    @PostMapping("/export")
    @ResponseStatus(HttpStatus.OK)
    public List<InwardInventoryExportDAO2> fetchAllInwardInventoryForExport2(@RequestBody FilterDataList filterDataList)
            throws Exception {

        return iiService.fetchInwardnventoryForExport2(filterDataList);
    }

    @ExceptionHandler(
            {JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        return new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
    }*/
}