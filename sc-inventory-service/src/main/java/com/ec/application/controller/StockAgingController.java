package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.GlobalStockAgingDetail;
import com.ec.application.model.GlobalStockAgingReport;
import com.ec.application.repository.GlobalStockAgingDetailRepository;
import com.ec.application.repository.GlobalStockAgingReportRepository;
import com.ec.application.service.StockAgingOrchestrator;
import com.ec.application.service.StockAgingReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/stock-aging")
public class StockAgingController {

    @Autowired
    private StockAgingOrchestrator orchestrator;

    @Autowired
    private StockAgingReportService reportService;

    @Autowired
    private GlobalStockAgingReportRepository agingReportRepo;

    @Autowired
    private GlobalStockAgingDetailRepository agingDetailRepo;

    /** Trigger manual sync. */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync() {
        String message = orchestrator.syncAllTenants();
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    /** Paginated filtered list — master schema. */
    @UseDefaultTenant
    @PostMapping("/list")
    public Page<GlobalStockAgingReport> list(
            @RequestBody FilterDataList filterDataList,
            @PageableDefault(page = 0, size = 20, sort = "minAgingDays", direction = Sort.Direction.DESC)
            Pageable pageable) throws Exception {
        return reportService.getFiltered(filterDataList, pageable);
    }

    /** Per-warehouse detail for a single product — used by popup. */
    @UseDefaultTenant
    @GetMapping("/detail/{productId}")
    public List<GlobalStockAgingDetail> detail(
            @PathVariable Long productId,
            @RequestParam String tenantSchema) {
        return agingDetailRepo.findByTenantSchemaAndProductId(tenantSchema, productId);
    }

    /** Bucket tile counts (all tenants). */
    @UseDefaultTenant
    @GetMapping("/tiles")
    public Map<String, Long> tiles() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("0-30", 0L);
        counts.put("31-60", 0L);
        counts.put("61-90", 0L);
        counts.put("90+", 0L);
        for (Object[] row : agingReportRepo.countByAgingBucket()) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Filter dropdown options. */
    @UseDefaultTenant
    @GetMapping("/dropdowns")
    public Map<String, Object> dropdowns() {
        Map<String, Object> result = new HashMap<>();
        result.put("products", agingReportRepo.findDistinctProductNames());
        result.put("categories", agingReportRepo.findDistinctCategories());
        return result;
    }

    /** Export current filtered view to Excel. */
    @UseDefaultTenant
    @PostMapping("/export/excel")
    public void exportExcel(
            @RequestBody FilterDataList filterDataList,
            HttpServletResponse response) throws Exception {
        reportService.exportExcel(filterDataList, response);
    }
}
