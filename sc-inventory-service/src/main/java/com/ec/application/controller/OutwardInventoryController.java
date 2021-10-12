package com.ec.application.controller;

import java.text.ParseException;
import java.util.List;

import com.ec.application.data.*;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.model.OutwardInventory;
import com.ec.application.service.OutwardInventoryService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/outward")
public class OutwardInventoryController {
    @Autowired
    OutwardInventoryService oiService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public OutwardInventory createOutwardInventory(@RequestBody OutwardInventoryData payload) throws Exception {
        return oiService.createOutwardnventory(payload);
    }

    @GetMapping("/{id}")
    public OutwardInventory getOutwardInventory(@PathVariable Long id) throws Exception {

        return oiService.findOutwardnventory(id);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OutwardInventory setReturnOutwardInventory(@PathVariable Long id,
                                                      @RequestBody ReturnRejectInwardOutwardData rd, @RequestParam String type) throws Exception {
        if (type == null)
            throw new Exception("Please provide type (Return/Reject)");
        return oiService.addReturnEntry(rd, id, type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ReturnOutwardInventoryData fetchAllOutwardInventory(@RequestBody FilterDataList filterDataList,
                                                               @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Direction.DESC) Pageable pageable)
            throws ParseException {
        return oiService.fetchOutwardnventory(filterDataList, pageable);
    }

    @PostMapping("/totals")
    @ResponseStatus(HttpStatus.OK)
    public List<ProductGroupedDAO> fetchAllOutwardInventoryTotals(@RequestBody FilterDataList filterDataList)
            throws Exception {

        return oiService.getTotalsForOutward(filterDataList);
    }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.OK)
    public List<OutwardInventoryExportDAO2> fetchAllOutwardInventoryForExport(
            @RequestBody FilterDataList filterDataList) throws Exception {

        return oiService.fetchInwardnventoryForExport2(filterDataList);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> deleteOutwardInventoryById(@PathVariable Long id) throws Exception {

        oiService.deleteOutwardInventoryById(id);
        return ResponseEntity.ok("Entity deleted");
    }

    @PutMapping("/{id}")
    public OutwardInventory updateOutwardInventory(@RequestBody OutwardInventoryData payload, @PathVariable Long id)
            throws Exception {

        return oiService.updateOutwardnventory(payload, id);
    }

    @ExceptionHandler(
            {JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }
    /*
     *
     *
     *
     * @DeleteMapping(value = "/{id}") public ResponseEntity<?>
     * deleteOutwardInventory(@PathVariable Long id) throws Exception {
     * outwardInventoryService.deleteOutwardnventory(id); return
     * ResponseEntity.ok("Entity deleted"); }
     */
}
