package com.ec.application.controller;

import com.ec.application.data.InventoryHistoricalStats;
import com.ec.application.service.DashboardServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard/v2")
public class DashboardControllerv2 {

    @Autowired
    DashboardServiceV2 dashboardServiceV2;

    @GetMapping("/iostats")
    public List<InventoryHistoricalStats> getInventoryHistoricalStats(){
        return dashboardServiceV2.getInventoryHistoricalStats();
    }
}
