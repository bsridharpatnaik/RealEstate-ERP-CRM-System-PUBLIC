package com.ec.common.Data;

import java.util.List;

public class ApiLogResponseDTO {
    private List<ApiLogReportDTO> reports;

    public List<ApiLogReportDTO> getReports() {
        return reports;
    }

    public void setReports(List<ApiLogReportDTO> reports) {
        this.reports = reports;
    }
}