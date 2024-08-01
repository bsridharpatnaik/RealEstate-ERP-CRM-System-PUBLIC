package com.ec.common.Service;

import com.ec.common.Data.ApiLogReportDTO;
import com.ec.common.Model.ApiLog;
import com.ec.common.Repository.ApiLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiLogService {

    @Autowired
    private ApiLogRepository apiLogRepository;

    @Async
    public void logToDatabase(String tenant, String url, String method, String payload, String username) {
        ApiLog log = new ApiLog();
        log.setTenantName(tenant);
        log.setUrl(url);
        log.setMethod(method);
        log.setPayload(payload);
        log.setUsername(username);
        log.setTenantName(tenant); // Make sure this field is available in ApiLog
        log.setTimestamp(LocalDateTime.now());
        apiLogRepository.save(log);
    }

    public List<ApiLogReportDTO> generateReport() {
        List<ApiLog> logs = apiLogRepository.findAll();

        // Filter out logs with null username or tenantName
        logs = logs.stream()
                .filter(log -> log.getUsername() != null && log.getTenantName() != null)
                .collect(Collectors.toList());

        // Group logs by username and tenant
        Map<String, Map<String, List<ApiLog>>> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(ApiLog::getUsername,
                        Collectors.groupingBy(ApiLog::getTenantName)));

        List<ApiLogReportDTO> reportList = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<ApiLog>>> userEntry : groupedLogs.entrySet()) {
            String username = userEntry.getKey();
            Map<String, List<ApiLog>> tenantMap = userEntry.getValue();

            for (Map.Entry<String, List<ApiLog>> tenantEntry : tenantMap.entrySet()) {
                String tenantName = tenantEntry.getKey();
                List<ApiLog> tenantLogs = tenantEntry.getValue();

                LocalDateTime firstApiCall = tenantLogs.stream()
                        .map(ApiLog::getTimestamp)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

                LocalDateTime lastApiCall = tenantLogs.stream()
                        .map(ApiLog::getTimestamp)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

                Map<String, Long> hourlyReport = generateHourlyReport(tenantLogs);

                ApiLogReportDTO reportDTO = new ApiLogReportDTO();
                reportDTO.setUsername(username);
                reportDTO.setTenantName(tenantName);
                reportDTO.setFirstApiCall(firstApiCall);
                reportDTO.setLastApiCall(lastApiCall);
                reportDTO.setHourlyReport(hourlyReport);

                reportList.add(reportDTO);
            }
        }

        return reportList;
    }

    private Map<String, Long> generateHourlyReport(List<ApiLog> logs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:00-HH:59");
        Map<String, Long> hourlyReport = new LinkedHashMap<>();

        logs.forEach(log -> {
            LocalDateTime timestamp = log.getTimestamp();
            if (timestamp != null) {
                String hourRange = timestamp.format(formatter);
                hourlyReport.put(hourRange, hourlyReport.getOrDefault(hourRange, 0L) + 1);
            }
        });

        return hourlyReport;
    }
}


