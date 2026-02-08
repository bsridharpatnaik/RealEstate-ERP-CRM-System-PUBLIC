package com.ec.application.dummy;

import com.ec.application.data.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dummy Global Dashboard Service
 *
 * Generates realistic random data for all dashboard endpoints.
 * This service can be used for UI development and testing without
 * requiring actual database data.
 */
@Service
public class DummyGlobalDashboardService {

    // Real tenant schemas from the system
    private static final String[] TENANTS = {
            "bhaavbhumi", "citycenter", "drgtrdcntr", "iseries", "mhvrtrdcntr", "mnglmcity"
    };

    // Indian construction material suppliers
    private static final String[] SUPPLIERS = {
            "Mahesh Traders", "Pushpa Plumbing Works", "Shri Ganesh Cement Supply",
            "Rajesh Hardware & Sanitary", "Kumar Steel Enterprises", "Balaji Building Materials",
            "Sai Siddhi Suppliers", "Vishwakarma Construction Supply", "Anand Tiles & Marbles",
            "Laxmi Hardware & Electricals"
    };

    // Construction-related products
    private static final String[] PRODUCTS = {
            "Red Bricks", "M Sand", "River Sand", "Cement (50kg)", "Steel TMT Bars",
            "Bathroom Taps", "PVC Pipes", "Granite Tiles", "Paint (20L)", "Plywood Sheets",
            "Electrical Wiring", "Door Frames", "Window Grills", "Ceramic Tiles"
    };

    private static final String[] UNITS = {"PCS", "CFT", "TON", "BAG", "KG", "LTR", "MTR", "SQM", "BUNDLE"};
    private static final String[] WEEK_LABELS = {"W-0", "W-1", "W-2", "W-3"};

    /**
     * Generate complete dashboard chart list with random data
     */
    public DashboardChartListDTO getAllSlicedData(Date startDate, Date endDate) {
        return new DashboardChartListDTO(
                // INDENT (TIME-SLICED / HISTORY)
                generateRandomDashboardChart(50, 200),  // newIndents
                generateRandomDashboardChart(30, 150),  // approvedIndents
                generateRandomDashboardChart(20, 100),  // poCompletedIndents
                generateRandomDashboardChart(40, 180),  // closedIndents

                // PO (TIME-SLICED / HISTORY)
                generateRandomDashboardChart(35, 160),  // poCreated
                generateRandomDashboardChart(25, 120),  // poCompleted
                generateRandomDashboardChart(10, 50),   // poShortClosed

                // INDENT (STATIC / CURRENT STATE)
                generateRandomDashboardChart(15, 80),   // awaitingApprovalIndents
                generateRandomDashboardChart(20, 90),   // zeroPOIndents
                generateRandomDashboardChart(12, 60),   // partialPOIndents
                generateRandomDashboardChart(18, 75),   // inwardPartialIndents

                // PO (STATIC / CURRENT STATE)
                generateRandomDashboardChart(22, 95),   // statusNewPO
                generateRandomDashboardChart(16, 70)    // statusPartialPO
        );
    }

    /**
     * Generate random dashboard product stock data
     * Returns exactly 10 construction-related products
     */
    public List<DashboardProductStockDTO> getDashboardProductStock() {
        List<DashboardProductStockDTO> products = new ArrayList<>();

        // Return exactly 10 products
        for (int i = 0; i < 10; i++) {
            DashboardProductStockDTO dto = new DashboardProductStockDTO();
            dto.setProductId(Long.valueOf(i + 1));
            dto.setProductCode("PROD-" + String.format("%03d", i + 1));
            dto.setProductName(PRODUCTS[i % PRODUCTS.length]);
            dto.setMeasurementUnit(UNITS[random(0, UNITS.length - 1)]);

            List<TenantStockDTO> tenantStocks = new ArrayList<>();
            double totalStock = 0;

            // Use 3-5 tenants per product
            int numTenants = random(3, 5);
            for (int j = 0; j < numTenants; j++) {
                double stock = randomDouble(50, 1000);
                tenantStocks.add(new TenantStockDTO(TENANTS[j], stock));
                totalStock += stock;
            }

            dto.setTenantWiseStock(tenantStocks);
            dto.setTotalStock(totalStock);
            products.add(dto);
        }

        return products;
    }

    /**
     * Generate indent lifecycle trend for last 4 weeks
     */
    public DashboardTrendChartDTO getIndentLifecycleTrendLast4Weeks() {
        List<DashboardTrendSeriesDTO> series = new ArrayList<>();

        series.add(generateTrendSeries("Created", 40, 80));
        series.add(generateTrendSeries("PO Partial", 30, 70));
        series.add(generateTrendSeries("PO Completed", 20, 60));
        series.add(generateTrendSeries("Inward Partial", 25, 65));
        series.add(generateTrendSeries("Closed", 35, 75));

        DashboardTrendChartDTO chart = new DashboardTrendChartDTO();
        chart.setTitle("Indent Lifecycle Trend (Last 4 Weeks)");
        chart.setPeriods(Arrays.asList(WEEK_LABELS));
        chart.setSeries(series);

        return chart;
    }

    /**
     * Generate PO lifecycle trend for last 4 weeks
     */
    public DashboardTrendChartDTO getPoLifecycleTrendLast4Weeks() {
        List<DashboardTrendSeriesDTO> series = new ArrayList<>();

        series.add(generateTrendSeries("Created", 25, 65));
        series.add(generateTrendSeries("Completed", 20, 55));
        series.add(generateTrendSeries("Short Closed", 5, 25));

        DashboardTrendChartDTO chart = new DashboardTrendChartDTO();
        chart.setTitle("PO Lifecycle Trend (Last 4 Weeks)");
        chart.setPeriods(Arrays.asList(WEEK_LABELS));
        chart.setSeries(series);

        return chart;
    }

    /**
     * Generate supplier lead time heatmap
     * Returns exactly 10 construction material suppliers
     */
    public List<SupplierLeadTimeHeatmapDTO> getSupplierLeadTimeHeatmap(int limit) {
        List<SupplierLeadTimeHeatmapDTO> heatmap = new ArrayList<>();

        // Always return 10 suppliers (all available suppliers)
        int count = 10;
        for (int i = 0; i < count; i++) {
            double avgDays = randomDouble(0.5, 15);
            String bucket = classifyLeadTimeBucket(avgDays);

            heatmap.add(new SupplierLeadTimeHeatmapDTO(
                    Long.valueOf(i + 1),
                    SUPPLIERS[i],
                    round(avgDays, 1),
                    bucket
            ));
        }

        return heatmap;
    }

    /**
     * Generate stale charts for both indents and POs
     */
    public StaleChartsDTO getStaleStackedChart() {
        List<IndentStaleBucketChartDTO> indentBuckets = generateIndentStaleBuckets();
        List<POStaleBucketChartDTO> poBuckets = generatePOStaleBuckets();
        return new StaleChartsDTO(indentBuckets, poBuckets);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generate a random dashboard chart with tenant breakdown
     */
    private DashboardChartDTO generateRandomDashboardChart(int minTotal, int maxTotal) {
        List<TenantCountDTO> tenantCounts = new ArrayList<>();
        long total = 0;

        int numTenants = random(2, TENANTS.length);
        for (int i = 0; i < numTenants; i++) {
            long count = random(minTotal / numTenants, maxTotal / numTenants);
            tenantCounts.add(new TenantCountDTO(TENANTS[i], count));
            total += count;
        }

        return new DashboardChartDTO(total, tenantCounts);
    }

    /**
     * Generate a trend series with random values for 4 weeks
     */
    private DashboardTrendSeriesDTO generateTrendSeries(String label, int min, int max) {
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            values.add((long) random(min, max));
        }
        return new DashboardTrendSeriesDTO(label, values);
    }

    /**
     * Generate indent stale bucket data
     */
    private List<IndentStaleBucketChartDTO> generateIndentStaleBuckets() {
        List<IndentStaleBucketChartDTO> buckets = new ArrayList<>();
        String[] bucketNames = {"GT_3_DAYS", "GT_7_DAYS", "GT_15_DAYS", "GT_30_DAYS"};

        for (String bucketName : bucketNames) {
            IndentStaleBucketChartDTO bucket = new IndentStaleBucketChartDTO(bucketName);

            int numTenants = random(2, 4);
            for (int i = 0; i < numTenants; i++) {
                bucket.addTenant(TENANTS[i], (long) random(5, 30));
            }

            buckets.add(bucket);
        }

        return buckets;
    }

    /**
     * Generate PO stale bucket data
     */
    private List<POStaleBucketChartDTO> generatePOStaleBuckets() {
        List<POStaleBucketChartDTO> buckets = new ArrayList<>();
        String[] bucketNames = {"GT_3_DAYS", "GT_7_DAYS", "GT_15_DAYS", "GT_30_DAYS"};

        for (String bucketName : bucketNames) {
            POStaleBucketChartDTO bucket = new POStaleBucketChartDTO(bucketName);

            int numSuppliers = random(2, 5);
            for (int i = 0; i < numSuppliers; i++) {
                bucket.addSupplier(SUPPLIERS[i], (long) random(3, 25));
            }

            buckets.add(bucket);
        }

        return buckets;
    }

    /**
     * Classify lead time into buckets for heatmap coloring
     */
    private String classifyLeadTimeBucket(double days) {
        if (days <= 1) {
            return "WITHIN_1_DAY";
        }
        if (days <= 4) {
            return "TWO_TO_FOUR_DAYS";
        }
        if (days <= 10) {
            return "FOUR_TO_TEN_DAYS";
        }
        return "TEN_PLUS_DAYS";
    }

    /**
     * Generate random integer between min and max (inclusive)
     */
    private int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Generate random double between min and max
     */
    private double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Round double to specified decimal places
     */
    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}