package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.DashboardChartDTO;
import com.ec.application.data.StatusGroupCountDTO;
import com.ec.application.data.TenantCountDTO;
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
}
