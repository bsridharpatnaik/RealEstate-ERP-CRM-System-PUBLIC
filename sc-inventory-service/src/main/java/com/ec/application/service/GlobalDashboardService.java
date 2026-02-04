package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.data.DashboardChartDTO;
import com.ec.application.data.DashboardChartListDTO;
import com.ec.application.data.TenantCountDTO;
import com.ec.application.repository.IndentStatusHistoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@UseDefaultTenant
public class GlobalDashboardService {

    @Autowired
    private IndentStatusHistoryService indentStatusHistoryService;

    public DashboardChartListDTO getAllIndentDashboards(Date startDate, Date endDate) {
        DashboardChartDTO createdDashboard = indentStatusHistoryService.getIndentCountForDashboard(IndentStatusConstants.STATUS_NEW, startDate, endDate);
        DashboardChartDTO approvedDashboard = indentStatusHistoryService.getIndentCountForDashboard(IndentStatusConstants.STATUS_APPROVED, startDate, endDate);
        DashboardChartDTO poPartialDashboard = indentStatusHistoryService.getIndentCountForDashboard(IndentStatusConstants.STATUS_PO_PARTIAL, startDate, endDate);
        DashboardChartDTO poCompletedDashboard = indentStatusHistoryService.getIndentCountForDashboard(IndentStatusConstants.STATUS_PO_COMPLETED, startDate, endDate);
        DashboardChartDTO closedDashboard = indentStatusHistoryService.getIndentCountForDashboard(IndentStatusConstants.STATUS_CLOSED, startDate, endDate);
        return new DashboardChartListDTO(createdDashboard, approvedDashboard, poPartialDashboard, poCompletedDashboard, closedDashboard);
    }
}
