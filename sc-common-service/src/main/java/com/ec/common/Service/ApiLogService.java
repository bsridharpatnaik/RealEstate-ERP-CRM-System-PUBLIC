package com.ec.common.Service;

import com.ec.common.Data.ApiLogReportDTO;
import com.ec.common.Data.DateReportDto;
import com.ec.common.Data.HourlyCountDto;
import com.ec.common.Data.UserReportDto;
import com.ec.common.Model.ApiLog;
import com.ec.common.Repository.ApiLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
public class ApiLogService {

    @Autowired
    private ApiLogRepository apiLogRepository;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public List<UserReportDto> getUserReports() {
        List<ApiLog> allLogs = apiLogRepository.findAll();

        // Normalize usernames: Capitalize first letter and lowercase the rest
        Map<String, List<ApiLog>> logsByUser = allLogs.stream()
                .collect(Collectors.groupingBy(log -> capitalizeUsername(log.getUsername())));

        // Sort usernames alphabetically
        List<Map.Entry<String, List<ApiLog>>> sortedEntries = new ArrayList<>(logsByUser.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        List<UserReportDto> userReports = new ArrayList<>();

        for (Map.Entry<String, List<ApiLog>> entry : sortedEntries) {
            String username = entry.getKey(); // Formatted username
            List<ApiLog> userLogs = entry.getValue();

            // Find first and last API call times
            LocalDateTime firstCall = userLogs.stream()
                    .map(ApiLog::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            LocalDateTime lastCall = userLogs.stream()
                    .map(ApiLog::getTimestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            // Format first and last call times
            String formattedFirstCall = formatTime(firstCall);
            String formattedLastCall = formatTime(lastCall);

            // Group logs by date
            Map<LocalDate, List<ApiLog>> logsByDate = userLogs.stream()
                    .collect(Collectors.groupingBy(log -> log.getTimestamp().toLocalDate()));

            List<DateReportDto> dateReports = new ArrayList<>();

            for (Map.Entry<LocalDate, List<ApiLog>> dateEntry : logsByDate.entrySet()) {
                LocalDate date = dateEntry.getKey();
                List<ApiLog> logsOnDate = dateEntry.getValue();

                // Group logs by hour and count occurrences
                Map<String, Long> hourlyCounts = logsOnDate.stream()
                        .collect(Collectors.groupingBy(
                                log -> formatHourRange(log.getTimestamp()),
                                Collectors.counting()
                        ));

                // Prepare hourly count DTOs
                List<HourlyCountDto> hourlyCountDtos = hourlyCounts.entrySet().stream()
                        .map(e -> new HourlyCountDto(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());

                // Prepare date report DTO
                DateReportDto dateReport = new DateReportDto(
                        date.format(dateFormatter),
                        formattedFirstCall,
                        formattedLastCall,
                        hourlyCountDtos
                );
                dateReports.add(dateReport);
            }

            userReports.add(new UserReportDto(username, dateReports));
        }

        return userReports;
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.toLocalTime().format(timeFormatter);
    }

    private String formatHourRange(LocalDateTime dateTime) {
        LocalTime hourStart = dateTime.toLocalTime().withMinute(0).withSecond(0).withNano(0);
        LocalTime hourEnd = hourStart.plusHours(1);
        return String.format("%s - %s", hourStart.format(DateTimeFormatter.ofPattern("h a")), hourEnd.format(DateTimeFormatter.ofPattern("h a")));
    }

    private String capitalizeUsername(String username) {
        if (username == null || username.isEmpty()) return username;
        return username.substring(0, 1).toUpperCase() + username.substring(1).toLowerCase();
    }

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


