package com.ec.application.controller;

import com.ec.application.data.DashboardChartDTO;
import com.ec.application.data.DashboardChartListDTO;
import com.ec.application.service.GlobalDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/global-dashboard")
public class GlobalDashboardController {

    @Autowired
    GlobalDashboardService globalDashboardService;

    @GetMapping("/charts")
    public DashboardChartListDTO getIndentDashboard(@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate, @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {
        return globalDashboardService.getAllIndentDashboards(startDate, endDate);
    }
}
