package com.ec.application.controller;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.data.*;
import com.ec.application.service.GlobalDashboardService;
import com.ec.application.service.StaleChartsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/global-dashboard")
public class GlobalDashboardController {

    @Autowired
    GlobalDashboardService globalDashboardService;

    @Autowired
    StaleChartsService indentStaleDashboardService;

    @GetMapping("/charts")
    public DashboardChartListDTO getIndentDashboard(@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate, @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {
        return globalDashboardService.getAllSlicedData(startDate, endDate);
    }

    @GetMapping("/products/stock")
    public ResponseEntity<List<DashboardProductStockDTO>> getDashboardProductsStock() {
        List<DashboardProductStockDTO> response = globalDashboardService.getDashboardProductStock();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/indent/trend")
    public ResponseEntity<DashboardTrendChartDTO> getIndentLifecycleTrendLast4Weeks() {
        DashboardTrendChartDTO response = globalDashboardService.getIndentLifecycleTrendLast4Weeks();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/po/trend")
    public ResponseEntity<DashboardTrendChartDTO> getPoLifecycleTrend() {
        return ResponseEntity.ok(globalDashboardService.getPoLifecycleTrendLast4Weeks());
    }

    @GetMapping("/stale-charts")
    public ResponseEntity<StaleChartsDTO> getStaleIndentStackedChart() {
        return ResponseEntity.ok(globalDashboardService.getStaleStackedChart());
    }

    @GetMapping("/suppliers/lead-time/heatmap")
    public ResponseEntity<List<SupplierLeadTimeHeatmapDTO>> getSupplierLeadTimeHeatmap(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(globalDashboardService.getSupplierLeadTimeHeatmap(limit)
        );
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        return new ApiOnlyMessageAndCodeError(500, "Something went wrong while handling data. Contact Administrator.");
    }
}
