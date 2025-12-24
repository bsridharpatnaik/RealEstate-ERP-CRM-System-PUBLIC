package com.ec.application.controller;

import com.ec.application.data.InventoryReportByDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

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
        // Get the current sorting orders
        List<Order> orders = pageable.getSort().stream().collect(Collectors.toList());

        if (orders.isEmpty()) {
            // If no sorting is provided, use the default sorting
            orders = Arrays.asList(
                    Order.desc("date"),
                    Order.desc("type"),
                    Order.asc("keyid")
            );
        } else {
            // Check if sorting by date is specified
            boolean dateAsc = orders.stream().anyMatch(order -> order.getProperty().equals("date") && order.getDirection().isAscending());
            boolean dateDesc = orders.stream().anyMatch(order -> order.getProperty().equals("date") && order.getDirection().isDescending());

            if (dateAsc) {
                orders = Arrays.asList(
                        Order.asc("date"),
                        Order.asc("type"),
                        Order.desc("keyid")
                );
            } else if (dateDesc) {
                orders = Arrays.asList(
                        Order.desc("date"),
                        Order.desc("type"),
                        Order.asc("keyid")
                );
            } else {
                // If no date sorting is found, use the default sorting
                orders = Arrays.asList(
                        Order.desc("date"),
                        Order.desc("type"),
                        Order.asc("keyid")
                );
            }
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }
}