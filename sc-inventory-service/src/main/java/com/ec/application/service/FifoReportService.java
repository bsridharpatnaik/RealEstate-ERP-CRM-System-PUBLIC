package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.GlobalFifoReportSpecification;
import com.ec.application.model.GlobalFifoReport;
import com.ec.application.repository.GlobalFifoReportRepository;
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

@Service
@RequiredArgsConstructor
public class FifoReportService {

    private static final SimpleDateFormat DATE_DISPLAY = new SimpleDateFormat("dd-MM-yyyy");

    private final GlobalFifoReportRepository repo;

    public Page<GlobalFifoReport> getFiltered(FilterDataList filterDataList, Pageable pageable) throws ParseException {
        Specification<GlobalFifoReport> spec = GlobalFifoReportSpecification.getSpecification(filterDataList);
        return spec == null
                ? repo.findAll(pageable)
                : repo.findAll(spec, pageable);
    }

    public void exportExcel(FilterDataList filterDataList, HttpServletResponse response) throws Exception {
        Specification<GlobalFifoReport> spec = GlobalFifoReportSpecification.getSpecification(filterDataList);
        List<GlobalFifoReport> rows = spec == null ? repo.findAll() : repo.findAll(spec);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("FIFO Override Report");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                "Date", "Project", "Outward ID", "Product", "Product Code", "Unit",
                "Warehouse", "Structure", "Final Location", "Contractor", "Purpose",
                "Batch ID", "Lot Number", "Brand", "Received Date", "Expiry Date",
                "Qty Consumed", "Override Reason", "Performed By"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rowNum = 1;
            for (GlobalFifoReport r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getOutwardDate() != null ? DATE_DISPLAY.format(r.getOutwardDate()) : "");
                row.createCell(1).setCellValue(r.getTenantSchema() != null ? r.getTenantSchema() : "");
                row.createCell(2).setCellValue(r.getOutwardId() != null ? r.getOutwardId() : 0L);
                row.createCell(3).setCellValue(r.getProductName() != null ? r.getProductName() : "");
                row.createCell(4).setCellValue(r.getProductCode() != null ? r.getProductCode() : "");
                row.createCell(5).setCellValue(r.getMeasurementUnit() != null ? r.getMeasurementUnit() : "");
                row.createCell(6).setCellValue(r.getWarehouseName() != null ? r.getWarehouseName() : "");
                row.createCell(7).setCellValue(r.getUsageLocationName() != null ? r.getUsageLocationName() : "");
                row.createCell(8).setCellValue(r.getUsageAreaName() != null ? r.getUsageAreaName() : "");
                row.createCell(9).setCellValue(r.getContractorName() != null ? r.getContractorName() : "");
                row.createCell(10).setCellValue(r.getPurpose() != null ? r.getPurpose() : "");
                row.createCell(11).setCellValue(r.getBatchId() != null ? r.getBatchId() : 0L);
                row.createCell(12).setCellValue(r.getBatchLotNumber() != null ? r.getBatchLotNumber() : "");
                row.createCell(13).setCellValue(r.getBatchBrand() != null ? r.getBatchBrand() : "");
                row.createCell(14).setCellValue(r.getBatchReceivedDate() != null ? r.getBatchReceivedDate() : "");
                row.createCell(15).setCellValue(r.getBatchExpiryDate() != null ? r.getBatchExpiryDate() : "");
                row.createCell(16).setCellValue(r.getQtyConsumed() != null ? r.getQtyConsumed() : 0.0);
                row.createCell(17).setCellValue(r.getOverrideComment() != null ? r.getOverrideComment() : "");
                row.createCell(18).setCellValue(r.getPerformedBy() != null ? r.getPerformedBy() : "");
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=fifo_override_report.xlsx");
            wb.write(response.getOutputStream());
        }
    }
}
