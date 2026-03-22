package com.ec.application.service;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.data.*;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentStatusHistory;
import com.ec.application.model.IndentStatusHistoryRelation;
import com.ec.application.repository.IndentStatusHistoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@UseDefaultTenant
public class IndentStatusHistoryService {

    @Autowired
    private IndentStatusHistoryRepo indentStatusHistoryRepo;

    @Transactional
    public void logStatusChange(
            IndentInventory indent,
            String oldStatus,
            String newStatus,
            String changedBy,
            String changeMessage,
            List<HistoryRelationInput> relations
    ) {
        IndentStatusHistory h = new IndentStatusHistory();
        h.setIndent(indent);
        h.setOldStatus(oldStatus);   // can be null on creation
        h.setNewStatus(newStatus);   // must not be null
        h.setChangedAt(new Date());
        h.setChangedBy(changedBy);
        h.setChangeMessage(changeMessage);

        if (relations != null && !relations.isEmpty()) {
            for (HistoryRelationInput r : relations) {
                IndentStatusHistoryRelation rel = new IndentStatusHistoryRelation();
                rel.setHistory(h);
                rel.setRelationType(r.getRelationType());
                rel.setTenant(r.getTenant());
                rel.setReferenceId(r.getReferenceId());
                h.getRelations().add(rel);
            }
        }

        indentStatusHistoryRepo.save(h);
    }

    @Transactional(readOnly = true)
    public List<IndentStatusHistory> getStatusHistoryForIndent(String indentId) {
        return indentStatusHistoryRepo.findByIndent_IndentIdOrderByIdDesc(indentId);
    }

    @Transactional(readOnly = true)
    public Map<String, DashboardChartDTO> getDashboards(Date startDate, Date endDate) {
        List<String> statuses = Arrays.asList(
                IndentStatusConstants.STATUS_NEW,
                IndentStatusConstants.STATUS_APPROVED,
                IndentStatusConstants.STATUS_PO_COMPLETED,
                IndentStatusConstants.STATUS_CLOSED,
                IndentStatusConstants.STATUS_CANCELLED,
                IndentStatusConstants.STATUS_REJECTED
        );

        List<StatusGroupCountDTO> rows = indentStatusHistoryRepo.fetchIndentDashboardData(statuses, startDate, endDate);
        Map<String, Map<String, Long>> statusMap = new HashMap<>();
        for (StatusGroupCountDTO row : rows) {
            statusMap
                    .computeIfAbsent(row.getStatus(), k -> new HashMap<>())
                    .merge(row.getGroupKey(), row.getCount(), Long::sum);
        }

        Map<String, DashboardChartDTO> dashboards = new HashMap<>();

        for (String status : statuses) {
            Map<String, Long> tenantMap = statusMap.getOrDefault(status, new HashMap<>());

            List<TenantCountDTO> tenantCounts = new ArrayList<>();
            long total = 0;

            for (Map.Entry<String, Long> e : tenantMap.entrySet()) {
                tenantCounts.add(new TenantCountDTO(e.getKey(), e.getValue()));
                total += e.getValue();
            }
            dashboards.put(status, new DashboardChartDTO(total, tenantCounts));
        }
        return dashboards;
    }

    @Transactional(readOnly = true)
    public DashboardTrendChartDTO getIndentLifecycleTrendLast4Weeks() {

        // 1. Prepare week buckets
        List<WeekBucket> weeks = ReusableMethods.getLast4Weeks();
        Date fromDate = weeks.get(0).getStart();

        // 2. Fetch all relevant status changes
        List<Object[]> rows = indentStatusHistoryRepo.fetchIndentStatusChangesSince(fromDate);

        // 3. Status → weekIndex → count
        Map<String, long[]> statusWeekCounts = new HashMap<String, long[]>();

        String[] trackedStatuses = new String[]{
                IndentStatusConstants.STATUS_NEW,
                IndentStatusConstants.STATUS_PO_PARTIAL,
                IndentStatusConstants.STATUS_PO_COMPLETED,
                IndentStatusConstants.STATUS_INWARD_PARTIAL,
                IndentStatusConstants.STATUS_CLOSED
        };

        for (String status : trackedStatuses) {
            statusWeekCounts.put(status, new long[4]);
        }

        // 4. Bucket data
        for (Object[] row : rows) {
            String status = (String) row[0];
            Date changedAt = (Date) row[1];

            if (!statusWeekCounts.containsKey(status)) {
                continue;
            }

            for (int i = 0; i < weeks.size(); i++) {
                WeekBucket wb = weeks.get(i);
                if (!changedAt.before(wb.getStart()) && changedAt.before(wb.getEnd())) {
                    statusWeekCounts.get(status)[i]++;
                    break;
                }
            }
        }

        // 5. Build response DTO
        DashboardTrendChartDTO chart = new DashboardTrendChartDTO();
        chart.setTitle("Indent Lifecycle Trend (Last 4 Weeks)");

        List<String> periods = new ArrayList<String>();
        for (WeekBucket wb : weeks) {
            periods.add(wb.getLabel());
        }
        chart.setPeriods(periods);
        List<DashboardTrendSeriesDTO> series = new ArrayList<DashboardTrendSeriesDTO>();
        series.add(buildSeries("Created", statusWeekCounts.get(IndentStatusConstants.STATUS_NEW)));
        series.add(buildSeries("PO Partial", statusWeekCounts.get(IndentStatusConstants.STATUS_PO_PARTIAL)));
        series.add(buildSeries("PO Completed", statusWeekCounts.get(IndentStatusConstants.STATUS_PO_COMPLETED)));
        series.add(buildSeries("Inward Partial", statusWeekCounts.get(IndentStatusConstants.STATUS_INWARD_PARTIAL)));
        series.add(buildSeries("Closed", statusWeekCounts.get(IndentStatusConstants.STATUS_CLOSED)));
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

    private IndentStatusHistoryRelation buildRelation(IndentStatusHistory history, String type, String tenant, String refId) {
        IndentStatusHistoryRelation r = new IndentStatusHistoryRelation();
        r.setHistory(history);
        r.setRelationType(type);
        r.setTenant(tenant);
        r.setReferenceId(refId);
        return r;
    }

}
