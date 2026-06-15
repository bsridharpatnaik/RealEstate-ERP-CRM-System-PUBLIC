package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class DashboardChartListDTO {
    DashboardChartDTO newIndents;
    DashboardChartDTO approvedIndents;
    DashboardChartDTO poCompletedIndents;
    DashboardChartDTO closedIndents;
    DashboardChartDTO cancelledIndents;
    DashboardChartDTO poCreated;
    DashboardChartDTO poCompleted;
    DashboardChartDTO poShortClosed;
    DashboardChartDTO poCancelled;
    DashboardChartDTO awaitingApprovalIndents;
    DashboardChartDTO zeroPOIndents;
    DashboardChartDTO partialPOIndents;
    DashboardChartDTO inwardPartialIndents;
    DashboardChartDTO statusNewPO;
    DashboardChartDTO statusPartialPO;
    DashboardChartDTO overduePOLines;


    public DashboardChartListDTO(DashboardChartDTO newIndents, DashboardChartDTO approvedIndents,
                                 DashboardChartDTO poCompletedIndents, DashboardChartDTO closedIndents,
                                 DashboardChartDTO cancelledIndents,
                                 DashboardChartDTO poCreated, DashboardChartDTO poCompleted, DashboardChartDTO poShortClosed,
                                 DashboardChartDTO poCancelled,
                                 DashboardChartDTO awaitingApprovalIndents, DashboardChartDTO zeroPOIndents, DashboardChartDTO partialPOIndents,
                                 DashboardChartDTO inwardPartialIndents, DashboardChartDTO statusNewPO, DashboardChartDTO statusPartialPO,
                                 DashboardChartDTO overduePOLines) {
        this.newIndents = newIndents;
        this.approvedIndents = approvedIndents;
        this.poCompletedIndents = poCompletedIndents;
        this.closedIndents = closedIndents;
        this.cancelledIndents = cancelledIndents;
        this.poCreated = poCreated;
        this.poCompleted = poCompleted;
        this.poShortClosed = poShortClosed;
        this.poCancelled = poCancelled;
        this.awaitingApprovalIndents = awaitingApprovalIndents;
        this.zeroPOIndents = zeroPOIndents;
        this.partialPOIndents = partialPOIndents;
        this.inwardPartialIndents = inwardPartialIndents;
        this.statusNewPO = statusNewPO;
        this.statusPartialPO = statusPartialPO;
        this.overduePOLines = overduePOLines;
    }
}
