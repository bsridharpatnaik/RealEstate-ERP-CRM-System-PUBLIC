package com.ec.application.controller;

import java.util.List;
import java.util.Map;

import com.ec.application.aspects.CheckAuthority;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.*;
import com.ec.application.model.IndentInventory;
import com.ec.application.service.IndentInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.model.InwardInventory;
import com.ec.application.service.InwardInventoryService;
import com.ec.application.Filters.FilterDataList;

@RestController
@RequestMapping("/indent")
@UseDefaultTenant
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
    public IndentInventory updateIndentInventoryById(@PathVariable String id, @RequestBody IndentInventoryData payload)
            throws Exception {
        return iiService.updateIndentInventory(payload, id);
    }

    @PatchMapping("/{indentId}/split")
    public ResponseEntity<?> splitLineItem(
            @PathVariable String indentId,
            @RequestBody SplitLineItemRequest request) {
        try {
            IndentInventory result = iiService.splitLineItem(indentId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiOnlyMessageAndCodeError(400, e.getMessage()));
        }
    }

    @PatchMapping("/{indentId}/approve")
    public ResponseEntity<?> approveIndent(@PathVariable String indentId) {
        try {
            IndentInventory result = iiService.approveIndentInventory(indentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiOnlyMessageAndCodeError(400, e.getMessage()));
        }
    }

    /**
     * Fetch all PO-eligible indent line items
     * grouped by category across all tenants.
     */
    @GetMapping("/open-indents/by-category")
    public Map<String, List<ConsolidatedIndentLineDTO>> fetchConsolidated(@RequestParam(required = false) String sortBy, @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        return iiService.fetchGroupedByCategory(sortBy, direction);
    }
/*
    @PostMapping("/export")
    @ResponseStatus(HttpStatus.OK)
    public List<InwardInventoryExportDAO2> fetchAllInwardInventoryForExport2(@RequestBody FilterDataList filterDataList)
            throws Exception {

        return iiService.fetchInwardnventoryForExport2(filterDataList);
    }*/

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        return new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
    }
}