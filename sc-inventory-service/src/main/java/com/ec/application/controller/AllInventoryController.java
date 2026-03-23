package com.ec.application.controller;

import com.ec.application.data.InventoryReportByDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.data.AllInventoryReturnData;
import com.ec.application.service.AllInventoryService;
import com.ec.application.Filters.FilterDataList;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

@RestController
@RequestMapping("/inventory")
public class AllInventoryController {
    @Autowired
    AllInventoryService allInventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public AllInventoryReturnData fetchAllInwardInventory(@RequestBody FilterDataList filterDataList,
                                                          @PageableDefault(page = 0, size = 10) Pageable pageable) throws Exception {
        Pageable adjustedPageable = adjustSorting(pageable);
        return allInventoryService.fetchAllInventory(filterDataList, adjustedPageable);
    }

    @PostMapping("/report")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryReportByDate> getInventoryReport(@RequestBody FilterDataList filterDataList) throws Exception {
        return allInventoryService.getInventoryReport(filterDataList);
    }

    @PostMapping("/export/excel")
    public void exportToExcel(@RequestBody FilterDataList filterDataList,
                              HttpServletResponse response) throws Exception {
        allInventoryService.exportToExcel(filterDataList, response);
    }

    @GetMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public void updateClosingStock() {
        allInventoryService.updateAllInventoryTable();
        allInventoryService.updateClosingStock();
    }

    @ExceptionHandler(
            {JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }

    private Pageable adjustSorting(Pageable pageable) {
        List<Order> orders = pageable.getSort().stream().collect(Collectors.toList());

        boolean dateAsc = orders.stream().anyMatch(
                order -> order.getProperty().equals("date") && order.getDirection().isAscending()
        );

        if (dateAsc) {
            orders = Arrays.asList(
                    Order.asc("date"),
                    Order.asc("sortOrder"),
                    Order.asc("entryid")
            );
        } else {
            orders = Arrays.asList(
                    Order.desc("date"),
                    Order.desc("sortOrder"),
                    Order.desc("entryid")
            );
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }
}