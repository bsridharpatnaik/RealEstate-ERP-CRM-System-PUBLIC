package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.HighValuePoRow;
import com.ec.application.data.PoSpendBreakdownRow;
import com.ec.application.data.PoSpendMonthlyRow;
import com.ec.application.data.PoSpendSummaryDTO;
import com.ec.application.service.PoSpendDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Procurement Spend Dashboard (admin report). All endpoints run against the
 * master schema (@UseDefaultTenant) since POs live there.
 */
@RestController
@RequestMapping("/po-spend")
public class PoSpendDashboardController {

    @Autowired
    private PoSpendDashboardService service;

    @UseDefaultTenant
    @PostMapping("/summary")
    public PoSpendSummaryDTO summary(@RequestBody FilterDataList filters) {
        return service.getSummary(filters);
    }

    @UseDefaultTenant
    @PostMapping("/by-project")
    public List<PoSpendBreakdownRow> byProject(@RequestBody FilterDataList filters) {
        return service.getByProject(filters);
    }

    @UseDefaultTenant
    @PostMapping("/by-supplier")
    public List<PoSpendBreakdownRow> bySupplier(@RequestBody FilterDataList filters) {
        return service.getBySupplier(filters);
    }

    @UseDefaultTenant
    @PostMapping("/by-firm")
    public List<PoSpendBreakdownRow> byFirm(@RequestBody FilterDataList filters) {
        return service.getByFirm(filters);
    }

    @UseDefaultTenant
    @PostMapping("/trend")
    public List<PoSpendMonthlyRow> trend(@RequestBody FilterDataList filters) {
        return service.getTrend(filters);
    }

    /** Paginated PO list. highValueOnly=true restricts to POs above the configured threshold. */
    @UseDefaultTenant
    @PostMapping("/list")
    public Page<HighValuePoRow> list(
            @RequestBody FilterDataList filters,
            @RequestParam(defaultValue = "false") boolean highValueOnly,
            @PageableDefault(page = 0, size = 20, sort = "grandTotal", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.getPoList(filters, highValueOnly, pageable);
    }

    @UseDefaultTenant
    @PostMapping("/export/excel")
    public void exportExcel(@RequestBody FilterDataList filters,
                            @RequestParam(defaultValue = "false") boolean highValueOnly,
                            HttpServletResponse response) throws Exception {
        service.exportExcel(filters, highValueOnly, response);
    }

    @UseDefaultTenant
    @GetMapping("/projects")
    public List<String> projects() {
        return service.getDistinctProjects();
    }

    @UseDefaultTenant
    @GetMapping("/suppliers")
    public List<String> suppliers() {
        return service.getDistinctSuppliers();
    }

    @UseDefaultTenant
    @GetMapping("/firms")
    public List<String> firms() {
        return service.getDistinctFirms();
    }
}
