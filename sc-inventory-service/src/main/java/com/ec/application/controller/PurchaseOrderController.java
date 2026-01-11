package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.*;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchase-order")
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping("/create")
    @CheckAuthority
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrder createPurchaseOrder(@RequestBody CreatePoRequest payload) throws Exception {
        return purchaseOrderService.createPurchaseOrder(payload);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ReturnPurchaseOrderData fetchPurchaseOrdersPage(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        return purchaseOrderService.fetchPurchaseOrdersPage(filterDataList, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseOrder findPurchaseOrderByID(@PathVariable String id) throws Exception {
        return purchaseOrderService.findByIdWithDetails(id);
    }
}