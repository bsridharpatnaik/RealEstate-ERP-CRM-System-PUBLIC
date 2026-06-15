package com.ec.application.dummy;

import com.ec.application.data.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Zero Global Dashboard Service
 *
 * Generates empty/zero data for all dashboard endpoints.
 * This service is used for testing UI behavior when no data is available.
 *
 * Purpose:
 * - Test empty state UI components
 * - Verify null-safe rendering
 * - Check "no data available" messages
 * - Validate chart/table behavior with zero values
 */
@Service
public class ZeroGlobalDashboardService {

    private static final String[] WEEK_LABELS = {"W-0", "W-1", "W-2", "W-3"};

    /**
     * Generate complete dashboard chart list with all zeros
     */
    public DashboardChartListDTO getAllSlicedData(Date startDate, Date endDate) {
        return new DashboardChartListDTO(
                // INDENT (TIME-SLICED / HISTORY) - All zeros
                createEmptyDashboardChart(),  // newIndents
                createEmptyDashboardChart(),  // approvedIndents
                createEmptyDashboardChart(),  // poCompletedIndents
                createEmptyDashboardChart(),  // closedIndents
                createEmptyDashboardChart(),  // cancelledIndents

                // PO (TIME-SLICED / HISTORY) - All zeros
                createEmptyDashboardChart(),  // poCreated
                createEmptyDashboardChart(),  // poCompleted
                createEmptyDashboardChart(),  // poShortClosed
                createEmptyDashboardChart(),  // poCancelled

                // INDENT (STATIC / CURRENT STATE) - All zeros
                createEmptyDashboardChart(),  // awaitingApprovalIndents
                createEmptyDashboardChart(),  // zeroPOIndents
                createEmptyDashboardChart(),  // partialPOIndents
                createEmptyDashboardChart(),  // inwardPartialIndents

                // PO (STATIC / CURRENT STATE) - All zeros
                createEmptyDashboardChart(),  // statusNewPO
                createEmptyDashboardChart(),  // statusPartialPO
                createEmptyDashboardChart()   // overduePOLines
        );
    }

    /**
     * Return empty list for product stock
     */
    public List<DashboardProductStockDTO> getDashboardProductStock() {
        return new ArrayList<>();
    }

    /**
     * Generate indent lifecycle trend with all zeros
     */
    public DashboardTrendChartDTO getIndentLifecycleTrendLast4Weeks() {
        List<DashboardTrendSeriesDTO> series = new ArrayList<>();

        series.add(createZeroTrendSeries("Created"));
        series.add(createZeroTrendSeries("PO Partial"));
        series.add(createZeroTrendSeries("PO Completed"));
        series.add(createZeroTrendSeries("Inward Partial"));
        series.add(createZeroTrendSeries("Closed"));

        DashboardTrendChartDTO chart = new DashboardTrendChartDTO();
        chart.setTitle("Indent Lifecycle Trend (Last 4 Weeks)");
        chart.setPeriods(Arrays.asList(WEEK_LABELS));
        chart.setSeries(series);

        return chart;
    }

    /**
     * Generate PO lifecycle trend with all zeros
     */
    public DashboardTrendChartDTO getPoLifecycleTrendLast4Weeks() {
        List<DashboardTrendSeriesDTO> series = new ArrayList<>();

        series.add(createZeroTrendSeries("Created"));
        series.add(createZeroTrendSeries("Completed"));
        series.add(createZeroTrendSeries("Short Closed"));

        DashboardTrendChartDTO chart = new DashboardTrendChartDTO();
        chart.setTitle("PO Lifecycle Trend (Last 4 Weeks)");
        chart.setPeriods(Arrays.asList(WEEK_LABELS));
        chart.setSeries(series);

        return chart;
    }

    /**
     * Return empty list for supplier lead time heatmap
     */
    public List<SupplierLeadTimeHeatmapDTO> getSupplierLeadTimeHeatmap(int limit) {
        return new ArrayList<>();
    }

    /**
     * Generate stale charts with empty buckets
     */
    public StaleChartsDTO getStaleStackedChart() {
        List<IndentStaleBucketChartDTO> indentBuckets = createEmptyIndentStaleBuckets();
        List<POStaleBucketChartDTO> poBuckets = createEmptyPOStaleBuckets();
        return new StaleChartsDTO(indentBuckets, poBuckets);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create an empty dashboard chart with zero count and empty tenant list
     */
    private DashboardChartDTO createEmptyDashboardChart() {
        return new DashboardChartDTO(0L, new ArrayList<>());
    }

    /**
     * Create a trend series with all zero values
     */
    private DashboardTrendSeriesDTO createZeroTrendSeries(String label) {
        List<Long> zeroValues = Arrays.asList(0L, 0L, 0L, 0L);
        return new DashboardTrendSeriesDTO(label, zeroValues);
    }

    /**
     * Generate empty indent stale bucket data
     */
    private List<IndentStaleBucketChartDTO> createEmptyIndentStaleBuckets() {
        List<IndentStaleBucketChartDTO> buckets = new ArrayList<>();
        String[] bucketNames = {"GT_3_DAYS", "GT_7_DAYS", "GT_15_DAYS", "GT_30_DAYS"};

        for (String bucketName : bucketNames) {
            // Create bucket with zero total and no tenant counts
            buckets.add(new IndentStaleBucketChartDTO(bucketName));
        }

        return buckets;
    }

    /**
     * Generate empty PO stale bucket data
     */
    private List<POStaleBucketChartDTO> createEmptyPOStaleBuckets() {
        List<POStaleBucketChartDTO> buckets = new ArrayList<>();
        String[] bucketNames = {"GT_3_DAYS", "GT_7_DAYS", "GT_15_DAYS", "GT_30_DAYS"};

        for (String bucketName : bucketNames) {
            // Create bucket with zero total and no supplier counts
            buckets.add(new POStaleBucketChartDTO(bucketName));
        }

        return buckets;
    }
}