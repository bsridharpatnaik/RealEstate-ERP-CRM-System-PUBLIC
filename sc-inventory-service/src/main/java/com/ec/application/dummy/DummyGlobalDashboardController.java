package com.ec.application.dummy;

import com.ec.application.data.*;
import com.ec.application.dummy.DummyGlobalDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * Dummy Global Dashboard Controller for UI Development
 *
 * This controller mimics the real GlobalDashboardController but returns
 * randomly generated data. All endpoints are prefixed with "dummy-" for easy identification.
 *
 * Usage:
 * - UI team can use these endpoints to build and test the dashboard
 * - Once dashboard is ready, simply switch from /dummy-global-dashboard to /global-dashboard
 * - All response structures match the real API exactly
 */
@RestController
@RequestMapping("/dummy-global-dashboard")
public class DummyGlobalDashboardController {

    @Autowired
    DummyGlobalDashboardService dummyGlobalDashboardService;

    /**
     * Get all dashboard charts with dummy data
     *
     * @param startDate Start date for filtering (format: dd-MM-yyyy)
     * @param endDate End date for filtering (format: dd-MM-yyyy)
     * @return DashboardChartListDTO with random data
     */
    @GetMapping("/charts")
    public DashboardChartListDTO getDummyIndentDashboard(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {
        return dummyGlobalDashboardService.getAllSlicedData(startDate, endDate);
    }

    /**
     * Get product stock information with dummy data
     *
     * @return List of DashboardProductStockDTO with random stock data
     */
    @GetMapping("/products/stock")
    public ResponseEntity<List<DashboardProductStockDTO>> getDummyDashboardProductsStock() {
        List<DashboardProductStockDTO> response = dummyGlobalDashboardService.getDashboardProductStock();
        return ResponseEntity.ok(response);
    }

    /**
     * Get indent lifecycle trend for last 4 weeks with dummy data
     *
     * @return DashboardTrendChartDTO with random trend data
     */
    @GetMapping("/indent/trend")
    public ResponseEntity<DashboardTrendChartDTO> getDummyIndentLifecycleTrendLast4Weeks() {
        DashboardTrendChartDTO response = dummyGlobalDashboardService.getIndentLifecycleTrendLast4Weeks();
        return ResponseEntity.ok(response);
    }

    /**
     * Get PO lifecycle trend for last 4 weeks with dummy data
     *
     * @return DashboardTrendChartDTO with random trend data
     */
    @GetMapping("/po/trend")
    public ResponseEntity<DashboardTrendChartDTO> getDummyPoLifecycleTrend() {
        return ResponseEntity.ok(dummyGlobalDashboardService.getPoLifecycleTrendLast4Weeks());
    }

    /**
     * Get stale charts (both indent and PO) with dummy data
     *
     * @return StaleChartsDTO with random stale bucket data
     */
    @GetMapping("/stale-charts")
    public ResponseEntity<StaleChartsDTO> getDummyStaleIndentStackedChart() {
        return ResponseEntity.ok(dummyGlobalDashboardService.getStaleStackedChart());
    }

    /**
     * Get supplier lead time heatmap with dummy data
     *
     * @param limit Maximum number of suppliers to return (default: 20)
     * @return List of SupplierLeadTimeHeatmapDTO with random lead time data
     */
    @GetMapping("/suppliers/lead-time/heatmap")
    public ResponseEntity<List<SupplierLeadTimeHeatmapDTO>> getDummySupplierLeadTimeHeatmap(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(dummyGlobalDashboardService.getSupplierLeadTimeHeatmap(limit));
    }
}