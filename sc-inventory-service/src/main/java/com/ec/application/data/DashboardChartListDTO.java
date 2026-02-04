package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class DashboardChartListDTO {
    DashboardChartDTO newIndents;
    DashboardChartDTO approvedIndents;
    DashboardChartDTO poPartialIndents;
    DashboardChartDTO poCompletedIndents;
    DashboardChartDTO closedIndents;

    public DashboardChartListDTO(DashboardChartDTO newIndents, DashboardChartDTO approvedIndents, DashboardChartDTO poPartialIndents, DashboardChartDTO poCompletedIndents, DashboardChartDTO closedIndents) {
        this.newIndents = newIndents;
        this.approvedIndents = approvedIndents;
        this.poPartialIndents = poPartialIndents;
        this.poCompletedIndents = poCompletedIndents;
        this.closedIndents = closedIndents;
    }
}
