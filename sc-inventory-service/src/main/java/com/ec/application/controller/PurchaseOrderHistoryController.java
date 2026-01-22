package com.ec.application.controller;

import com.ec.application.data.PreviousPurchaseRateDTO;
import com.ec.application.service.PurchaseOrderHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrderHistoryController {

    @Autowired
    private PurchaseOrderHistoryService historyService;

    @GetMapping("/previous-rates")
    public List<PreviousPurchaseRateDTO> getPreviousRates(@RequestParam("productId") Long productId, @PageableDefault(page = 0, size = 10, sort = "lastModifiedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return historyService.getPreviousRates(productId, pageable);
    }
}
