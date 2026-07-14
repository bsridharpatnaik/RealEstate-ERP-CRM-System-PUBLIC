package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.ExpiryAlertRow;
import com.ec.application.data.ProductStockRow;
import com.ec.application.data.ProjectStockEmailData;
import com.ec.application.data.WarehouseStockRow;
import com.ec.application.model.InventoryBatch;
import com.ec.application.model.Stock;
import com.ec.application.repository.InventoryBatchRepository;
import com.ec.application.repository.PurchaseOrderLineRepository;
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
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

@Service
public class StockEmailReportService {

    @Autowired
    StockRepo stockRepo;

    @Autowired
    PurchaseOrderLineRepository purchaseOrderLineRepo;

    @Autowired
    InventoryBatchRepository inventoryBatchRepository;

    Logger log = LoggerFactory.getLogger(StockEmailReportService.class);

    /**
     * Loads latest net rate per product from master schema in one bulk query.
     * Must be called before switching to tenant context — @UseDefaultTenant ensures master schema.
     */
    @UseDefaultTenant
    public Map<Long, Double> fetchLatestNetRateMap() {
        List<Object[]> rows = purchaseOrderLineRepo.findLatestNetRatePerProduct();
        Map<Long, Double> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[1] != null)
                map.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
        }
        return map;
    }

    /**
     * Collects stock data for the current tenant (ThreadLocalStorage must be set by caller).
     * Splits into non-zero stock rows (with warehouse breakdown) and zero-stock item names.
     */
    public ProjectStockEmailData collectTenantStockData(String tenantName, Map<Long, Double> netRateMap) {
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

                Double netRate = netRateMap.get(first.getProduct().getProductId());
                row.setNetRate(netRate);
                row.setTgv(netRate != null ? round2(netRate * totalQty) : null);

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
     * Collects batch rows expiring between [from, to] for the current tenant.
     * ThreadLocalStorage must already be set to the target tenant by the caller.
     *
     * @param tenantDisplayName Human-readable project name shown in the email.
     * @param from              Window start (inclusive).
     * @param to                Window end (inclusive).
     */
    public List<ExpiryAlertRow> collectExpiryRows(String tenantDisplayName, Date from, Date to) {
        List<InventoryBatch> batches = inventoryBatchRepository.findBatchesExpiringBetween(from, to);
        List<ExpiryAlertRow> rows = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        for (InventoryBatch b : batches) {
            ExpiryAlertRow r = new ExpiryAlertRow();
            r.setProjectName(tenantDisplayName);
            r.setProductName(b.getProduct() != null ? b.getProduct().getProductName() : "");
            r.setWarehouseName(b.getWarehouse() != null ? b.getWarehouse().getWarehouseName() : "");
            r.setBrand(b.getBrand() != null ? b.getBrand() : "");
            r.setLotNumber(b.getLotNumber() != null ? b.getLotNumber() : "");
            r.setExpiryDate(b.getExpiryDate() != null ? sdf.format(b.getExpiryDate()) : "");
            r.setQtyRemaining(b.getQtyRemaining());
            r.setUnit(b.getProduct() != null ? b.getProduct().getMeasurementUnit() : "");
            rows.add(r);
        }
        rows.sort(Comparator.comparing(ExpiryAlertRow::getExpiryDate)
                .thenComparing(ExpiryAlertRow::getProductName));
        return rows;
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
            sheet.setColumnWidth(4, 5000);  // Net Rate
            sheet.setColumnWidth(5, 5000);  // TGV
            sheet.setColumnWidth(6, 7500);  // TGV (In Words)

            int r = 0;

            // ── Section 1: Stock Summary ──
            r = writeSectionHeader(sheet, r, "STOCK SUMMARY", sectionGreen);
            r = writeColHeaders(sheet, r, colHeader,
                    "Product", "Category", "Total Qty", "Unit", "Net Rate (Before GST)", "TGV", "TGV (In Words)");
            int altCount = 0;
            for (ProductStockRow row : project.getStockRows()) {
                r = writeDataRow(sheet, r, altCount++ % 2 == 0 ? normal : alt,
                        row.getProductName(), row.getCategory(),
                        String.valueOf(row.getTotalQty()), row.getUnit(),
                        formatOrBlank(row.getNetRate()), formatOrBlank(row.getTgv()),
                        toIndianDenomination(row.getTgv()));
            }
            if (project.getStockRows().isEmpty()) {
                r = writeDataRow(sheet, r, normal, "No items in stock", "", "", "", "", "", "");
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

    private String formatOrBlank(Double value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String toIndianDenomination(Double value) {
        if (value == null) return "";
        long amount = Math.round(value);
        if (amount == 0) return "0";
        StringBuilder sb = new StringBuilder();
        long crore    = amount / 10_000_000L;
        long lakh     = (amount % 10_000_000L) / 100_000L;
        long thousand = (amount % 100_000L)     / 1_000L;
        long rest     = amount % 1_000L;
        if (crore    > 0) sb.append(crore).append(" Crore ");
        if (lakh     > 0) sb.append(lakh).append(" Lakh ");
        if (thousand > 0) sb.append(thousand).append(" Thousand ");
        if (rest     > 0) sb.append(rest);
        return sb.toString().trim();
    }
}
