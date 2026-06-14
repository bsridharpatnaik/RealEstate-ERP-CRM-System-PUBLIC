package com.ec.application.service;

import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.SupplierPerformanceRow;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierPerformanceService {

    private final EntityManager em;
    private final UserDetailsService userDetailsService;

    /**
     * "overdue" = open PO (not cancelled, not completed, not short-closed) whose age in days
     * exceeds the product's expected lead time.
     * We aggregate at PO level: a PO is overdue if ANY of its lines is overdue.
     */
    private static final String DATA_SELECT =
        "SELECT" +
        "  s.contactId AS supplierId," +
        "  s.name AS supplierName," +
        "  COUNT(DISTINCT po.purchase_order_id) AS totalPos," +
        "  COUNT(DISTINCT CASE WHEN po.status = 'COMPLETED' THEN po.purchase_order_id END) AS completedPos," +
        "  COUNT(DISTINCT CASE WHEN po.status NOT IN ('CANCELLED','COMPLETED','SHORT CLOSED','SHORT CLOSE') THEN po.purchase_order_id END) AS pendingPos," +
        "  COUNT(DISTINCT CASE WHEN po.status IN ('SHORT CLOSED','SHORT CLOSE') THEN po.purchase_order_id END) AS shortClosedPos," +
        "  COUNT(DISTINCT CASE WHEN po.status = 'CANCELLED' THEN po.purchase_order_id END) AS cancelledPos," +
        // overdue: open (pending/partial) POs whose age > expected lead time
        "  COUNT(DISTINCT CASE" +
        "    WHEN po.status NOT IN ('CANCELLED','COMPLETED','SHORT CLOSED','SHORT CLOSE')" +
        "     AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL" +
        "     AND DATEDIFF(CURDATE(), po.po_date) > COALESCE(p.lead_time_days, cat.lead_time_days)" +
        "    THEN po.purchase_order_id END) AS overduePos," +
        // on-time eligible = non-cancelled POs that have a lead time defined
        "  COUNT(DISTINCT CASE" +
        "    WHEN po.status != 'CANCELLED'" +
        "     AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL" +
        "    THEN po.purchase_order_id END) AS onTimeEligible," +
        "  ROUND(" +
        "    (COUNT(DISTINCT CASE WHEN po.status != 'CANCELLED' AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL THEN po.purchase_order_id END)" +
        "     - COUNT(DISTINCT CASE WHEN po.status NOT IN ('CANCELLED','COMPLETED','SHORT CLOSED','SHORT CLOSE') AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL AND DATEDIFF(CURDATE(), po.po_date) > COALESCE(p.lead_time_days, cat.lead_time_days) THEN po.purchase_order_id END)" +
        "    ) * 100.0 / NULLIF(COUNT(DISTINCT CASE WHEN po.status != 'CANCELLED' AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL THEN po.purchase_order_id END), 0)" +
        "  , 1) AS onTimeRate," +
        "  COALESCE(SUM(pol.quantity * COALESCE(pol.netRate, pol.rate, 0)), 0) AS totalOrderValue," +
        "  MAX(po.po_date) AS lastPoDate";

    private static final String BASE_FROM =
        " FROM contacts s" +
        " JOIN purchase_order po ON po.supplier_id = s.contactId AND po.is_deleted = 0" +
        " JOIN purchase_order_line pol ON pol.po_id = po.purchase_order_id AND pol.is_deleted = 0" +
        " JOIN product p ON p.productId = pol.product_id AND p.is_deleted = 0" +
        " LEFT JOIN category cat ON cat.categoryId = p.categoryId AND cat.is_deleted = 0" +
        " WHERE s.is_deleted = 0 AND s.contacttype = 'supplier'";

    private static final String GROUP_BY = " GROUP BY s.contactId, s.name";

    // ── Public API ────────────────────────────────────────────────────────────

    public Page<SupplierPerformanceRow> getPage(FilterDataList filters, Pageable pageable) throws Exception {
        WhereClause wc = buildWhere(filters);
        String fullSql = DATA_SELECT + BASE_FROM + wc.sql + GROUP_BY;

        String countSql = "SELECT COUNT(*) FROM (" + fullSql + ") cnt_sub";
        Query countQ = em.createNativeQuery(countSql);
        applyParams(countQ, wc.params);
        long total = ((Number) countQ.getSingleResult()).longValue();

        String dataSql = fullSql + buildOrderBy(pageable) +
                " LIMIT " + pageable.getPageSize() + " OFFSET " + pageable.getOffset();
        Query dataQ = em.createNativeQuery(dataSql);
        applyParams(dataQ, wc.params);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();
        List<SupplierPerformanceRow> content = rows.stream().map(this::map).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }

    public Map<String, Object> getSummaryTiles(FilterDataList filters) throws Exception {
        WhereClause wc = buildWhere(filters);
        String innerSql = DATA_SELECT + BASE_FROM + wc.sql + GROUP_BY;

        String tileSql =
            "SELECT" +
            "  COUNT(*) AS supplierCount," +
            "  SUM(totalPos) AS totalPos," +
            "  SUM(overduePos) AS totalOverdue," +
            "  ROUND(AVG(CASE WHEN onTimeEligible > 0 THEN onTimeRate END), 1) AS avgOnTimeRate," +
            "  SUM(totalOrderValue) AS totalOrderValue" +
            " FROM (" + innerSql + ") tile_sub";

        Query q = em.createNativeQuery(tileSql);
        applyParams(q, wc.params);

        Object[] row = (Object[]) q.getSingleResult();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSuppliers", toLong(row[0]));
        result.put("totalPos",       toLong(row[1]));
        result.put("totalOverdue",   toLong(row[2]));
        result.put("avgOnTimeRate",  toDouble(row[3]));
        result.put("totalOrderValue", toDouble(row[4]));
        return result;
    }

    /**
     * Price comparison for a single firm: per-product, this firm's avg rate vs
     * market (all other suppliers) min/avg/max for the same product.
     */
    public List<Map<String, Object>> getPriceComparison(Long firmId) {
        // Use netRate (rate after discount) for fair comparison; fall back to rate when netRate is null
        // supplierId here is contacts.contactId
        String sql =
            "SELECT" +
            "  p.productId," +
            "  p.product_name AS productName," +
            "  p.measurementUnit AS unit," +
            "  ROUND(AVG(CASE WHEN po.supplier_id = :supplierId THEN COALESCE(pol.netRate, pol.rate) END), 2) AS thisRate," +
            "  ROUND(MIN(CASE WHEN po.supplier_id != :supplierId AND COALESCE(pol.netRate, pol.rate) > 0 THEN COALESCE(pol.netRate, pol.rate) END), 2) AS marketMin," +
            "  ROUND(AVG(CASE WHEN po.supplier_id != :supplierId AND COALESCE(pol.netRate, pol.rate) > 0 THEN COALESCE(pol.netRate, pol.rate) END), 2) AS marketAvg," +
            "  ROUND(MAX(CASE WHEN po.supplier_id != :supplierId AND COALESCE(pol.netRate, pol.rate) > 0 THEN COALESCE(pol.netRate, pol.rate) END), 2) AS marketMax," +
            "  COUNT(DISTINCT CASE WHEN po.supplier_id = :supplierId THEN po.purchase_order_id END) AS thisPosCount" +
            " FROM purchase_order_line pol" +
            " JOIN purchase_order po ON po.purchase_order_id = pol.po_id AND po.is_deleted = 0" +
            " JOIN product p ON p.productId = pol.product_id AND p.is_deleted = 0" +
            " WHERE pol.is_deleted = 0" +
            "   AND COALESCE(pol.netRate, pol.rate) > 0" +
            "   AND pol.product_id IN (" +
            "     SELECT DISTINCT pol2.product_id FROM purchase_order_line pol2" +
            "     JOIN purchase_order po2 ON po2.purchase_order_id = pol2.po_id AND po2.is_deleted = 0" +
            "     WHERE pol2.is_deleted = 0 AND po2.supplier_id = :supplierId" +
            "   )" +
            " GROUP BY p.productId, p.product_name, p.measurementUnit" +
            " HAVING thisRate IS NOT NULL" +
            " ORDER BY p.product_name ASC";

        Query q = em.createNativeQuery(sql);
        q.setParameter("supplierId", firmId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId",   toLong(r[0]));
            m.put("productName", r[1] != null ? r[1].toString() : "");
            m.put("unit",        r[2] != null ? r[2].toString() : "");
            m.put("thisRate",    toDouble(r[3]));
            m.put("marketMin",   toDouble(r[4]));
            m.put("marketAvg",   toDouble(r[5]));
            m.put("marketMax",   toDouble(r[6]));
            m.put("poCount",     toLong(r[7]));
            Double avg = toDouble(r[5]);
            Double mine = toDouble(r[3]);
            m.put("priceIndex", (avg != null && avg > 0 && mine != null) ? Math.round(mine / avg * 100.0) : null);
            return m;
        }).collect(Collectors.toList());
    }

    public void exportExcel(FilterDataList filters, HttpServletResponse response) throws Exception {
        WhereClause wc = buildWhere(filters);
        String dataSql = DATA_SELECT + BASE_FROM + wc.sql + GROUP_BY + " ORDER BY s.name ASC";
        Query dataQ = em.createNativeQuery(dataSql);
        applyParams(dataQ, wc.params);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();
        List<SupplierPerformanceRow> data = rows.stream().map(this::map).collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Supplier Performance");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                "Supplier", "Total POs", "Completed", "Pending", "Short-closed",
                "Cancelled", "Overdue", "On-time Rate %", "Total Order Value"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rowNum = 1;
            for (SupplierPerformanceRow r : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getSupplierName() != null ? r.getSupplierName() : "");
                row.createCell(1).setCellValue(r.getTotalPos() != null ? r.getTotalPos() : 0L);
                row.createCell(2).setCellValue(r.getCompletedPos() != null ? r.getCompletedPos() : 0L);
                row.createCell(3).setCellValue(r.getPendingPos() != null ? r.getPendingPos() : 0L);
                row.createCell(4).setCellValue(r.getShortClosedPos() != null ? r.getShortClosedPos() : 0L);
                row.createCell(5).setCellValue(r.getCancelledPos() != null ? r.getCancelledPos() : 0L);
                row.createCell(6).setCellValue(r.getOverduePos() != null ? r.getOverduePos() : 0L);
                row.createCell(7).setCellValue(r.getOnTimeRate() != null ? r.getOnTimeRate() : 0.0);
                row.createCell(8).setCellValue(r.getTotalOrderValue() != null ? r.getTotalOrderValue() : 0.0);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=supplier_performance.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    // ── Where builder ─────────────────────────────────────────────────────────

    private static class WhereClause {
        String sql = "";
        Map<String, Object> params = new LinkedHashMap<>();
    }

    private WhereClause buildWhere(FilterDataList filters) throws Exception {
        WhereClause wc = new WhereClause();

        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        // Note: POs in master schema are not directly tenant-scoped.
        // We use allowedSchemas as an info-only note here; all PO data is visible to admin/purchase-manager.

        if (filters == null || filters.getFilterData() == null) return wc;

        for (FilterAttributeData f : filters.getFilterData()) {
            if (f.getAttrName() == null || f.getAttrValue() == null || f.getAttrValue().isEmpty()) continue;
            List<String> vals = f.getAttrValue().stream()
                    .filter(v -> v != null && !v.trim().isEmpty())
                    .collect(Collectors.toList());
            if (vals.isEmpty()) continue;

            switch (f.getAttrName()) {
                case "startDate":
                    wc.sql += " AND po.po_date >= STR_TO_DATE(:startDate, '%d-%m-%Y')";
                    wc.params.put("startDate", vals.get(0));
                    break;
                case "endDate":
                    wc.sql += " AND po.po_date < DATE_ADD(STR_TO_DATE(:endDate, '%d-%m-%Y'), INTERVAL 1 DAY)";
                    wc.params.put("endDate", vals.get(0));
                    break;
                case "supplierName":
                    wc.sql += " AND s.name LIKE :supplierName";
                    wc.params.put("supplierName", "%" + vals.get(0) + "%");
                    break;
                case "categoryName":
                    wc.sql += " AND cat.category_name = :categoryName";
                    wc.params.put("categoryName", vals.get(0));
                    break;
            }
        }
        return wc;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyParams(Query q, Map<String, Object> params) {
        params.forEach(q::setParameter);
    }

    private String buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) return " ORDER BY onTimeRate ASC";
        return pageable.getSort().stream()
                .map(o -> {
                    String col;
                    switch (o.getProperty()) {
                        case "supplierName":    col = "s.name"; break;
                        case "totalPos":       col = "totalPos"; break;
                        case "completedPos":   col = "completedPos"; break;
                        case "overduePos":     col = "overduePos"; break;
                        case "onTimeRate":     col = "onTimeRate"; break;
                        case "totalOrderValue":col = "totalOrderValue"; break;
                        case "lastPoDate":     col = "lastPoDate"; break;
                        default:               col = "onTimeRate";
                    }
                    return col + " " + o.getDirection().name();
                })
                .collect(Collectors.joining(", ", " ORDER BY ", ""));
    }

    private SupplierPerformanceRow map(Object[] row) {
        SupplierPerformanceRow r = new SupplierPerformanceRow();
        r.setSupplierId(toLong(row[0]));
        r.setSupplierName(row[1] != null ? row[1].toString() : null);
        r.setTotalPos(toLong(row[2]));
        r.setCompletedPos(toLong(row[3]));
        r.setPendingPos(toLong(row[4]));
        r.setShortClosedPos(toLong(row[5]));
        r.setCancelledPos(toLong(row[6]));
        r.setOverduePos(toLong(row[7]));
        // row[8]=onTimeEligible, row[9]=onTimeRate, row[10]=totalOrderValue, row[11]=lastPoDate
        r.setOnTimeRate(toDouble(row[9]));
        r.setTotalOrderValue(toDouble(row[10]));
        r.setLastPoDate(row[11] != null ? (java.util.Date) row[11] : null);
        return r;
    }

    private Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof BigDecimal) return ((BigDecimal) o).longValue();
        if (o instanceof Number) return ((Number) o).longValue();
        return 0L;
    }

    private Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return ((BigDecimal) o).doubleValue();
        if (o instanceof Number) return ((Number) o).doubleValue();
        return null;
    }
}
