package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.data.DashboardChartDTO;
import com.ec.application.data.StatusGroupCountDTO;
import com.ec.application.data.TenantCountDTO;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentStatusHistory;
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
    public void logStatusChange(IndentInventory indent, String oldStatus, String newStatus, String changedBy, String changeMessage) {
        IndentStatusHistory h = new IndentStatusHistory();
        h.setIndent(indent);
        h.setOldStatus(oldStatus);   // can be null on creation
        h.setNewStatus(newStatus);   // must not be null
        h.setChangedAt(new Date());
        h.setChangedBy(changedBy);
        h.setChangeMessage(changeMessage);
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
                IndentStatusConstants.STATUS_CLOSED
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
}
