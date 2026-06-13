package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.GlobalExpiredStockSpecification;
import com.ec.application.model.GlobalExpiredStockReport;
import com.ec.application.repository.GlobalExpiredStockReportRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExpiredStockReportService {

    private final GlobalExpiredStockReportRepository repo;

    public Page<GlobalExpiredStockReport> getFiltered(FilterDataList filterDataList, Pageable pageable) {
        Specification<GlobalExpiredStockReport> spec =
                GlobalExpiredStockSpecification.getSpecification(filterDataList);
        return spec == null ? repo.findAll(pageable) : repo.findAll(spec, pageable);
    }

    /**
     * Tile counts:
     *   expired  — daysUntilExpiry < 0
     *   within1  — daysUntilExpiry <= 1  (includes expired)
     *   within10 — daysUntilExpiry <= 10
     *   within30 — daysUntilExpiry <= 30
     *   within90 — daysUntilExpiry <= 90
     *   total    — all rows
     */
    public Map<String, Object> getTiles() {
        Map<String, Object> result = new HashMap<>();
        result.put("expired",  Optional.ofNullable(repo.countExpired()).orElse(0L));
        result.put("within1",  Optional.ofNullable(repo.countWithinDays(1)).orElse(0L));
        result.put("within10", Optional.ofNullable(repo.countWithinDays(10)).orElse(0L));
        result.put("within30", Optional.ofNullable(repo.countWithinDays(30)).orElse(0L));
        result.put("within90", Optional.ofNullable(repo.countWithinDays(90)).orElse(0L));
        result.put("total",    repo.count());
        return result;
    }

    public Map<String, Object> getDropdowns() {
        Map<String, Object> result = new HashMap<>();
        result.put("categories", repo.findDistinctCategories());
        return result;
    }

    public void exportExcel(FilterDataList filterDataList, HttpServletResponse response) throws Exception {
        Specification<GlobalExpiredStockReport> spec =
                GlobalExpiredStockSpecification.getSpecification(filterDataList);
        List<GlobalExpiredStockReport> rows = spec == null ? repo.findAll() : repo.findAll(spec);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Expired Stock Report");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                "Project", "Product", "Product Code", "Unit", "Category",
                "Warehouse", "Lot #", "Brand", "Received Date", "Expiry Date",
                "Qty Remaining", "Days Until Expiry"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            int rowNum = 1;
            for (GlobalExpiredStockReport r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getTenantSchema() != null ? r.getTenantSchema() : "");
                row.createCell(1).setCellValue(r.getProductName() != null ? r.getProductName() : "");
                row.createCell(2).setCellValue(r.getProductCode() != null ? r.getProductCode() : "");
                row.createCell(3).setCellValue(r.getUnit() != null ? r.getUnit() : "");
                row.createCell(4).setCellValue(r.getCategory() != null ? r.getCategory() : "");
                row.createCell(5).setCellValue(r.getWarehouseName() != null ? r.getWarehouseName() : "");
                row.createCell(6).setCellValue(r.getLotNumber() != null ? r.getLotNumber() : "");
                row.createCell(7).setCellValue(r.getBrand() != null ? r.getBrand() : "");
                row.createCell(8).setCellValue(r.getReceivedDate() != null ? r.getReceivedDate() : "");
                row.createCell(9).setCellValue(r.getExpiryDate() != null ? r.getExpiryDate() : "");
                row.createCell(10).setCellValue(r.getQtyRemaining() != null ? r.getQtyRemaining() : 0.0);
                row.createCell(11).setCellValue(r.getDaysUntilExpiry() != null ? r.getDaysUntilExpiry() : 0);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=expired_stock_report.xlsx");
            wb.write(response.getOutputStream());
        }
    }
}
