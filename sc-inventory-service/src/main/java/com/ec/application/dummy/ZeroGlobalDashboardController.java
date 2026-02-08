package com.ec.application.dummy;

import com.ec.application.data.*;
import com.ec.application.dummy.ZeroGlobalDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * Zero Global Dashboard Controller for Empty State Testing
 *
 * This controller mimics the real GlobalDashboardController but returns
 * empty/zero data. All endpoints are prefixed with "zero-" for easy identification.
 *
 * Purpose:
 * - Test UI behavior when no data is available
 * - Verify empty state handling
 * - Check for null pointer exceptions
 * - Validate graceful degradation of UI components
 *
 * Usage:
 * - UI team can test empty states using these endpoints
 * - Verify loading states and "no data" messages
 * - Ensure charts/tables handle zero data gracefully
 */
@RestController
@RequestMapping("/zero-global-dashboard")
public class ZeroGlobalDashboardController {

    @Autowired
    ZeroGlobalDashboardService zeroGlobalDashboardService;

    /**
     * Get all dashboard charts with zero/empty data
     *
     * @param startDate Start date for filtering (format: dd-MM-yyyy)
     * @param endDate End date for filtering (format: dd-MM-yyyy)
     * @return DashboardChartListDTO with all zeros and empty lists
     */
    @GetMapping("/charts")
    public DashboardChartListDTO getZeroIndentDashboard(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {
        return zeroGlobalDashboardService.getAllSlicedData(startDate, endDate);
    }

    /**
     * Get product stock information with zero/empty data
     *
     * @return Empty list of DashboardProductStockDTO
     */
    @GetMapping("/products/stock")
    public ResponseEntity<List<DashboardProductStockDTO>> getZeroDashboardProductsStock() {
        List<DashboardProductStockDTO> response = zeroGlobalDashboardService.getDashboardProductStock();
        return ResponseEntity.ok(response);
    }

    /**
     * Get indent lifecycle trend with zero data
     *
     * @return DashboardTrendChartDTO with all zero values
     */
    @GetMapping("/indent/trend")
    public ResponseEntity<DashboardTrendChartDTO> getZeroIndentLifecycleTrendLast4Weeks() {
        DashboardTrendChartDTO response = zeroGlobalDashboardService.getIndentLifecycleTrendLast4Weeks();
        return ResponseEntity.ok(response);
    }

    /**
     * Get PO lifecycle trend with zero data
     *
     * @return DashboardTrendChartDTO with all zero values
     */
    @GetMapping("/po/trend")
    public ResponseEntity<DashboardTrendChartDTO> getZeroPoLifecycleTrend() {
        return ResponseEntity.ok(zeroGlobalDashboardService.getPoLifecycleTrendLast4Weeks());
    }

    /**
     * Get stale charts with zero/empty data
     *
     * @return StaleChartsDTO with empty buckets
     */
    @GetMapping("/stale-charts")
    public ResponseEntity<StaleChartsDTO> getZeroStaleIndentStackedChart() {
        return ResponseEntity.ok(zeroGlobalDashboardService.getStaleStackedChart());
    }

    /**
     * Get supplier lead time heatmap with empty data
     *
     * @param limit Maximum number of suppliers to return (default: 20)
     * @return Empty list of SupplierLeadTimeHeatmapDTO
     */
    @GetMapping("/suppliers/lead-time/heatmap")
    public ResponseEntity<List<SupplierLeadTimeHeatmapDTO>> getZeroSupplierLeadTimeHeatmap(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(zeroGlobalDashboardService.getSupplierLeadTimeHeatmap(limit));
    }
}