package com.ec.application.service;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.repository.IndentStatusHistoryRepo;
import com.ec.application.repository.PurchaseOrderStatusHistoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@UseDefaultTenant
public class PurchaseOrderStatusHistoryService {

    @Autowired
    private PurchaseOrderStatusHistoryRepo purchaseOrderStatusHistoryRepo;

    @Transactional
    public void logStatusChange(
            PurchaseOrder po,
            String oldStatus,
            String newStatus,
            String changedBy,
            String changeMessage,
            List<HistoryRelationInput> relations
    ) {
        PurchaseOrderStatusHistory h = new PurchaseOrderStatusHistory();
        h.setPurchaseOrder(po);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus);
        h.setChangedAt(new Date());
        h.setChangedBy(changedBy);
        h.setChangeMessage(changeMessage);

        if (relations != null && !relations.isEmpty()) {
            for (HistoryRelationInput r : relations) {
                PurchaseOrderStatusHistoryRelation rel =
                        new PurchaseOrderStatusHistoryRelation();
                rel.setPoHistory(h);
                rel.setRelationType(r.getRelationType());
                rel.setTenant(r.getTenant());
                rel.setReferenceId(r.getReferenceId());
                h.getRelations().add(rel);
            }
        }

        purchaseOrderStatusHistoryRepo.save(h);
    }


    @Transactional(readOnly = true)
    public List<PurchaseOrderStatusHistory> getStatusHistoryForPO(String purchaseOrderId) {
        return purchaseOrderStatusHistoryRepo.findByPurchaseOrderIdOrderByIdDesc(purchaseOrderId);
    }

    @Transactional(readOnly = true)
    public Map<String, DashboardChartDTO> getDashboards(Date startDate, Date endDate) {

        List<String> statuses = Arrays.asList(
                POStatusConstants.STATUS_NEW,
                POStatusConstants.STATUS_COMPLETED,
                POStatusConstants.STATUS_SHORT_CLOSED,
                POStatusConstants.STATUS_CANCELLED
        );

        List<StatusGroupCountDTO> rows = purchaseOrderStatusHistoryRepo.fetchPODashboardData(statuses, startDate, endDate);
        Map<String, Map<String, Long>> statusMap = new HashMap<>();
        for (StatusGroupCountDTO row : rows) {
            statusMap
                    .computeIfAbsent(row.getStatus(), k -> new HashMap<>())
                    .merge(row.getGroupKey(), row.getCount(), Long::sum);
        }
        Map<String, DashboardChartDTO> dashboards = new HashMap<>();
        for (String status : statuses) {
            Map<String, Long> firmMap = statusMap.getOrDefault(status, new HashMap<>());
            List<TenantCountDTO> firmCounts = new ArrayList<>();
            long total = 0;

            for (Map.Entry<String, Long> e : firmMap.entrySet()) {
                firmCounts.add(new TenantCountDTO(e.getKey(), e.getValue()));
                total += e.getValue();
            }

            dashboards.put(status,
                    new DashboardChartDTO(total, firmCounts));
        }
        return dashboards;
    }

    @Transactional(readOnly = true)
    public DashboardTrendChartDTO getPoLifecycleTrendLast4Weeks() {

        // 1. Prepare week buckets
        List<WeekBucket> weeks = ReusableMethods.getLast4Weeks();
        Date fromDate = weeks.get(0).getStart();

        // 2. Fetch PO status changes
        List<PoStatusChangeDTO> rows = purchaseOrderStatusHistoryRepo.fetchPoStatusChangesSince(fromDate);

        // 3. Status → weekIndex → count
        Map<String, long[]> statusWeekCounts = new HashMap<String, long[]>();

        String[] trackedStatuses = new String[]{
                POStatusConstants.STATUS_NEW,
                POStatusConstants.STATUS_PARTIAL,
                POStatusConstants.STATUS_COMPLETED,
                POStatusConstants.STATUS_SHORT_CLOSED
        };

        for (String status : trackedStatuses) {
            statusWeekCounts.put(status, new long[4]);
        }

        // 4. Bucket events into weeks
        for (PoStatusChangeDTO row : rows) {

            String status = row.getNewStatus();
            Date changedAt = row.getChangedAt();

            if (!statusWeekCounts.containsKey(status)) {
                continue;
            }

            for (int i = 0; i < weeks.size(); i++) {
                WeekBucket wb = weeks.get(i);
                System.out.println("Checking status " + status + " changedAt " + changedAt + " for week " + wb.getLabel());

                if (!changedAt.before(wb.getStart()) && changedAt.before(wb.getEnd())) {
                    statusWeekCounts.get(status)[i]++;
                    break;
                }
            }
        }

        // 5. Build response
        DashboardTrendChartDTO chart = new DashboardTrendChartDTO();
        chart.setTitle("PO Lifecycle Trend (Last 4 Weeks)");

        List<String> periods = new ArrayList<String>();
        for (WeekBucket wb : weeks) {
            periods.add(wb.getLabel());
        }
        chart.setPeriods(periods);
        List<DashboardTrendSeriesDTO> series = new ArrayList<DashboardTrendSeriesDTO>();
        series.add(buildSeries("Created", statusWeekCounts.get(POStatusConstants.STATUS_NEW)));
        series.add(buildSeries("Partial", statusWeekCounts.get(POStatusConstants.STATUS_PARTIAL)));
        series.add(buildSeries("Completed", statusWeekCounts.get(POStatusConstants.STATUS_COMPLETED)));
        series.add(buildSeries("Short Closed", statusWeekCounts.get(POStatusConstants.STATUS_SHORT_CLOSED)));
        chart.setSeries(series);
        return chart;
    }

    private DashboardTrendSeriesDTO buildSeries(String label, long[] data) {
        List<Long> values = new ArrayList<Long>();
        for (long v : data) {
            values.add(v);
        }
        return new DashboardTrendSeriesDTO(label, values);
    }

    @Transactional(readOnly = true)
    public List<SupplierLeadTimeHeatmapDTO> getSupplierLeadTimeHeatmap(int limit) {

        List<Object[]> rows =
                purchaseOrderStatusHistoryRepo.findAvgLeadTimeBySupplier(
                        POStatusConstants.STATUS_NEW,
                        Arrays.asList(
                                POStatusConstants.STATUS_COMPLETED,
                                POStatusConstants.STATUS_SHORT_CLOSED
                        )
                );

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<SupplierLeadTimeHeatmapDTO> result = new ArrayList<SupplierLeadTimeHeatmapDTO>();

        int count = 0;

        for (Object[] row : rows) {
            Long supplierId = ((Number) row[0]).longValue();
            String supplierName = (String) row[1];
            Number avgSecondsNum = (Number) row[2]; // BigInteger / BigDecimal / Double
            double avgSeconds = avgSecondsNum.doubleValue();
            double avgDays = avgSeconds / (60 * 60 * 24);
            String bucket = classifyBucket(avgDays);
            result.add(new SupplierLeadTimeHeatmapDTO(
                    supplierId,
                    supplierName,
                    round(avgDays, 1),
                    bucket
            ));

            count++;
        }
        return result;
    }

    private String classifyBucket(double days) {

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

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

}
