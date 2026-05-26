package com.ec.application.service;

import com.ec.application.data.ProductStockRow;
import com.ec.application.data.ProjectStockEmailData;
import com.ec.application.data.WarehouseStockRow;
import com.ec.application.model.Stock;
import com.ec.application.repository.StockRepo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockEmailReportService {

    @Autowired
    StockRepo stockRepo;

    Logger log = LoggerFactory.getLogger(StockEmailReportService.class);

    /**
     * Collects stock data for the current tenant (ThreadLocalStorage must be set by caller).
     * Splits into non-zero stock rows (with warehouse breakdown) and zero-stock item names.
     */
    public ProjectStockEmailData collectTenantStockData(String tenantName) {
        List<Stock> stocks = stockRepo.findAllActiveStockExcludingDeadStockWarehouse();

        // Group all stock records by productId
        Map<Long, List<Stock>> byProduct = stocks.stream()
                .collect(Collectors.groupingBy(s -> s.getProduct().getProductId()));

        List<ProductStockRow> stockRows = new ArrayList<>();
        List<String> zeroStockItems = new ArrayList<>();

        for (List<Stock> productStocks : byProduct.values()) {
            Stock first = productStocks.get(0);
            double totalQty = productStocks.stream()
                    .mapToDouble(s -> s.getQuantityInHand() == null ? 0.0 : s.getQuantityInHand())
                    .sum();
            totalQty = round2(totalQty);

            if (totalQty <= 0) {
                zeroStockItems.add(first.getProduct().getProductName());
            } else {
                ProductStockRow row = new ProductStockRow();
                row.setProductName(first.getProduct().getProductName());
                row.setCategory(first.getProduct().getCategory() != null
                        ? first.getProduct().getCategory().getCategoryName() : "");
                row.setTotalQty(totalQty);
                row.setUnit(first.getProduct().getMeasurementUnit());

                // Only include warehouses with qty > 0
                List<WarehouseStockRow> whRows = productStocks.stream()
                        .filter(s -> s.getQuantityInHand() != null && s.getQuantityInHand() > 0)
                        .map(s -> new WarehouseStockRow(
                                s.getWarehouse().getWarehouseName(),
                                round2(s.getQuantityInHand())))
                        .sorted(Comparator.comparing(WarehouseStockRow::getWarehouseName))
                        .collect(Collectors.toList());
                row.setWarehouseBreakdown(whRows);
                stockRows.add(row);
            }
        }

        stockRows.sort(Comparator.comparing(ProductStockRow::getProductName));
        Collections.sort(zeroStockItems);

        ProjectStockEmailData data = new ProjectStockEmailData();
        data.setProjectName(tenantName);
        data.setStockRows(stockRows);
        data.setZeroStockItems(zeroStockItems);
        return data;
    }

    /**
     * Builds a single Excel workbook with one sheet per project.
     * Each sheet has 3 sections: Stock Summary, Warehouse Breakdown, Zero Stock Items.
     */
    public byte[] buildExcelBytes(List<ProjectStockEmailData> allProjects) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();

        XSSFCellStyle sectionGreen = sectionHeaderStyle(workbook,
                new XSSFColor(new byte[]{(byte) 46, (byte) 125, (byte) 50}, null));
        XSSFCellStyle sectionRed   = sectionHeaderStyle(workbook,
                new XSSFColor(new byte[]{(byte) 183, (byte) 28, (byte) 28}, null));
        XSSFCellStyle colHeader    = colHeaderStyle(workbook,
                new XSSFColor(new byte[]{(byte) 76, (byte) 175, (byte) 80}, null));
        XSSFCellStyle normal       = normalStyle(workbook);
        XSSFCellStyle alt          = altStyle(workbook);

        for (ProjectStockEmailData project : allProjects) {
            String sheetName = project.getProjectName().length() > 31
                    ? project.getProjectName().substring(0, 31)
                    : project.getProjectName();
            XSSFSheet sheet = workbook.createSheet(sheetName);
            sheet.setColumnWidth(0, 9000);  // Product
            sheet.setColumnWidth(1, 5500);  // Category / Warehouse
            sheet.setColumnWidth(2, 4000);  // Qty
            sheet.setColumnWidth(3, 3500);  // Unit

            int r = 0;

            // ── Section 1: Stock Summary ──
            r = writeSectionHeader(sheet, r, "STOCK SUMMARY", sectionGreen);
            r = writeColHeaders(sheet, r, colHeader, "Product", "Category", "Total Qty", "Unit");
            int altCount = 0;
            for (ProductStockRow row : project.getStockRows()) {
                r = writeDataRow(sheet, r, altCount++ % 2 == 0 ? normal : alt,
                        row.getProductName(), row.getCategory(),
                        String.valueOf(row.getTotalQty()), row.getUnit());
            }
            if (project.getStockRows().isEmpty()) {
                r = writeDataRow(sheet, r, normal, "No items in stock", "", "", "");
            }
            r += 2;

            // ── Section 2: Warehouse Breakdown ──
            r = writeSectionHeader(sheet, r, "WAREHOUSE BREAKDOWN (NON-ZERO ONLY)", sectionGreen);
            r = writeColHeaders(sheet, r, colHeader, "Product", "Warehouse", "Qty", "Unit");
            altCount = 0;
            for (ProductStockRow row : project.getStockRows()) {
                for (WarehouseStockRow wh : row.getWarehouseBreakdown()) {
                    r = writeDataRow(sheet, r, altCount++ % 2 == 0 ? normal : alt,
                            row.getProductName(), wh.getWarehouseName(),
                            String.valueOf(wh.getQuantity()), row.getUnit());
                }
            }
            r += 2;

            // ── Section 3: Zero Stock ──
            r = writeSectionHeader(sheet, r, "ZERO STOCK ITEMS", sectionRed);
            if (project.getZeroStockItems().isEmpty()) {
                XSSFRow zRow = sheet.createRow(r++);
                XSSFCell c = zRow.createCell(0);
                c.setCellValue("None — all items have stock");
                c.setCellStyle(normal);
            } else {
                for (String name : project.getZeroStockItems()) {
                    XSSFRow zRow = sheet.createRow(r++);
                    XSSFCell c = zRow.createCell(0);
                    c.setCellValue(name);
                    c.setCellStyle(normal);
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // ── Helpers ──

    private int writeSectionHeader(XSSFSheet sheet, int rowNum, String title, XSSFCellStyle style) {
        XSSFRow row = sheet.createRow(rowNum);
        row.setHeightInPoints(20);
        XSSFCell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        return rowNum + 1;
    }

    private int writeColHeaders(XSSFSheet sheet, int rowNum, XSSFCellStyle style, String... headers) {
        XSSFRow row = sheet.createRow(rowNum);
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
        return rowNum + 1;
    }

    private int writeDataRow(XSSFSheet sheet, int rowNum, XSSFCellStyle style, String... values) {
        XSSFRow row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(values[i] != null ? values[i] : "");
            cell.setCellStyle(style);
        }
        return rowNum + 1;
    }

    private XSSFCellStyle sectionHeaderStyle(XSSFWorkbook wb, XSSFColor color) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle colHeaderStyle(XSSFWorkbook wb, XSSFColor color) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle normalStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private XSSFCellStyle altStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(
                new XSSFColor(new byte[]{(byte) 232, (byte) 245, (byte) 233}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
