package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.GlobalLowStockSpecification;
import com.ec.application.model.GlobalLowStockReport;
import com.ec.application.repository.GlobalLowStockReportRepository;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class LowStockReportService {

    private static final SimpleDateFormat DATE_DISPLAY = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    private final GlobalLowStockReportRepository repo;
    private final UserDetailsService userDetailsService;

    public Page<GlobalLowStockReport> getFiltered(FilterDataList filterDataList, Pageable pageable) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        Specification<GlobalLowStockReport> spec = GlobalLowStockSpecification.getSpecification(filterDataList, allowedSchemas);
        return spec == null ? repo.findAll(pageable) : repo.findAll(spec, pageable);
    }

    /** Tile data — counts per time window + project breakdown. */
    public Map<String, Object> getTiles() throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        Map<String, Object> result = new HashMap<>();

        long[] windowDays = {1, 3, 7, 30};
        String[] keys     = {"last1Day", "last3Days", "last7Days", "last30Days"};

        for (int i = 0; i < windowDays.length; i++) {
            Date since = dateMinusDays(windowDays[i]);
            long total = Optional.ofNullable(repo.countSince(since, allowedSchemas)).orElse(0L);
            List<Object[]> byProject = repo.countByProjectSince(since, allowedSchemas);
            result.put(keys[i], buildTileMap(total, byProject));
        }

        // All currently low-stock products
        long totalAll = Optional.ofNullable(repo.countAll(allowedSchemas)).orElse(0L);
        List<Object[]> allByProject = repo.countAllByProject(allowedSchemas);
        result.put("total", buildTileMap(totalAll, allByProject));

        return result;
    }

    public Map<String, Object> getDropdowns() {
        Map<String, Object> result = new HashMap<>();
        result.put("categories", repo.findDistinctCategories());
        return result;
    }

    public void exportExcel(FilterDataList filterDataList, HttpServletResponse response) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        Specification<GlobalLowStockReport> spec = GlobalLowStockSpecification.getSpecification(filterDataList, allowedSchemas);
        List<GlobalLowStockReport> rows = spec == null ? repo.findAll() : repo.findAll(spec);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Low Stock Report");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                "Project", "Product", "Product Code", "Unit", "Category",
                "Qty in Hand", "Reorder Level", "Deficit", "Low Stock Since"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            int rowNum = 1;
            for (GlobalLowStockReport r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getTenantSchema() != null ? r.getTenantSchema() : "");
                row.createCell(1).setCellValue(r.getProductName() != null ? r.getProductName() : "");
                row.createCell(2).setCellValue(r.getProductCode() != null ? r.getProductCode() : "");
                row.createCell(3).setCellValue(r.getUnit() != null ? r.getUnit() : "");
                row.createCell(4).setCellValue(r.getCategory() != null ? r.getCategory() : "");
                row.createCell(5).setCellValue(r.getQtyInHand() != null ? r.getQtyInHand() : 0.0);
                row.createCell(6).setCellValue(r.getReorderLevel() != null ? r.getReorderLevel() : 0.0);
                row.createCell(7).setCellValue(r.getDeficit() != null ? r.getDeficit() : 0.0);
                row.createCell(8).setCellValue(r.getLowStockSince() != null
                        ? DATE_DISPLAY.format(r.getLowStockSince()) : "");
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=low_stock_report.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> buildTileMap(long total, List<Object[]> byProjectRows) {
        List<Map<String, Object>> byProject = new ArrayList<>();
        for (Object[] row : byProjectRows) {
            Map<String, Object> p = new HashMap<>();
            p.put("project", row[0]);
            p.put("count", row[1]);
            byProject.add(p);
        }
        Map<String, Object> tile = new HashMap<>();
        tile.put("total", total);
        tile.put("byProject", byProject);
        return tile;
    }

    /**
     * Returns start-of-day for (today - (days-1)).
     * days=1 → today midnight (00:00:00) — "New Today"
     * days=3 → 2 days ago midnight      — "Last 3 Days" covers today + 2 prior calendar days
     */
    private Date dateMinusDays(long days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, (int) -(days - 1));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
