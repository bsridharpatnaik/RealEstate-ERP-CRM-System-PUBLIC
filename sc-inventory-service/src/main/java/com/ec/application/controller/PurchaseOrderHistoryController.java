package com.ec.application.controller;

import com.ec.application.data.PoLineRateHistoryDTO;
import com.ec.application.data.PreviousPurchaseRateDTO;
import com.ec.application.data.PriceScatterPointDTO;
import com.ec.application.service.PurchaseOrderHistoryPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrderHistoryController {

    @Autowired
    private PurchaseOrderHistoryPriceService historyService;

    @GetMapping("/previous-rates")
    public List<PreviousPurchaseRateDTO> getPreviousRates(@RequestParam("productId") Long productId) {
        return historyService.getPreviousRates(productId);
    }

    @GetMapping("/price-trend/scatter")
    public List<PriceScatterPointDTO> getScatterTrend(@RequestParam("productId") Long productId) {
        return historyService.getScatterTrend(productId);
    }

    @GetMapping("/po-rates")
    public List<PoLineRateHistoryDTO> getRatesForPO(@RequestParam("poNumber") String poNumber) {
        return historyService.getRatesForAllProductsInPO(poNumber);
    }
}
