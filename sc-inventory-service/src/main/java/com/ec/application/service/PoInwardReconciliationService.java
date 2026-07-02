package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.data.PoInwardReconciliationRow;
import com.ec.application.config.SchemaConfig;
import com.ec.application.multitenant.ThreadLocalStorage;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class PoInwardReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PoInwardReconciliationService.class);

    private final EntityManager em;
    private final UserDetailsService userDetailsService;
    private final BOQService boqService;
    private final SchemaConfig schemaConfig;

    private static final String BASE_FROM =
        " FROM purchase_order po" +
        " JOIN purchase_order_line pol ON pol.po_id = po.purchase_order_id AND pol.is_deleted = 0" +
        " JOIN Product p ON p.productId = pol.product_id AND p.is_deleted = 0" +
        " LEFT JOIN Category cat ON cat.categoryId = p.categoryId AND cat.is_deleted = 0" +
        " LEFT JOIN Firm f ON f.firmId = po.firm_id AND f.is_deleted = 0" +
        " LEFT JOIN indent_inventory_entries iie" +
        "   ON iie.purchaseOrderId = po.purchase_order_id" +
        "  AND iie.productId = pol.product_id AND iie.is_deleted = 0" +
        " LEFT JOIN indent_inventory ii ON ii.indent_id = iie.indent_id AND ii.is_deleted = 0" +
        " WHERE po.is_deleted = 0";

    private static final String GROUP_BY =
        " GROUP BY po.purchase_order_id, pol.product_id," +
        " COALESCE(ii.tenant, po.project_name, 'Unknown')";

    private static final String RECON_STATUS_CASE =
        " CASE WHEN COALESCE(SUM(iie.quantity_received), 0) <= 0 THEN 'NOT_STARTED'" +
        "      WHEN COALESCE(SUM(iie.quantity_received), 0) >= MAX(pol.quantity) THEN 'COMPLETE'" +
        "      ELSE 'PARTIAL' END";

    private static final String DATA_SELECT =
        "SELECT" +
        "  COALESCE(ii.tenant, po.project_name, 'Unknown') AS project," +
        "  po.purchase_order_id," +
        "  po.po_date," +
        "  po.status," +
        "  f.firm_name AS supplier," +
        "  p.product_name," +
        "  p.product_code," +
        "  p.measurementUnit AS unit," +
        "  MAX(pol.quantity) AS ordered_qty," +
        "  COALESCE(SUM(iie.quantity_received), 0) AS received_qty," +
        "  COALESCE(p.lead_time_days, cat.lead_time_days) AS lead_time_days," +
        "  CASE WHEN COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL" +
        "            AND po.status NOT IN ('CANCELLED','COMPLETE INWARD','SHORT CLOSED','SHORT CLOSE')" +
        "       THEN (DATEDIFF(CURDATE(), po.po_date) - COALESCE(p.lead_time_days, cat.lead_time_days))" +
        "       ELSE NULL END AS days_overdue";

    // ── Public API ────────────────────────────────────────────────────────────

    public Page<PoInwardReconciliationRow> getPage(FilterDataList filters, Pageable pageable) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        WhereClause wc = buildWhere(filters, allowedSchemas);

        // Count
        String countSql = "SELECT COUNT(*) FROM (" +
                DATA_SELECT + BASE_FROM + wc.where + GROUP_BY + wc.having + ") cnt_sub";
        Query countQ = em.createNativeQuery(countSql);
        applyParams(countQ, wc.params);
        applyParams(countQ, wc.havingParams);
        long total = ((Number) countQ.getSingleResult()).longValue();

        // Data
        String dataSql = DATA_SELECT + BASE_FROM + wc.where + GROUP_BY + wc.having +
                buildOrderBy(pageable) +
                " LIMIT " + pageable.getPageSize() + " OFFSET " + pageable.getOffset();
        Query dataQ = em.createNativeQuery(dataSql);
        applyParams(dataQ, wc.params);
        applyParams(dataQ, wc.havingParams);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();
        List<PoInwardReconciliationRow> content = rows.stream().map(this::map).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }

    public Map<String, Long> getSummaryStats(FilterDataList filters) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        WhereClause wc = buildWhere(filters, allowedSchemas);
        // Use WHERE filters only (no HAVING/status filter) so tile counts always reflect true totals
        String innerSql = DATA_SELECT + BASE_FROM + wc.where + GROUP_BY;
        String sql = "SELECT" +
                "  SUM(CASE WHEN received_qty <= 0 THEN 1 ELSE 0 END) AS not_started," +
                "  SUM(CASE WHEN received_qty > 0 AND received_qty < ordered_qty THEN 1 ELSE 0 END) AS partial," +
                "  SUM(CASE WHEN received_qty >= ordered_qty THEN 1 ELSE 0 END) AS complete" +
                " FROM (" + innerSql + ") stats_sub";
        Query q = em.createNativeQuery(sql);
        applyParams(q, wc.params);
        Object[] r = (Object[]) q.getSingleResult();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("NOT_STARTED", r[0] != null ? ((Number) r[0]).longValue() : 0L);
        stats.put("PARTIAL",     r[1] != null ? ((Number) r[1]).longValue() : 0L);
        stats.put("COMPLETE",    r[2] != null ? ((Number) r[2]).longValue() : 0L);
        return stats;
    }

    public List<String> getDistinctProjects() {
        String sql = "SELECT DISTINCT COALESCE(ii.tenant, po.project_name) AS proj" +
                " FROM purchase_order po" +
                " LEFT JOIN indent_inventory_entries iie ON iie.purchaseOrderId = po.purchase_order_id AND iie.is_deleted = 0" +
                " LEFT JOIN indent_inventory ii ON ii.indent_id = iie.indent_id AND ii.is_deleted = 0" +
                " WHERE po.is_deleted = 0 AND COALESCE(ii.tenant, po.project_name) IS NOT NULL" +
                " ORDER BY proj";
        @SuppressWarnings("unchecked")
        List<String> result = em.createNativeQuery(sql).getResultList();
        return result;
    }

    public List<String> getDistinctProducts() {
        String sql = "SELECT DISTINCT p.product_name" +
                " FROM purchase_order_line pol" +
                " JOIN Product p ON p.productId = pol.product_id AND p.is_deleted = 0" +
                " JOIN purchase_order po ON po.purchase_order_id = pol.po_id AND po.is_deleted = 0" +
                " WHERE pol.is_deleted = 0 ORDER BY p.product_name";
        @SuppressWarnings("unchecked")
        List<String> result = em.createNativeQuery(sql).getResultList();
        return result;
    }

    public void exportExcel(FilterDataList filters, HttpServletResponse response) throws Exception {
        List<String> allowedSchemas = userDetailsService.getCurrentUserAllowedSchemas();
        WhereClause wc = buildWhere(filters, allowedSchemas);
        String countSql = "SELECT COUNT(*) FROM (" + DATA_SELECT + BASE_FROM + wc.where + GROUP_BY + wc.having + ") cnt_sub";
        Query countQ = em.createNativeQuery(countSql);
        applyParams(countQ, wc.params);
        applyParams(countQ, wc.havingParams);
        long exportCount = ((Number) countQ.getSingleResult()).longValue();
        if (exportCount > 5000)
            throw new Exception("Too many rows to export. Please apply filters to reduce results below 5000 and try again.");
        String sql = DATA_SELECT + BASE_FROM + wc.where + GROUP_BY + wc.having + " ORDER BY po.po_date DESC, po.purchase_order_id ASC, p.product_name ASC";
        Query q = em.createNativeQuery(sql);
        applyParams(q, wc.params);
        applyParams(q, wc.havingParams);

        @SuppressWarnings("unchecked")
        List<Object[]> rawRows = q.getResultList();
        List<PoInwardReconciliationRow> mappedRows = rawRows.stream().map(this::map).collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("PO vs Inward Recon");
            CellStyle hdr = headerStyle(wb);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            String[] cols = {"Project", "PO Number", "PO Date", "PO Status", "Supplier",
                    "Product", "Code", "Unit", "Ordered Qty", "Received Qty", "Balance Qty",
                    "% Received", "Status"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(hdr);
                sheet.setColumnWidth(i, 22 * 256);
            }
            int rn = 1;
            for (PoInwardReconciliationRow row : mappedRows) {
                Row exRow = sheet.createRow(rn++);
                exRow.createCell(0).setCellValue(safe(row.getProject()));
                exRow.createCell(1).setCellValue(safe(row.getPurchaseOrderId()));
                exRow.createCell(2).setCellValue(row.getPoDate() != null ? sdf.format(row.getPoDate()) : "");
                exRow.createCell(3).setCellValue(safe(row.getPoStatus()));
                exRow.createCell(4).setCellValue(safe(row.getSupplier()));
                exRow.createCell(5).setCellValue(safe(row.getProductName()));
                exRow.createCell(6).setCellValue(safe(row.getProductCode()));
                exRow.createCell(7).setCellValue(safe(row.getUnit()));
                exRow.createCell(8).setCellValue(n(row.getOrderedQty()));
                exRow.createCell(9).setCellValue(n(row.getReceivedQty()));
                exRow.createCell(10).setCellValue(n(row.getBalanceQty()));
                exRow.createCell(11).setCellValue(row.getPercentReceived() != null
                        ? String.format("%.1f%%", row.getPercentReceived()) : "0%");
                exRow.createCell(12).setCellValue(safe(row.getReconciliationStatus()));
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=po_inward_reconciliation.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WhereClause buildWhere(FilterDataList filters, List<String> allowedSchemas) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        Map<String, Object> havingParams = new LinkedHashMap<>();

        StringBuilder having = new StringBuilder();

        if (allowedSchemas != null && !allowedSchemas.isEmpty()) {
            if (allowedSchemas.size() == 1) {
                sql.append(" AND COALESCE(ii.tenant, po.project_name, 'Unknown') = :as0");
                params.put("as0", allowedSchemas.get(0));
            } else {
                StringBuilder inClause = new StringBuilder(
                        " AND COALESCE(ii.tenant, po.project_name, 'Unknown') IN (");
                for (int i = 0; i < allowedSchemas.size(); i++) {
                    inClause.append(i == 0 ? "" : ",").append(":as").append(i);
                    params.put("as" + i, allowedSchemas.get(i));
                }
                inClause.append(")");
                sql.append(inClause);
            }
        }

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
                        sql.append(" AND COALESCE(ii.tenant, po.project_name, 'Unknown') = :project0");
                        params.put("project0", projects.get(0));
                    } else if (projects.size() > 1) {
                        StringBuilder inClause = new StringBuilder(
                                " AND COALESCE(ii.tenant, po.project_name, 'Unknown') IN (");
                        for (int pi = 0; pi < projects.size(); pi++) {
                            String pname = "project" + pi;
                            inClause.append(pi == 0 ? "" : ",").append(":").append(pname);
                            params.put(pname, projects.get(pi));
                        }
                        inClause.append(")");
                        sql.append(inClause);
                    }
                } else if ("poStatus".equals(f.getAttrName())) {
                    sql.append(" AND po.status = :poStatus");
                    params.put("poStatus", v);
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
                    sql.append(" AND po.po_date >= STR_TO_DATE(:startDate, '%d-%m-%Y')");
                    params.put("startDate", v);
                } else if ("endDate".equals(f.getAttrName())) {
                    sql.append(" AND po.po_date < DATE_ADD(STR_TO_DATE(:endDate, '%d-%m-%Y'), INTERVAL 1 DAY)");
                    params.put("endDate", v);
                } else if ("reconciliationStatus".equals(f.getAttrName())) {
                    List<String> statuses = f.getAttrValue().stream()
                            .filter(s -> s != null && !s.isEmpty())
                            .collect(java.util.stream.Collectors.toList());
                    if (statuses.size() == 1) {
                        having.append(" HAVING ").append(RECON_STATUS_CASE).append(" = :recon0");
                        havingParams.put("recon0", statuses.get(0));
                    } else if (statuses.size() > 1) {
                        StringBuilder inClause = new StringBuilder(" HAVING ")
                                .append(RECON_STATUS_CASE).append(" IN (");
                        for (int ri = 0; ri < statuses.size(); ri++) {
                            String pname = "recon" + ri;
                            inClause.append(ri == 0 ? "" : ",").append(":").append(pname);
                            havingParams.put(pname, statuses.get(ri));
                        }
                        inClause.append(")");
                        having.append(inClause);
                    }
                }
            }
        }
        return new WhereClause(sql.toString(), having.toString(), params, havingParams);
    }

    private String buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = pageable.getSort().iterator().next();
            String col = "po.po_date";
            String prop = order.getProperty();
            if ("project".equals(prop))          col = "COALESCE(ii.tenant, po.project_name, 'Unknown')";
            else if ("purchaseOrderId".equals(prop)) col = "po.purchase_order_id";
            else if ("poDate".equals(prop))      col = "po.po_date";
            else if ("poStatus".equals(prop))    col = "po.status";
            else if ("supplier".equals(prop))    col = "f.firm_name";
            else if ("productName".equals(prop)) col = "p.product_name";
            else if ("orderedQty".equals(prop))  col = "MAX(pol.quantity)";
            else if ("receivedQty".equals(prop)) col = "COALESCE(SUM(iie.quantity_received), 0)";
            else if ("balanceQty".equals(prop))  col = "(MAX(pol.quantity) - COALESCE(SUM(iie.quantity_received), 0))";
            return " ORDER BY " + col + " " + order.getDirection().name();
        }
        return " ORDER BY po.po_date DESC, po.purchase_order_id ASC, p.product_name ASC";
    }

    private void applyParams(Query q, Map<String, Object> params) {
        for (Map.Entry<String, Object> e : params.entrySet()) {
            q.setParameter(e.getKey(), e.getValue());
        }
    }

    private PoInwardReconciliationRow map(Object[] r) {
        PoInwardReconciliationRow row = new PoInwardReconciliationRow();
        row.setProject(str(r[0]));
        row.setPurchaseOrderId(str(r[1]));
        row.setPoDate(r[2] instanceof java.sql.Timestamp ? new Date(((java.sql.Timestamp) r[2]).getTime()) : null);
        row.setPoStatus(str(r[3]));
        row.setSupplier(str(r[4]));
        row.setProductName(str(r[5]));
        row.setProductCode(str(r[6]));
        row.setUnit(str(r[7]));
        double ordered  = toDouble(r[8]);
        double received = toDouble(r[9]);
        double balance  = ordered - received;
        row.setOrderedQty(ordered);
        row.setReceivedQty(received);
        row.setBalanceQty(balance);
        row.setPercentReceived(ordered > 0 ? (received / ordered) * 100 : 0.0);
        if (received <= 0)          row.setReconciliationStatus("NOT_STARTED");
        else if (received >= ordered) row.setReconciliationStatus("COMPLETE");
        else                          row.setReconciliationStatus("PARTIAL");
        // Lead time / overdue (r[10], r[11])
        if (r.length > 10 && r[10] != null) {
            row.setLeadTimeDays(((Number) r[10]).intValue());
        }
        if (r.length > 11 && r[11] != null) {
            int daysOverdue = ((Number) r[11]).intValue();
            row.setDaysOverdue(daysOverdue);
            row.setIsOverdue(daysOverdue > 0);
        }
        return row;
    }

    /**
     * For each unique project in the rows, temporarily switches to that tenant schema,
     * fetches effective BOQ totals per product, then enriches rows with boqPlannedQty.
     * Restores original tenant (master) in finally block.
     */
    private void enrichWithBOQ(List<PoInwardReconciliationRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        // group productName by project (tenantCode)
        Map<String, Set<String>> productsByTenant = new LinkedHashMap<>();
        for (PoInwardReconciliationRow row : rows) {
            String tenant = row.getProject();
            if (tenant == null || tenant.isEmpty() || "Unknown".equals(tenant)) continue;
            productsByTenant.computeIfAbsent(tenant, k -> new LinkedHashSet<>()).add(row.getProductName());
        }

        String originalTenant = ThreadLocalStorage.getTenantName();
        // boqMap: tenantCode -> (productName -> boqPlannedQty)
        Map<String, Map<String, Double>> boqMap = new HashMap<>();
        try {
            for (Map.Entry<String, Set<String>> entry : productsByTenant.entrySet()) {
                String tenant = entry.getKey();
                ThreadLocalStorage.setTenantName(tenant);
                try {
                    // Use cached BOQ rows (2-min cache per tenant context)
                    List<Object[]> boqRows = boqService.getCachedBOQStatusRows();
                    Map<String, Double> tenantBoqMap = new HashMap<>();
                    for (Object[] r : boqRows) {
                        String pName = r[8] != null ? r[8].toString() : null;
                        if (pName == null) continue;
                        double qty = r[10] != null ? ((Number) r[10]).doubleValue() : 0;
                        double wastage = r[12] != null ? ((Number) r[12]).doubleValue() : 0;
                        double effective = qty * (1 + wastage / 100.0);
                        tenantBoqMap.merge(pName, effective, Double::sum);
                    }
                    boqMap.put(tenant, tenantBoqMap);
                } catch (Exception e) {
                    log.warn("BOQ enrichment failed for tenant {}: {}", tenant, e.getMessage());
                }
            }
        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }

        for (PoInwardReconciliationRow row : rows) {
            Map<String, Double> tenantBoq = boqMap.get(row.getProject());
            if (tenantBoq == null || row.getProductName() == null) continue;
            Double planned = tenantBoq.get(row.getProductName());
            if (planned != null && planned > 0) row.setBoqPlannedQty(planned);
        }
    }

    private String str(Object o)   { return o == null ? null : o.toString(); }
    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof BigDecimal) return ((BigDecimal) o).doubleValue();
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0.0;
    }
    private String safe(String s)  { return s == null ? "" : s; }
    private double n(Double d)     { return d == null ? 0.0 : d; }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static class WhereClause {
        final String where;
        final String having;
        final Map<String, Object> params;        // WHERE clause params only
        final Map<String, Object> havingParams;  // HAVING clause params only
        WhereClause(String where, String having, Map<String, Object> params, Map<String, Object> havingParams) {
            this.where = where; this.having = having; this.params = params; this.havingParams = havingParams;
        }
    }
}
