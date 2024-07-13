package com.ec.application.controller;

import com.ec.application.data.StockPercentageForDashboard;
import com.ec.application.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    StockService stockService;

    @GetMapping("/stocknotification")
    @ResponseStatus(HttpStatus.OK)
    public void  sendStockNotification() throws Exception {
        stockService.sendStockNotificationEmail();
    }
}
