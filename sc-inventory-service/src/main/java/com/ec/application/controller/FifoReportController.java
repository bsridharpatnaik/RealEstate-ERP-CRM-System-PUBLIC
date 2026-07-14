package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.GlobalFifoReport;
import com.ec.application.repository.GlobalFifoReportRepository;
import com.ec.application.service.FifoReportService;
import com.ec.application.service.FifoReportSyncOrchestrator;
import com.ec.application.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.List;

@RestController
@RequestMapping("/fifo-report")
public class FifoReportController {

    @Autowired
    private FifoReportSyncOrchestrator orchestrator;

    @Autowired
    private FifoReportService fifoReportService;

    @Autowired
    private GlobalFifoReportRepository globalFifoReportRepository;

    @Autowired
    private UserDetailsService userDetailsService;

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
            @PageableDefault(page = 0, size = 50,
                sort = {"outwardDate", "outwardId"},
                direction = Sort.Direction.DESC) Pageable pageable)
            throws Exception {
        return fifoReportService.getFiltered(filterDataList, pageable);
    }

    /** Tile stats — counts by period with project breakdown. */
    @UseDefaultTenant
    @GetMapping("/tiles")
    public Map<String, Object> tiles() throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date today = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date from7 = cal.getTime();
        cal.setTime(today); cal.add(Calendar.DAY_OF_YEAR, -30);
        Date from30 = cal.getTime();
        cal.setTime(today); cal.add(Calendar.DAY_OF_YEAR, -90);
        Date from90 = cal.getTime();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("last7Days",  buildTile(globalFifoReportRepository.countTotalSince(from7, allowedSchemas),
                                           globalFifoReportRepository.countByProjectSince(from7, allowedSchemas)));
        result.put("last30Days", buildTile(globalFifoReportRepository.countTotalSince(from30, allowedSchemas),
                                           globalFifoReportRepository.countByProjectSince(from30, allowedSchemas)));
        result.put("last90Days", buildTile(globalFifoReportRepository.countTotalSince(from90, allowedSchemas),
                                           globalFifoReportRepository.countByProjectSince(from90, allowedSchemas)));
        result.put("uniqueProducts", buildTile(globalFifoReportRepository.countDistinctProducts(allowedSchemas),
                                               globalFifoReportRepository.countDistinctProductsByProject(allowedSchemas)));
        return result;
    }

    private Map<String, Object> buildTile(Long total, List<Object[]> byProject) {
        Map<String, Object> tile = new LinkedHashMap<>();
        tile.put("total", total != null ? total : 0L);
        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (Object[] row : byProject) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("project", row[0]);
            entry.put("count", row[1]);
            breakdown.add(entry);
        }
        tile.put("byProject", breakdown);
        return tile;
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
