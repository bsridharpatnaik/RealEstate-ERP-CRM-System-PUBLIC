package com.ec.application.controller;

import com.ec.application.aspects.CheckAuthority;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.ConsolidatedIndentLineDTO;
import com.ec.application.data.CreatePoRequest;
import com.ec.application.data.IndentInventoryData;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
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

}