package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.SupplierPerformanceRow;
import com.ec.application.service.SupplierPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/supplier-performance")
@RequiredArgsConstructor
public class SupplierPerformanceController {

    private final SupplierPerformanceService service;

    @UseDefaultTenant
    @PostMapping("/list")
    public Page<SupplierPerformanceRow> list(
            @RequestBody(required = false) FilterDataList filters,
            @PageableDefault(page = 0, size = 50, sort = "onTimeRate", direction = Sort.Direction.ASC) Pageable pageable)
            throws Exception {
        return service.getPage(filters, pageable);
    }

    @UseDefaultTenant
    @PostMapping("/tiles")
    public Map<String, Object> tiles(
            @RequestBody(required = false) FilterDataList filters) throws Exception {
        return service.getSummaryTiles(filters);
    }

    @UseDefaultTenant
    @GetMapping("/{supplierId}/price-comparison")
    public List<Map<String, Object>> priceComparison(@PathVariable Long supplierId) {
        return service.getPriceComparison(supplierId);
    }

    @UseDefaultTenant
    @PostMapping("/export/excel")
    public void exportExcel(
            @RequestBody(required = false) FilterDataList filters,
            HttpServletResponse response) throws Exception {
        service.exportExcel(filters, response);
    }
}
