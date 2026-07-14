package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.GlobalExpiredStockReport;
import com.ec.application.service.ExpiredStockOrchestrator;
import com.ec.application.service.ExpiredStockReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/expired-stock")
public class ExpiredStockController {

    @Autowired
    private ExpiredStockOrchestrator orchestrator;

    @Autowired
    private ExpiredStockReportService reportService;

    /** Trigger manual sync. */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync() {
        String message = orchestrator.syncAllTenants();
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    /** Paginated filtered list — sorted by daysUntilExpiry ASC (most urgent first). */
    @UseDefaultTenant
    @PostMapping("/list")
    public Page<GlobalExpiredStockReport> list(
            @RequestBody FilterDataList filterDataList,
            @PageableDefault(page = 0, size = 100, sort = "daysUntilExpiry", direction = Sort.Direction.ASC)
            Pageable pageable) throws Exception {
        return reportService.getFiltered(filterDataList, pageable);
    }

    /** Tile counts per expiry window. */
    @UseDefaultTenant
    @GetMapping("/tiles")
    public Map<String, Object> tiles() throws Exception {
        return reportService.getTiles();
    }

    /** Dropdown options for filters. */
    @UseDefaultTenant
    @GetMapping("/dropdowns")
    public Map<String, Object> dropdowns() {
        return reportService.getDropdowns();
    }

    /** Export filtered list to Excel. */
    @UseDefaultTenant
    @PostMapping("/export/excel")
    public void exportExcel(
            @RequestBody FilterDataList filterDataList,
            HttpServletResponse response) throws Exception {
        reportService.exportExcel(filterDataList, response);
    }
}
