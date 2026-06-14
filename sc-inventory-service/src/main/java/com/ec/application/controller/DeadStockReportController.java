package com.ec.application.controller;

import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.GlobalDeadStockReport;
import com.ec.application.repository.GlobalDeadStockReportRepository;
import com.ec.application.service.DeadStockSyncOrchestrator;
import com.ec.application.service.UserDetailsService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/dead-stock-report")
public class DeadStockReportController {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy");

    @Autowired
    private DeadStockSyncOrchestrator orchestrator;

    @Autowired
    private GlobalDeadStockReportRepository deadStockRepo;

    @Autowired
    private UserDetailsService userDetailsService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync() {
        String message = orchestrator.syncAllTenants();
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    @UseDefaultTenant
    @GetMapping("/tiles")
    public Map<String, Object> tiles() throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        Specification<GlobalDeadStockReport> allowedSpec =
                (root, q, cb) -> root.get("tenantSchema").in(allowedSchemas);
        List<GlobalDeadStockReport> all = deadStockRepo.findAll(allowedSpec);

        long uniqueProducts = all.stream()
                .map(r -> r.getTenantSchema() + "__" + r.getProductId())
                .distinct().count();
        long projectsAffected = all.stream().map(GlobalDeadStockReport::getTenantSchema).distinct().count();
        double totalValue = all.stream()
                .filter(r -> r.getBatchId() == null)
                .filter(r -> r.getLastPoRate() != null)
                .mapToDouble(r -> r.getQty() * r.getLastPoRate())
                .sum();
        long batchTrackedProducts = all.stream()
                .filter(r -> r.getBatchId() != null)
                .map(r -> r.getTenantSchema() + "__" + r.getProductId())
                .distinct().count();

        Map<String, Object> result = new HashMap<>();
        result.put("uniqueProducts", uniqueProducts);
        result.put("projectsAffected", projectsAffected);
        result.put("totalValue", Math.round(totalValue));
        result.put("batchTrackedProducts", batchTrackedProducts);
        return result;
    }

    @UseDefaultTenant
    @GetMapping("/dropdowns")
    public Map<String, Object> dropdowns() {
        List<String> categories = deadStockRepo.findDistinctCategories();
        Map<String, Object> result = new HashMap<>();
        result.put("categories", categories);
        return result;
    }

    @UseDefaultTenant
    @PostMapping("/list")
    public Page<GlobalDeadStockReport> list(
            @RequestBody(required = false) FilterDataList filterDataList,
            @PageableDefault(page = 0, size = 500, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        return deadStockRepo.findAll(buildSpec(filterDataList, allowedSchemas), pageable);
    }

    @UseDefaultTenant
    @PostMapping("/export/excel")
    public void exportExcel(@RequestBody(required = false) FilterDataList filterDataList,
                            HttpServletResponse response) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        List<GlobalDeadStockReport> rows = deadStockRepo.findAll(buildSpec(filterDataList, allowedSchemas),
                Sort.by(Sort.Direction.ASC, "tenantSchema", "productName"));

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Dead Stock Report");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                "Project", "Product", "Product Code", "Category", "Unit",
                "Total Qty (Dead Stock)", "PO Rate (₹)", "Stock Value (₹)",
                "Batch ID", "Lot #", "Brand", "Received Date", "Expiry Date", "Batch Qty"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            int rowNum = 1;
            for (GlobalDeadStockReport r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getTenantSchema() != null ? r.getTenantSchema() : "");
                row.createCell(1).setCellValue(r.getProductName() != null ? r.getProductName() : "");
                row.createCell(2).setCellValue(r.getProductCode() != null ? r.getProductCode() : "");
                row.createCell(3).setCellValue(r.getCategory() != null ? r.getCategory() : "");
                row.createCell(4).setCellValue(r.getUnit() != null ? r.getUnit() : "");
                row.createCell(5).setCellValue(r.getQty() != null ? r.getQty() : 0.0);
                row.createCell(6).setCellValue(r.getLastPoRate() != null ? r.getLastPoRate() : 0.0);
                double value = (r.getQty() != null && r.getLastPoRate() != null)
                        ? r.getQty() * r.getLastPoRate() : 0.0;
                row.createCell(7).setCellValue(value);
                row.createCell(8).setCellValue(r.getBatchId() != null ? r.getBatchId() : 0L);
                row.createCell(9).setCellValue(r.getBatchLotNumber() != null ? r.getBatchLotNumber() : "");
                row.createCell(10).setCellValue(r.getBatchBrand() != null ? r.getBatchBrand() : "");
                row.createCell(11).setCellValue(r.getBatchReceivedDate() != null
                        ? DATE_FMT.format(r.getBatchReceivedDate()) : "");
                row.createCell(12).setCellValue(r.getBatchExpiryDate() != null
                        ? DATE_FMT.format(r.getBatchExpiryDate()) : "");
                row.createCell(13).setCellValue(r.getBatchQtyRemaining() != null ? r.getBatchQtyRemaining() : 0.0);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=dead_stock_report.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    private Specification<GlobalDeadStockReport> buildSpec(FilterDataList filterDataList, List<String> allowedSchemas) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always restrict to user's allowed projects
            if (allowedSchemas != null && !allowedSchemas.isEmpty()) {
                predicates.add(root.get("tenantSchema").in(allowedSchemas));
            }

            if (filterDataList == null || filterDataList.getFilterData() == null)
                return cb.and(predicates.toArray(new Predicate[0]));

            for (FilterAttributeData f : filterDataList.getFilterData()) {
                if (f.getAttrName() == null || f.getAttrValue() == null) continue;
                List<String> values = f.getAttrValue();
                if (values.isEmpty()) continue;

                switch (f.getAttrName()) {
                    case "tenantSchema":
                        predicates.add(root.get("tenantSchema").in(values));
                        break;
                    case "productName":
                        predicates.add(root.get("productName").in(values));
                        break;
                    case "category":
                        predicates.add(root.get("category").in(values));
                        break;
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
