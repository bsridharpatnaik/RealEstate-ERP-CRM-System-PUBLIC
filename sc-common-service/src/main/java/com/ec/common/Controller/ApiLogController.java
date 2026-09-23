package com.ec.common.Controller;

import com.ec.common.Data.ApiLogReportDTO;
import com.ec.common.Data.UserReportDto;
import com.ec.common.Service.ApiLogService;
import com.ec.common.Service.RecentActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
public class ApiLogController {
    @Autowired
    private ApiLogService apiLogService;

    @Autowired
    private RecentActivityService recentActivityService;

    @GetMapping("/usagehistory")
    public List<ApiLogReportDTO> getApiLogReport() {
        return apiLogService.generateReport();
    }

    @GetMapping("/user")
    public List<UserReportDto> getUserReports() {
        return apiLogService.getUserReports();
    }

    @GetMapping("/clearlog")
    public ResponseEntity<String> clearLogs() {
        apiLogService.cleanupOldLogs();
        return ResponseEntity.ok("API logs cleanup completed successfully");
    }

    /**
     * Live activity feed for the admin "who's active" view. Poll with the lastSeq from the
     * previous response to get only new entries (tail -f style). activeMinutes controls the
     * "currently active users" window.
     */
    @GetMapping("/live-activity")
    public Map<String, Object> getLiveActivity(
            @RequestParam(value = "afterSeq", required = false, defaultValue = "0") long afterSeq,
            @RequestParam(value = "activeMinutes", required = false, defaultValue = "15") int activeMinutes) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("enabled", recentActivityService.isEnabled());
        resp.put("entries", recentActivityService.getSince(afterSeq));
        resp.put("lastSeq", recentActivityService.getLastSeq());
        resp.put("activeUsers", recentActivityService.getActiveUsers(activeMinutes));
        resp.put("serverTime", LocalDateTime.now());
        return resp;
    }
}
