package com.ec.common.Data;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiLogReportDTO {
    private String username;
    private String tenantName;
    private LocalDateTime firstApiCall;
    private LocalDateTime lastApiCall;
    private Map<String, Long> hourlyReport; // Key: Hour range, Value: Number of calls

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public LocalDateTime getFirstApiCall() {
        return firstApiCall;
    }

    public void setFirstApiCall(LocalDateTime firstApiCall) {
        this.firstApiCall = firstApiCall;
    }

    public LocalDateTime getLastApiCall() {
        return lastApiCall;
    }

    public void setLastApiCall(LocalDateTime lastApiCall) {
        this.lastApiCall = lastApiCall;
    }

    public Map<String, Long> getHourlyReport() {
        return hourlyReport;
    }

    public void setHourlyReport(Map<String, Long> hourlyReport) {
        this.hourlyReport = hourlyReport;
    }
}

