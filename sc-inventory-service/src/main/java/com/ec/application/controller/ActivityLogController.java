package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.model.ActivityLog;
import com.ec.application.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activity-log")
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @PostMapping("/list")
    @ResponseStatus(HttpStatus.OK)
    public Page<ActivityLog> getActivityLog(
            @RequestBody FilterDataList filterDataList,
            @PageableDefault(page = 0, size = 50, sort = "activityTime", direction = Sort.Direction.DESC) Pageable pageable)
            throws Exception {
        return activityLogService.getFiltered(filterDataList, pageable);
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportActivityLog(@RequestBody FilterDataList filterDataList) throws Exception {
        byte[] excel = activityLogService.exportExcel(filterDataList);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Activity_Log.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
