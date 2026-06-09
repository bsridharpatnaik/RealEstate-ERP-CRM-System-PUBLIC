package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.GlobalFifoReport;
import com.ec.application.repository.GlobalFifoReportRepository;
import com.ec.application.service.FifoReportService;
import com.ec.application.service.FifoReportSyncOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fifo-report")
public class FifoReportController {

    @Autowired
    private FifoReportSyncOrchestrator orchestrator;

    @Autowired
    private FifoReportService fifoReportService;

    @Autowired
    private GlobalFifoReportRepository globalFifoReportRepository;

    /** Trigger manual sync — same pattern as stock-summary and activity-log. */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync() {
        String message = orchestrator.syncAllTenants();
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    /** Paginated filtered list. */
    @UseDefaultTenant
    @PostMapping("/list")
    public Page<GlobalFifoReport> list(
            @RequestBody FilterDataList filterDataList,
            @PageableDefault(page = 0, size = 20, sort = "outwardDate", direction = Sort.Direction.DESC) Pageable pageable)
            throws Exception {
        return fifoReportService.getFiltered(filterDataList, pageable);
    }

    /** Dropdown options for filter panel — distinct values from master table. */
    @UseDefaultTenant
    @GetMapping("/dropdowns")
    public Map<String, Object> dropdowns() {
        Map<String, Object> result = new HashMap<>();
        result.put("products", globalFifoReportRepository.findDistinctProductNames());
        result.put("contractors", globalFifoReportRepository.findDistinctContractorNames());
        result.put("performedBy", globalFifoReportRepository.findDistinctPerformedBy());
        return result;
    }

    /** Export current filtered view to Excel. */
    @UseDefaultTenant
    @PostMapping("/export/excel")
    public void exportExcel(
            @RequestBody FilterDataList filterDataList,
            HttpServletResponse response) throws Exception {
        fifoReportService.exportExcel(filterDataList, response);
    }
}
