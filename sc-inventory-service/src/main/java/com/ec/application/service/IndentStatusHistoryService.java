package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.DashboardChartDTO;
import com.ec.application.data.TenantCountDTO;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentStatusHistory;
import com.ec.application.repository.IndentStatusHistoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

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
    public DashboardChartDTO getIndentCountForDashboard(String status, Date startDate, Date endDate) {
        Long totalCount = indentStatusHistoryRepo.countIndents(status, startDate, endDate);
        List<TenantCountDTO> tenantCounts = indentStatusHistoryRepo.findIndentCountByTenant(status, startDate, endDate);
        return new DashboardChartDTO(totalCount, tenantCounts);
    }
}
