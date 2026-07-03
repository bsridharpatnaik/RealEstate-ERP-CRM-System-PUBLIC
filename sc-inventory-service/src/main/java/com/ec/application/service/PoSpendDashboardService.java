package com.ec.application.service;

import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.HighValuePoRow;
import com.ec.application.data.PoSpendBreakdownRow;
import com.ec.application.data.PoSpendMonthlyRow;
import com.ec.application.data.PoSpendSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Procurement Spend Dashboard — live aggregations over master-schema purchase_order.
 * Money rules: CANCELLED excluded everywhere; PO value = grandTotal; null project = 'Unassigned'.
 * All endpoints run under @UseDefaultTenant (master schema) — see controller.
 */
@Service
@RequiredArgsConstructor
public class PoSpendDashboardService {

    private static final Logger log = LoggerFactory.getLogger(PoSpendDashboardService.class);

    private final EntityManager em;

    @Value("${po.spend.high-value-threshold:100000}")
    private double highValueThreshold;

    @Value("${po.spend.color-bands:100000,500000,1000000}")
    private String colorBandsRaw;

    // Cancelled POs never count as spend. Supplier/firm joined for filtering + breakdowns.
    private static final String BASE_FROM =
        " FROM purchase_order po" +
        " LEFT JOIN contacts s ON s.contactId = po.supplier_id AND s.contactType = 'SUPPLIER' AND s.is_deleted = 0" +
        " LEFT JOIN Firm f ON f.firmId = po.firm_id AND f.is_deleted = 0" +
        " WHERE po.is_deleted = 0 AND po.status <> 'CANCELLED'";

    // ── WHERE builder ────────────────────────────────────────────────────────

    private static class WhereClause {
        final StringBuilder sql = new StringBuilder();
        final Map<String, Object> params = new HashMap<>();
    }

    private WhereClause buildWhere(FilterDataList filters) {
        WhereClause w = new WhereClause();
        if (filters == null || filters.getFilterData() == null) return w;

        int i = 0;
        for (FilterAttributeData fad : filters.getFilterData()) {
            String attr = fad.getAttrName();
            List<String> values = fad.getAttrValue();
            if (attr == null || values == null || values.isEmpty()) continue;

            switch (attr) {
                case "startDate":
                    w.sql.append(" AND po.po_date >= :startDate");
                    w.params.put("startDate", values.get(0));
                    break;
                case "endDate":
                    w.sql.append(" AND po.po_date < DATE_ADD(:endDate, INTERVAL 1 DAY)");
                    w.params.put("endDate", values.get(0));
                    break;
                case "projects": {
                    String p = "projects";
                    w.sql.append(" AND COALESCE(po.project_name, 'Unassigned') IN (:").append(p).append(")");
                    w.params.put(p, values);
                    break;
                }
                case "suppliers": {
                    String p = "suppliers";
                    w.sql.append(" AND COALESCE(s.name, 'Unknown') IN (:").append(p).append(")");
                    w.params.put(p, values);
                    break;
                }
                case "firms": {
                    String p = "firms";
                    w.sql.append(" AND COALESCE(f.firm_name, 'Unknown') IN (:").append(p).append(")");
                    w.params.put(p, values);
                    break;
                }
                case "statuses": {
                    String p = "statuses";
                    w.sql.append(" AND po.status IN (:").append(p).append(")");
                    w.params.put(p, values);
                    break;
                }
                case "minValue":
                    w.sql.append(" AND COALESCE(po.grandTotal, 0) >= :minValue");
                    w.params.put("minValue", Double.valueOf(values.get(0)));
                    break;
                case "maxValue":
                    w.sql.append(" AND COALESCE(po.grandTotal, 0) <= :maxValue");
                    w.params.put("maxValue", Double.valueOf(values.get(0)));
                    break;
                default:
                    break;
            }
            i++;
        }
        return w;
    }

    private void applyParams(Query q, Map<String, Object> params) {
        for (Map.Entry<String, Object> e : params.entrySet()) {
            q.setParameter(e.getKey(), e.getValue());
        }
    }

    // ── Summary tiles ─────────────────────────────────────────────────────────

    public PoSpendSummaryDTO getSummary(FilterDataList filters) {
        WhereClause w = buildWhere(filters);

        String sql =
            "SELECT" +
            "  COALESCE(SUM(COALESCE(po.grandTotal,0)),0)," +
            "  COUNT(*)," +
            "  COALESCE(SUM(CASE WHEN po.status IN ('NEW','PARTIAL') THEN COALESCE(po.grandTotal,0) ELSE 0 END),0)," +
            "  SUM(CASE WHEN po.status IN ('NEW','PARTIAL') THEN 1 ELSE 0 END)," +
            "  COALESCE(SUM(CASE WHEN COALESCE(po.grandTotal,0) > :hv THEN COALESCE(po.grandTotal,0) ELSE 0 END),0)," +
            "  SUM(CASE WHEN COALESCE(po.grandTotal,0) > :hv THEN 1 ELSE 0 END)," +
            "  COALESCE(MAX(po.grandTotal),0)," +
            "  COALESCE(AVG(po.grandTotal),0)," +
            "  COALESCE(SUM(CASE WHEN po.status = 'SHORT CLOSED' THEN COALESCE(po.grandTotal,0) ELSE 0 END),0)," +
            "  SUM(CASE WHEN po.status = 'SHORT CLOSED' THEN 1 ELSE 0 END)," +
            "  COALESCE(SUM(CASE WHEN po.po_date >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH) THEN COALESCE(po.grandTotal,0) ELSE 0 END),0)" +
            BASE_FROM + w.sql;

        Query q = em.createNativeQuery(sql);
        q.setParameter("hv", highValueThreshold);
        applyParams(q, w.params);

        Object[] r = (Object[]) q.getSingleResult();

        PoSpendSummaryDTO dto = new PoSpendSummaryDTO();
        dto.setTotalSpend(toDouble(r[0]));
        dto.setTotalPoCount(toLong(r[1]));
        dto.setOpenCommitment(toDouble(r[2]));
        dto.setOpenPoCount(toLong(r[3]));
        dto.setHighValueSpend(toDouble(r[4]));
        dto.setHighValueCount(toLong(r[5]));
        dto.setLargestPoValue(toDouble(r[6]));
        dto.setAvgPoValue(toDouble(r[7]));
        dto.setShortClosedValue(toDouble(r[8]));
        dto.setShortClosedCount(toLong(r[9]));
        dto.setLast3MonthsSpend(toDouble(r[10]));
        dto.setHighValueThreshold(highValueThreshold);
        dto.setColorBands(parseBands());
        return dto;
    }

    // ── Breakdowns ────────────────────────────────────────────────────────────

    public List<PoSpendBreakdownRow> getByProject(FilterDataList filters) {
        return breakdown(filters, "COALESCE(po.project_name, 'Unassigned')");
    }

    public List<PoSpendBreakdownRow> getBySupplier(FilterDataList filters) {
        return breakdown(filters, "COALESCE(s.name, 'Unknown')");
    }

    public List<PoSpendBreakdownRow> getByFirm(FilterDataList filters) {
        return breakdown(filters, "COALESCE(f.firm_name, 'Unknown')");
    }

    private List<PoSpendBreakdownRow> breakdown(FilterDataList filters, String labelExpr) {
        WhereClause w = buildWhere(filters);
        String sql =
            "SELECT " + labelExpr + " AS label," +
            " COALESCE(SUM(COALESCE(po.grandTotal,0)),0) AS total, COUNT(*) AS cnt" +
            BASE_FROM + w.sql +
            " GROUP BY " + labelExpr +
            " ORDER BY total DESC";
        Query q = em.createNativeQuery(sql);
        applyParams(q, w.params);

        List<?> rows = q.getResultList();
        List<PoSpendBreakdownRow> out = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            out.add(new PoSpendBreakdownRow(
                    r[0] == null ? "Unknown" : r[0].toString(),
                    toDouble(r[1]),
                    toLong(r[2])));
        }
        return out;
    }

    // ── Monthly trend (last 12 calendar months) ───────────────────────────────

    public List<PoSpendMonthlyRow> getTrend(FilterDataList filters) {
        WhereClause w = buildWhere(filters);
        String sql =
            "SELECT DATE_FORMAT(po.po_date, '%Y-%m') AS ym," +
            " COALESCE(SUM(COALESCE(po.grandTotal,0)),0) AS total, COUNT(*) AS cnt" +
            BASE_FROM + w.sql +
            " AND po.po_date >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 11 MONTH)" +
            " GROUP BY ym ORDER BY ym";
        Query q = em.createNativeQuery(sql);
        applyParams(q, w.params);

        List<?> rows = q.getResultList();
        List<PoSpendMonthlyRow> out = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            out.add(new PoSpendMonthlyRow(
                    r[0] == null ? "" : r[0].toString(),
                    toDouble(r[1]),
                    toLong(r[2])));
        }
        return out;
    }

    // ── High-value / filtered PO list (paged) ─────────────────────────────────

    /** highValueOnly=true restricts to POs above the configured threshold. */
    public Page<HighValuePoRow> getPoList(FilterDataList filters, boolean highValueOnly, Pageable pageable) {
        WhereClause w = buildWhere(filters);
        String extra = highValueOnly ? " AND COALESCE(po.grandTotal,0) > :hv" : "";

        String dataSql =
            "SELECT po.purchase_order_id, po.po_date, COALESCE(po.project_name,'Unassigned')," +
            " COALESCE(s.name,'Unknown'), COALESCE(f.firm_name,'Unknown'), po.status," +
            " COALESCE(po.grandTotal,0), po.is_special_po" +
            BASE_FROM + w.sql + extra +
            " ORDER BY po.grandTotal DESC";

        Query q = em.createNativeQuery(dataSql);
        if (highValueOnly) q.setParameter("hv", highValueThreshold);
        applyParams(q, w.params);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());

        List<?> rows = q.getResultList();
        List<HighValuePoRow> content = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            content.add(new HighValuePoRow(
                    r[0] == null ? "" : r[0].toString(),
                    (Date) r[1],
                    r[2] == null ? "Unassigned" : r[2].toString(),
                    r[3] == null ? "Unknown" : r[3].toString(),
                    r[4] == null ? "Unknown" : r[4].toString(),
                    r[5] == null ? "" : r[5].toString(),
                    toDouble(r[6]),
                    toBool(r[7])));
        }

        String countSql = "SELECT COUNT(*)" + BASE_FROM + w.sql + extra;
        Query cq = em.createNativeQuery(countSql);
        if (highValueOnly) cq.setParameter("hv", highValueThreshold);
        applyParams(cq, w.params);
        long total = toLong(cq.getSingleResult());

        return new PageImpl<>(content, pageable, total);
    }

    // ── Dropdowns ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<String> getDistinctProjects() {
        return em.createNativeQuery(
                "SELECT DISTINCT COALESCE(project_name,'Unassigned') FROM purchase_order" +
                " WHERE is_deleted = 0 AND status <> 'CANCELLED' ORDER BY 1").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> getDistinctSuppliers() {
        return em.createNativeQuery(
                "SELECT DISTINCT COALESCE(s.name,'Unknown') FROM purchase_order po" +
                " LEFT JOIN contacts s ON s.contactId = po.supplier_id AND s.contactType = 'SUPPLIER'" +
                " WHERE po.is_deleted = 0 AND po.status <> 'CANCELLED' ORDER BY 1").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> getDistinctFirms() {
        return em.createNativeQuery(
                "SELECT DISTINCT COALESCE(f.firm_name,'Unknown') FROM purchase_order po" +
                " LEFT JOIN Firm f ON f.firmId = po.firm_id" +
                " WHERE po.is_deleted = 0 AND po.status <> 'CANCELLED' ORDER BY 1").getResultList();
    }

    // ── Excel export (filtered PO list) ───────────────────────────────────────

    public void exportExcel(FilterDataList filters, boolean highValueOnly, HttpServletResponse response) throws Exception {
        Page<HighValuePoRow> page = getPoList(filters, highValueOnly, org.springframework.data.domain.PageRequest.of(0, 5000));
        List<HighValuePoRow> rows = page.getContent();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"po-spend.xlsx\"");

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("PO Spend");
            String[] headers = {"PO No", "Date", "Project", "Supplier", "Firm", "Status", "Grand Total", "Special PO"};
            Row hr = sheet.createRow(0);
            CellStyle hs = wb.createCellStyle();
            Font hf = wb.createFont();
            hf.setBold(true);
            hs.setFont(hf);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = hr.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(hs);
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
            int rIdx = 1;
            for (HighValuePoRow r : rows) {
                Row row = sheet.createRow(rIdx++);
                row.createCell(0).setCellValue(r.getPurchaseOrderId());
                row.createCell(1).setCellValue(r.getPoDate() != null ? sdf.format(r.getPoDate()) : "");
                row.createCell(2).setCellValue(r.getProject());
                row.createCell(3).setCellValue(r.getSupplierName());
                row.createCell(4).setCellValue(r.getFirmName());
                row.createCell(5).setCellValue(r.getStatus());
                row.createCell(6).setCellValue(r.getGrandTotal());
                row.createCell(7).setCellValue(r.isSpecialPo() ? "Yes" : "No");
            }
            for (int c = 0; c < headers.length; c++) sheet.autoSizeColumn(c);
            wb.write(response.getOutputStream());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Double> parseBands() {
        List<Double> bands = new ArrayList<>();
        if (colorBandsRaw == null || colorBandsRaw.trim().isEmpty()) return bands;
        for (String s : colorBandsRaw.split(",")) {
            try { bands.add(Double.valueOf(s.trim())); } catch (NumberFormatException ignored) { }
        }
        return bands;
    }

    private static double toDouble(Object o) {
        return o == null ? 0d : ((Number) o).doubleValue();
    }

    private static long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        return false;
    }
}
