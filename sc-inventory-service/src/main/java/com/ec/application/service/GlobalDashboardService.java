package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.*;
import com.ec.application.repository.IndentStatusHistoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@UseDefaultTenant
public class GlobalDashboardService {

    @Autowired
    private IndentStatusHistoryService indentStatusHistoryService;

    @Autowired
    private PurchaseOrderStatusHistoryService purchaseOrderStatusHistoryService;

    @Autowired
    IndentInventoryService indentInventoryService;

    @Autowired
    PurchaseOrderService purchaseOrderService;

    @Autowired
    StockSummaryService stockSummaryService;

    /**
     * Builds and returns all dashboard data required by the Global Dashboard
     *
     * @param startDate start of time window (inclusive) for sliced metrics
     * @param endDate   end of time window (inclusive) for sliced metrics
     * @return DashboardChartListDTO
     * Fully populated dashboard object containing:
     * - Historical (time-sliced) KPIs
     * - Live (current-state) KPIs
     */
    public DashboardChartListDTO getAllSlicedData(Date startDate, Date endDate) {

        // --------------------------------------------------------------------
        // 1. TIME-SLICED (HISTORICAL) DASHBOARDS
        // --------------------------------------------------------------------

        // Indent history-based dashboards (grouped by tenant)
        Map<String, DashboardChartDTO> indentSlicedDashboards =
                indentStatusHistoryService.getDashboards(startDate, endDate);

        // Purchase Order history-based dashboards (grouped by firm)
        Map<String, DashboardChartDTO> poSlicedDashboards =
                purchaseOrderStatusHistoryService.getDashboards(startDate, endDate);

        // --------------------------------------------------------------------
        // 2. STATIC / LIVE (CURRENT-STATE) DASHBOARDS
        // --------------------------------------------------------------------

        // Current indent state dashboards (no date filter)
        Map<String, DashboardChartDTO> indentStaticDashboards =
                indentInventoryService.getCurrentIndentDashboards();

        // Current PO state dashboards (no date filter)
        Map<String, DashboardChartDTO> poStaticDashboards =
                purchaseOrderService.getCurrentPODashboards();

        // --------------------------------------------------------------------
        // 3. ASSEMBLE FINAL DASHBOARD RESPONSE
        // --------------------------------------------------------------------
        // IMPORTANT:
        // The order of arguments here maps directly to the UI expectations
        // and the DashboardChartListDTO constructor contract.
        // --------------------------------------------------------------------

        return new DashboardChartListDTO(

                // -------- INDENT (TIME-SLICED / HISTORY) --------
                indentSlicedDashboards.get(IndentStatusConstants.STATUS_NEW),
                indentSlicedDashboards.get(IndentStatusConstants.STATUS_APPROVED),
                indentSlicedDashboards.get(IndentStatusConstants.STATUS_PO_COMPLETED),
                indentSlicedDashboards.get(IndentStatusConstants.STATUS_CLOSED),

                // -------- PO (TIME-SLICED / HISTORY) --------
                poSlicedDashboards.get(POStatusConstants.STATUS_NEW),
                poSlicedDashboards.get(POStatusConstants.STATUS_COMPLETED),
                poSlicedDashboards.get(POStatusConstants.STATUS_SHORT_CLOSED),

                // -------- INDENT (STATIC / CURRENT STATE) --------
                indentStaticDashboards.get(IndentStatusConstants.STATUS_NEW),            // Awaiting Approval
                indentStaticDashboards.get(IndentStatusConstants.STATUS_APPROVED),       // No PO
                indentStaticDashboards.get(IndentStatusConstants.STATUS_PO_PARTIAL),     // PO Partial
                indentStaticDashboards.get(IndentStatusConstants.STATUS_INWARD_PARTIAL), // Inward Partial

                // -------- PO (STATIC / CURRENT STATE) --------
                poStaticDashboards.get(POStatusConstants.STATUS_NEW),     // PO Zero Inward
                poStaticDashboards.get(POStatusConstants.STATUS_PARTIAL)  // PO Partial
        );
    }

    public List<DashboardProductStockDTO> getDashboardProductStock() {
        return stockSummaryService.getDashboardProductStock();
    }

    public DashboardTrendChartDTO getIndentLifecycleTrendLast4Weeks() {
        return indentStatusHistoryService.getIndentLifecycleTrendLast4Weeks();
    }

    public DashboardTrendChartDTO getPoLifecycleTrendLast4Weeks() {
        return purchaseOrderStatusHistoryService.getPoLifecycleTrendLast4Weeks();
    }

    public List<SupplierLeadTimeHeatmapDTO> getSupplierLeadTimeHeatmap(int limit) {
        return purchaseOrderStatusHistoryService.getSupplierLeadTimeHeatmap(limit);
    }
}