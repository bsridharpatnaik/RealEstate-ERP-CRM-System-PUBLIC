package com.ec.application.service;

import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.IndentFulfillmentRow;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndentFulfillmentService {

    private final EntityManager em;

    private static final String DATA_SELECT =
        "SELECT" +
        "  ii.indent_id," +
        "  ii.tenant AS project," +
        "  ii.indent_date," +
        "  ii.indent_status," +
        "  p.product_name," +
        "  p.product_code," +
        "  iie.measurement_unit AS unit," +
        "  COALESCE(iie.quantity, 0) AS requested_qty," +
        "  pol.quantity AS po_qty," +
        "  COALESCE(iie.quantity_received, 0) AS received_qty," +
        "  COALESCE(iie.quantity_pending, 0) AS pending_qty," +
        "  iie.purchaseOrderId AS po_number," +
        "  po.status AS po_status," +
        "  iie.line_item_status," +
        "  COALESCE(p.lead_time_days, cat.lead_time_days) AS lead_time_days";

    private static final String BASE_FROM =
        " FROM indent_inventory ii" +
        " JOIN indent_inventory_entries iie ON iie.indent_id = ii.indent_id AND iie.is_deleted = 0" +
        " JOIN product p ON p.productId = iie.productId AND p.is_deleted = 0" +
        " LEFT JOIN category cat ON cat.id = p.category_id AND cat.is_deleted = 0" +
        " LEFT JOIN purchase_order po ON po.purchase_order_id = iie.purchaseOrderId AND po.is_deleted = 0" +
        " LEFT JOIN purchase_order_line pol ON pol.po_id = iie.purchaseOrderId" +
        "   AND pol.product_id = iie.productId AND pol.is_deleted = 0" +
        " WHERE ii.is_deleted = 0";

    // ── Public API ────────────────────────────────────────────────────────────

    public Page<IndentFulfillmentRow> getPage(FilterDataList filters, Pageable pageable) {
        WhereClause wc = buildWhere(filters);

        String countSql = "SELECT COUNT(*) FROM (" +
                DATA_SELECT + BASE_FROM + wc.sql + ") cnt_sub";
        Query countQ = em.createNativeQuery(countSql);
        applyParams(countQ, wc.params);
        long total = ((Number) countQ.getSingleResult()).longValue();

        String dataSql = DATA_SELECT + BASE_FROM + wc.sql +
                buildOrderBy(pageable) +
                " LIMIT " + pageable.getPageSize() + " OFFSET " + pageable.getOffset();
        Query dataQ = em.createNativeQuery(dataSql);
        applyParams(dataQ, wc.params);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();
        List<IndentFulfillmentRow> content = rows.stream().map(this::map).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }

    public Map<String, Long> getSummaryStats(FilterDataList filters) {
        WhereClause wc = buildWhere(filters);
        String sql = "SELECT indent_status, COUNT(*) AS cnt" +
                " FROM indent_inventory ii WHERE ii.is_deleted = 0" + wc.statusSql +
                " GROUP BY indent_status ORDER BY cnt DESC";
        Query q = em.createNativeQuery(sql);
        applyParams(q, wc.statusParams);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] r : rows) {
            stats.put((String) r[0], ((Number) r[1]).longValue());
        }
        return stats;
    }

    public List<String> getDistinctProjects() {
        @SuppressWarnings("unchecked")
        List<String> result = em.createNativeQuery(
                "SELECT DISTINCT tenant FROM indent_inventory" +
                " WHERE is_deleted = 0 AND tenant IS NOT NULL ORDER BY tenant")
                .getResultList();
        return result;
    }

    public List<String> getDistinctProducts() {
        @SuppressWarnings("unchecked")
        List<String> result = em.createNativeQuery(
                "SELECT DISTINCT p.product_name FROM indent_inventory_entries iie" +
                " JOIN product p ON p.productId = iie.productId AND p.is_deleted = 0" +
                " JOIN indent_inventory ii ON ii.indent_id = iie.indent_id AND ii.is_deleted = 0" +
                " WHERE iie.is_deleted = 0 ORDER BY p.product_name")
                .getResultList();
        return result;
    }

    public void exportExcel(FilterDataList filters, HttpServletResponse response) throws Exception {
        WhereClause wc = buildWhere(filters);
        String sql = DATA_SELECT + BASE_FROM + wc.sql + " ORDER BY ii.indent_date DESC, ii.indent_id ASC, p.product_name ASC";
        Query q = em.createNativeQuery(sql);
        applyParams(q, wc.params);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Indent Fulfillment");
            CellStyle hdr = headerStyle(wb);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            String[] cols = {"Project", "Indent ID", "Indent Date", "Indent Status",
                    "Product", "Code", "Unit", "Requested Qty", "PO Qty", "Received Qty",
                    "Pending Qty", "% Fulfilled", "PO Number", "PO Status", "Line Status"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(hdr);
                sheet.setColumnWidth(i, 20 * 256);
            }
            int rn = 1;
            for (Object[] r : rows) {
                IndentFulfillmentRow row = map(r);
                Row exRow = sheet.createRow(rn++);
                exRow.createCell(0).setCellValue(safe(row.getProject()));
                exRow.createCell(1).setCellValue(safe(row.getIndentId()));
                exRow.createCell(2).setCellValue(row.getIndentDate() != null ? sdf.format(row.getIndentDate()) : "");
                exRow.createCell(3).setCellValue(safe(row.getIndentStatus()));
                exRow.createCell(4).setCellValue(safe(row.getProductName()));
                exRow.createCell(5).setCellValue(safe(row.getProductCode()));
                exRow.createCell(6).setCellValue(safe(row.getUnit()));
                exRow.createCell(7).setCellValue(n(row.getRequestedQty()));
                exRow.createCell(8).setCellValue(row.getPoQty() != null ? row.getPoQty() : 0.0);
                exRow.createCell(9).setCellValue(n(row.getReceivedQty()));
                exRow.createCell(10).setCellValue(n(row.getPendingQty()));
                exRow.createCell(11).setCellValue(row.getPercentFulfilled() != null
                        ? String.format("%.1f%%", row.getPercentFulfilled()) : "0%");
                exRow.createCell(12).setCellValue(safe(row.getPoNumber()));
                exRow.createCell(13).setCellValue(safe(row.getPoStatus()));
                exRow.createCell(14).setCellValue(safe(row.getLineItemStatus()));
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=indent_fulfillment.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WhereClause buildWhere(FilterDataList filters) {
        StringBuilder sql = new StringBuilder();
        // separate params for status-only query
        StringBuilder statusSql = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        Map<String, Object> statusParams = new LinkedHashMap<>();

        if (filters != null && filters.getFilterData() != null) {
            for (FilterAttributeData f : filters.getFilterData()) {
                if (f.getAttrValue() == null || f.getAttrValue().isEmpty()) continue;
                String v = f.getAttrValue().get(0);
                if (v == null || v.isEmpty()) continue;
                if ("project".equals(f.getAttrName())) {
                    List<String> projects = f.getAttrValue().stream()
                            .filter(s -> s != null && !s.isEmpty())
                            .collect(java.util.stream.Collectors.toList());
                    if (projects.size() == 1) {
                        sql.append(" AND ii.tenant = :project0");
                        statusSql.append(" AND tenant = :project0");
                        params.put("project0", projects.get(0));
                        statusParams.put("project0", projects.get(0));
                    } else if (projects.size() > 1) {
                        StringBuilder inClause = new StringBuilder(" AND ii.tenant IN (");
                        StringBuilder statusIn = new StringBuilder(" AND tenant IN (");
                        for (int pi = 0; pi < projects.size(); pi++) {
                            String pname = "project" + pi;
                            inClause.append(pi == 0 ? "" : ",").append(":").append(pname);
                            statusIn.append(pi == 0 ? "" : ",").append(":").append(pname);
                            params.put(pname, projects.get(pi));
                            statusParams.put(pname, projects.get(pi));
                        }
                        inClause.append(")"); statusIn.append(")");
                        sql.append(inClause); statusSql.append(statusIn);
                    }
                } else if ("indentStatus".equals(f.getAttrName())) {
                    // attrValue may contain multiple statuses (one per list entry)
                    List<String> statuses = f.getAttrValue().stream()
                            .filter(s -> s != null && !s.isEmpty())
                            .collect(java.util.stream.Collectors.toList());
                    if (statuses.size() == 1) {
                        sql.append(" AND ii.indent_status = :indentStatus");
                        statusSql.append(" AND indent_status = :indentStatus");
                        params.put("indentStatus", statuses.get(0));
                        statusParams.put("indentStatus", statuses.get(0));
                    } else if (statuses.size() > 1) {
                        StringBuilder inClause = new StringBuilder(" AND ii.indent_status IN (");
                        StringBuilder statusIn = new StringBuilder(" AND indent_status IN (");
                        for (int si = 0; si < statuses.size(); si++) {
                            String pname = "indentStatus" + si;
                            inClause.append(si == 0 ? "" : ",").append(":").append(pname);
                            statusIn.append(si == 0 ? "" : ",").append(":").append(pname);
                            params.put(pname, statuses.get(si));
                            statusParams.put(pname, statuses.get(si));
                        }
                        inClause.append(")"); statusIn.append(")");
                        sql.append(inClause); statusSql.append(statusIn);
                    }
                } else if ("lineItemStatus".equals(f.getAttrName())) {
                    sql.append(" AND iie.line_item_status = :lineItemStatus");
                    params.put("lineItemStatus", v);
                } else if ("productName".equals(f.getAttrName())) {
                    List<String> products = f.getAttrValue().stream()
                            .filter(s -> s != null && !s.isEmpty())
                            .collect(java.util.stream.Collectors.toList());
                    if (products.size() == 1) {
                        sql.append(" AND p.product_name = :productName0");
                        params.put("productName0", products.get(0));
                    } else if (products.size() > 1) {
                        StringBuilder inClause = new StringBuilder(" AND p.product_name IN (");
                        for (int pi = 0; pi < products.size(); pi++) {
                            String pname = "productName" + pi;
                            inClause.append(pi == 0 ? "" : ",").append(":").append(pname);
                            params.put(pname, products.get(pi));
                        }
                        inClause.append(")");
                        sql.append(inClause);
                    }
                } else if ("startDate".equals(f.getAttrName())) {
                    sql.append(" AND ii.indent_date >= STR_TO_DATE(:startDate, '%d-%m-%Y')");
                    params.put("startDate", v);
                } else if ("endDate".equals(f.getAttrName())) {
                    sql.append(" AND ii.indent_date < DATE_ADD(STR_TO_DATE(:endDate, '%d-%m-%Y'), INTERVAL 1 DAY)");
                    params.put("endDate", v);
                }
            }
        }
        return new WhereClause(sql.toString(), params, statusSql.toString(), statusParams);
    }

    private String buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = pageable.getSort().iterator().next();
            String col = "ii.indent_date";
            String prop = order.getProperty();
            if ("project".equals(prop))            col = "ii.tenant";
            else if ("indentId".equals(prop))      col = "ii.indent_id";
            else if ("indentDate".equals(prop))    col = "ii.indent_date";
            else if ("indentStatus".equals(prop))  col = "ii.indent_status";
            else if ("productName".equals(prop))   col = "p.product_name";
            else if ("requestedQty".equals(prop))  col = "iie.quantity";
            else if ("receivedQty".equals(prop))   col = "iie.quantity_received";
            else if ("pendingQty".equals(prop))    col = "iie.quantity_pending";
            return " ORDER BY " + col + " " + order.getDirection().name();
        }
        return " ORDER BY ii.indent_date DESC, ii.indent_id ASC, p.product_name ASC";
    }

    private void applyParams(Query q, Map<String, Object> params) {
        for (Map.Entry<String, Object> e : params.entrySet()) {
            q.setParameter(e.getKey(), e.getValue());
        }
    }

    private IndentFulfillmentRow map(Object[] r) {
        // indices: 0=indentId, 1=project, 2=indentDate, 3=indentStatus,
        //          4=productName, 5=productCode, 6=unit,
        //          7=requestedQty, 8=poQty, 9=receivedQty, 10=pendingQty,
        //          11=poNumber, 12=poStatus, 13=lineItemStatus
        IndentFulfillmentRow row = new IndentFulfillmentRow();
        row.setIndentId(str(r[0]));
        row.setProject(str(r[1]));
        row.setIndentDate(r[2] instanceof java.sql.Timestamp ? new Date(((java.sql.Timestamp) r[2]).getTime()) : null);
        row.setIndentStatus(str(r[3]));
        row.setProductName(str(r[4]));
        row.setProductCode(str(r[5]));
        row.setUnit(str(r[6]));
        double requested = toDouble(r[7]);
        double received  = toDouble(r[9]);
        double pending   = toDouble(r[10]);
        row.setRequestedQty(requested);
        row.setPoQty(r[8] != null ? toDouble(r[8]) : null);
        row.setReceivedQty(received);
        row.setPendingQty(pending);
        row.setPercentFulfilled(requested > 0 ? (received / requested) * 100 : 0.0);
        row.setPoNumber(str(r[11]));
        row.setPoStatus(str(r[12]));
        row.setLineItemStatus(str(r[13]));
        // r[14] = lead_time_days
        if (r.length > 14 && r[14] != null) {
            row.setLeadTimeDays(((Number) r[14]).intValue());
        }
        return row;
    }

    private String str(Object o) { return o == null ? null : o.toString(); }
    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof BigDecimal) return ((BigDecimal) o).doubleValue();
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0.0;
    }
    private String safe(String s) { return s == null ? "" : s; }
    private double n(Double d)    { return d == null ? 0.0 : d; }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static class WhereClause {
        final String sql;
        final Map<String, Object> params;
        final String statusSql;
        final Map<String, Object> statusParams;
        WhereClause(String sql, Map<String, Object> params,
                    String statusSql, Map<String, Object> statusParams) {
            this.sql = sql; this.params = params;
            this.statusSql = statusSql; this.statusParams = statusParams;
        }
    }
}
