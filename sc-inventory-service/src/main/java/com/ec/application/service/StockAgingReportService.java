package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.GlobalStockAgingSpecification;
import com.ec.application.model.GlobalStockAgingReport;
import com.ec.application.repository.GlobalStockAgingReportRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class StockAgingReportService {

    private static final SimpleDateFormat DATE_DISPLAY = new SimpleDateFormat("dd-MM-yyyy");

    private final GlobalStockAgingReportRepository repo;
    private final UserDetailsService userDetailsService;

    public Page<GlobalStockAgingReport> getFiltered(FilterDataList filterDataList, Pageable pageable) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        Specification<GlobalStockAgingReport> spec = GlobalStockAgingSpecification.getSpecification(filterDataList, allowedSchemas);
        // Always exclude soft-deleted rows
        Specification<GlobalStockAgingReport> notDeleted = (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
        Specification<GlobalStockAgingReport> combined = spec == null ? notDeleted : spec.and(notDeleted);
        return repo.findAll(combined, pageable);
    }

    public void exportExcel(FilterDataList filterDataList, HttpServletResponse response) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        Specification<GlobalStockAgingReport> spec = GlobalStockAgingSpecification.getSpecification(filterDataList, allowedSchemas);
        Specification<GlobalStockAgingReport> notDeleted = (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
        Specification<GlobalStockAgingReport> combined = spec == null ? notDeleted : spec.and(notDeleted);
        List<GlobalStockAgingReport> rows = repo.findAll(combined);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Stock Aging Report");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                "Project", "Product", "Product Code", "Unit", "Category",
                "Qty in Hand", "Last Inward Date", "Aging (Days)", "Aging Bucket",
                "POG (₹)", "Last PO Rate (₹)", "Last PO Date"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rowNum = 1;
            for (GlobalStockAgingReport r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getTenantSchema() != null ? r.getTenantSchema() : "");
                row.createCell(1).setCellValue(r.getProductName() != null ? r.getProductName() : "");
                row.createCell(2).setCellValue(r.getProductCode() != null ? r.getProductCode() : "");
                row.createCell(3).setCellValue(r.getUnit() != null ? r.getUnit() : "");
                row.createCell(4).setCellValue(r.getCategory() != null ? r.getCategory() : "");
                row.createCell(5).setCellValue(r.getTotalQtyInHand() != null ? r.getTotalQtyInHand() : 0.0);
                row.createCell(6).setCellValue(r.getLastInwardDate() != null
                        ? DATE_DISPLAY.format(r.getLastInwardDate()) : "No Inward");
                row.createCell(7).setCellValue(r.getMinAgingDays() != null ? r.getMinAgingDays() : 0);
                row.createCell(8).setCellValue(r.getAgingBucket() != null ? r.getAgingBucket() : "");
                row.createCell(9).setCellValue(r.getPog() != null ? r.getPog() : 0.0);
                row.createCell(10).setCellValue(r.getLastPoRate() != null ? r.getLastPoRate() : 0.0);
                row.createCell(11).setCellValue(r.getLastPoDate() != null
                        ? DATE_DISPLAY.format(r.getLastPoDate()) : "");
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=stock_aging_report.xlsx");
            wb.write(response.getOutputStream());
        }
    }
}
