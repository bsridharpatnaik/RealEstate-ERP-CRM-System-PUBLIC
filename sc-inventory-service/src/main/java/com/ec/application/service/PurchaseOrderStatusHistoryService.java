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
    public void logStatusChange(PurchaseOrder po, String oldStatus, String newStatus, String changedBy, String changeMessage) {
        PurchaseOrderStatusHistory h = new PurchaseOrderStatusHistory();
        h.setPurchaseOrder(po);
        h.setOldStatus(oldStatus);   // can be null on creation
        h.setNewStatus(newStatus);   // must not be null
        h.setChangedAt(new Date());
        h.setChangedBy(changedBy);
        h.setChangeMessage(changeMessage);
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
                POStatusConstants.STATUS_SHORT_CLOSED
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


}
