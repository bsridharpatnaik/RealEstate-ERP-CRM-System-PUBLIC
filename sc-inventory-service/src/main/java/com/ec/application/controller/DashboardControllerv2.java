package com.ec.application.controller;

import com.ec.application.data.InventoryHistoricalStats;
import com.ec.application.data.StockPercentageForDashboard;
import com.ec.application.model.InwardInventoryStatsForDashboardV2;
import com.ec.application.model.InwardOutwardTrend;
import com.ec.application.model.OutwardInventoryStatsForDashboardV2;
import com.ec.application.service.DashboardServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/stockstats")
    public List<StockPercentageForDashboard> getStockStats(){
        return dashboardServiceV2.getStockPercentForDashboard();
    }

    @GetMapping("/iotrend")
    public List<InwardOutwardTrend> getIoTrend(){
        return dashboardServiceV2.getInwardOutwardTrend();
    }

    @GetMapping("/inwardstats")
    public Page<InwardInventoryStatsForDashboardV2> getInwardStats(@PageableDefault(page = 0, size = Integer.MAX_VALUE, sort = "avgLeadTime",
            direction = Sort.Direction.DESC) Pageable pageable){
        return dashboardServiceV2.getInwardStats(pageable);
    }

    @GetMapping("/outwardstats")
    public Page<OutwardInventoryStatsForDashboardV2> getOutwardStats(@PageableDefault(page = 0, size = Integer.MAX_VALUE, sort = "totalCount",
            direction = Sort.Direction.DESC) Pageable pageable){
        return dashboardServiceV2.getOutwardStats(pageable);
    }
}
