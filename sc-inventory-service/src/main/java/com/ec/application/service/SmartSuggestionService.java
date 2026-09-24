package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.data.ProductBOQSummaryDto;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

/**
 * "Smart suggestion" on indent details (next to Approve) and PO details: gathers the facts with plain SQL and
 * SmartSuggestionWriter turns them into sentences. No AI, no cost, no daily limit.
 * Not transactional on purpose — indentCheck briefly switches tenant for BOQ lookups.
 */
@Service
public class SmartSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SmartSuggestionService.class);

    @Autowired private EntityManager em;
    @Autowired private SchemaConfig schemaConfig;
    @Autowired private BOQService boqService;


    public Map<String, Object> poSummary(String poId, boolean showValue) throws Exception {
        String m = schemaConfig.getMasterSchema();
        List<Object[]> header = rows(
            "SELECT po.purchase_order_id, DATE_FORMAT(po.po_date, '%d-%b-%Y'), po.status, po.project_name, s.name," +
            "       f.firm_name, ROUND(po.grandTotal), po.subject, po.shortCloseReason" +
            " FROM " + m + ".purchase_order po" +
            " LEFT JOIN " + m + ".contacts s ON s.contactId = po.supplier_id" +
            " LEFT JOIN " + m + ".Firm f ON f.firmId = po.firm_id" +
            " WHERE po.purchase_order_id = :id AND po.is_deleted = 0", "id", poId);
        if (header.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PO not found");
        Object[] h = header.get(0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("poNumber", h[0]);
        data.put("poDate", h[1]);
        data.put("status", h[2]);
        data.put("project", h[3]);
        data.put("supplier", h[4]);
        data.put("firm", h[5]);
        data.put("grandTotalRupees", showValue ? h[6] : null);
        data.put("subject", h[7]);
        data.put("shortCloseReason", h[8]);

        // Ordered per product vs received through the indent lines linked to this PO (same source as PO reconciliation).
        List<Map<String, Object>> lines = new ArrayList<>();
        for (Object[] r : rows(
            "SELECT p.product_name, p.measurementUnit, SUM(pol.quantity), MAX(pol.rate)," +
            "       (SELECT COALESCE(SUM(iie.quantity_received), 0) FROM " + m + ".indent_inventory_entries iie" +
            "         WHERE iie.purchaseOrderId = :id AND iie.productId = pol.product_id AND iie.is_deleted = 0)" +
            " FROM " + m + ".purchase_order_line pol" +
            " JOIN " + m + ".Product p ON p.productId = pol.product_id" +
            " WHERE pol.po_id = :id AND pol.is_deleted = 0" +
            " GROUP BY pol.product_id, p.product_name, p.measurementUnit" +
            " ORDER BY p.product_name", "id", poId)) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("item", r[0]);
            line.put("unit", r[1]);
            line.put("ordered", r[2]);
            line.put("rate", r[3]);
            line.put("received", r[4]);
            lines.add(line);
        }
        data.put("lines", lines);

        List<Map<String, Object>> history = new ArrayList<>();
        for (Object[] r : rows(
            "SELECT DATE_FORMAT(changedAt, '%d-%b-%Y'), oldStatus, newStatus, changedBy, changeMessage" +
            " FROM " + m + ".po_status_history WHERE purchase_order_id = :id ORDER BY changedAt, id", "id", poId)) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("date", r[0]);
            e.put("from", r[1]);
            e.put("to", r[2]);
            e.put("by", r[3]);
            e.put("note", r[4]);
            history.add(e);
        }
        data.put("statusHistory", history);

        return Collections.singletonMap("text", SmartSuggestionWriter.po(data));
    }

    public Map<String, Object> indentCheck(String indentId) throws Exception {
        String m = schemaConfig.getMasterSchema();
        List<Object[]> header = rows(
            "SELECT indent_id, tenant, indent_status, DATE_FORMAT(indent_date, '%d-%b-%Y'), required_by" +
            " FROM " + m + ".indent_inventory WHERE indent_id = :id AND is_deleted = 0", "id", indentId);
        if (header.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Indent not found");
        Object[] h = header.get(0);
        String tenant = (String) h[1];
        Map<String, String> projectNames = projectNames();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("indentNumber", h[0]);
        data.put("project", projectNames.getOrDefault(tenant, tenant));
        data.put("status", h[2]);
        data.put("indentDate", h[3]);
        data.put("requiredBy", h[4]);

        // Items keyed by productId; real demand only (no cancelled / split lines).
        Map<Long, Map<String, Object>> items = new LinkedHashMap<>();
        for (Object[] r : rows(
            "SELECT iie.productId, p.product_name, iie.quantity, iie.measurement_unit, iie.remarks" +
            " FROM " + m + ".indent_inventory_entries iie" +
            " JOIN " + m + ".Product p ON p.productId = iie.productId" +
            " WHERE iie.indent_id = :id AND iie.is_deleted = 0 AND iie.line_item_status NOT IN ('CANCELLED','SPLIT')" +
            " ORDER BY iie.entryid", "id", indentId)) {
            Long pid = ((Number) r[0]).longValue();
            Map<String, Object> item = items.computeIfAbsent(pid, k -> {
                Map<String, Object> it = new LinkedHashMap<>();
                it.put("item", r[1]);
                it.put("unit", r[3]);
                it.put("requestedQty", 0.0);
                return it;
            });
            item.put("requestedQty", (Double) item.get("requestedQty") + toDouble(r[2]));
            if (r[4] != null && !r[4].toString().trim().isEmpty()) item.put("remarks", r[4]);
        }
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This indent has no active items");
        List<Long> pids = new ArrayList<>(items.keySet());

        // Stock snapshot across projects (master sync tables — one query each, no tenant switching).
        for (Object[] r : rows(
            "SELECT productId, tenantSchema, ROUND(SUM(quantityInHand), 2) FROM " + m + ".stock_summary" +
            " WHERE productId IN (:pids) AND quantityInHand > 0 GROUP BY productId, tenantSchema ORDER BY tenantSchema",
            "pids", pids)) {
            Map<String, Object> item = items.get(((Number) r[0]).longValue());
            String code = (String) r[1];
            if (code.equals(tenant)) item.put("stockInThisProject", r[2]);
            else addTo(item, "stockInOtherProjects", project(projectNames, code, r[2]));
        }
        // Age of the oldest stock still on hand here (FIFO, from the stock aging sync).
        for (Object[] r : rows(
            "SELECT productId, MAX(minAgingDays) FROM " + m + ".global_stock_aging_report" +
            " WHERE tenantSchema = :tenant AND isDeleted = 0 AND productId IN (:pids) AND minAgingDays < 9999" +
            " GROUP BY productId", "tenant", tenant, "pids", pids)) {
            Map<String, Object> item = items.get(((Number) r[0]).longValue());
            if (item != null) item.put("stockAgeDays", ((Number) r[1]).intValue());
        }
        for (Object[] r : rows(
            "SELECT productId, tenantSchema, ROUND(SUM(qty), 2) FROM " + m + ".global_dead_stock_report" +
            " WHERE productId IN (:pids) AND tenantSchema <> :tenant GROUP BY productId, tenantSchema ORDER BY tenantSchema",
            "pids", pids, "tenant", tenant)) {
            addTo(items.get(((Number) r[0]).longValue()), "deadStockInOtherProjects", project(projectNames, (String) r[1], r[2]));
        }

        for (Object[] r : rows(
            "SELECT iie.productId, ii.indent_id, DATE_FORMAT(ii.indent_date, '%d-%b-%Y'), ROUND(SUM(iie.quantity), 2), ii.indent_status" +
            " FROM " + m + ".indent_inventory_entries iie" +
            " JOIN " + m + ".indent_inventory ii ON ii.indent_id = iie.indent_id AND ii.is_deleted = 0" +
            " WHERE ii.tenant = :tenant AND ii.indent_id <> :id AND iie.is_deleted = 0 AND iie.productId IN (:pids)" +
            "   AND ii.indent_status NOT IN ('CANCELLED','REJECTED') AND iie.line_item_status NOT IN ('CANCELLED','SPLIT')" +
            "   AND ii.indent_date >= CURDATE() - INTERVAL 30 DAY" +
            " GROUP BY iie.productId, ii.indent_id, ii.indent_date, ii.indent_status" +   // one entry per indent, even with split lines
            " ORDER BY ii.indent_date DESC, ii.indent_id", "tenant", tenant, "id", indentId, "pids", pids)) {
            Map<String, Object> similar = new LinkedHashMap<>();
            similar.put("indent", r[1]);
            similar.put("date", r[2]);
            similar.put("qty", r[3]);
            similar.put("status", r[4]);
            addTo(items.get(((Number) r[0]).longValue()), "recentSimilarIndents", similar);
        }

        // Already ordered for this project and still to arrive: open POs (New/Partial) on indent lines not yet fully received.
        for (Object[] r : rows(
            "SELECT iie.productId, po.purchase_order_id, DATE_FORMAT(po.po_date, '%d-%b-%Y')," +
            "       ROUND(SUM(iie.quantity - COALESCE(iie.quantity_received, 0)), 2), ROUND(SUM(COALESCE(iie.quantity_received, 0)), 2)" +
            " FROM " + m + ".indent_inventory_entries iie" +
            " JOIN " + m + ".indent_inventory ii ON ii.indent_id = iie.indent_id AND ii.is_deleted = 0" +
            " JOIN " + m + ".purchase_order po ON po.purchase_order_id = iie.purchaseOrderId AND po.is_deleted = 0" +
            "   AND po.status IN ('NEW','PARTIAL')" +
            " WHERE ii.tenant = :tenant AND ii.indent_id <> :id AND iie.is_deleted = 0 AND iie.productId IN (:pids)" +
            "   AND iie.line_item_status IN ('PO CREATED','INWARD PARTIAL')" +
            " GROUP BY iie.productId, po.purchase_order_id, po.po_date" +
            " HAVING SUM(iie.quantity - COALESCE(iie.quantity_received, 0)) > 0" +
            " ORDER BY po.po_date, po.purchase_order_id", "tenant", tenant, "id", indentId, "pids", pids)) {
            Map<String, Object> open = new LinkedHashMap<>();
            open.put("po", r[1]);
            open.put("date", r[2]);
            open.put("pending", r[3]);
            open.put("received", r[4]);
            addTo(items.get(((Number) r[0]).longValue()), "openPurchaseOrders", open);
        }

        addBoq(items, tenant);
        data.put("items", new ArrayList<>(items.values()));

        return Collections.singletonMap("text", SmartSuggestionWriter.indent(data));
    }

    /**
     * Create PO: open POs (New/Partial) for these products across all projects, with quantity still to arrive.
     * Project comes from the indent lines on the PO (always set) — purchase_order.project_name is empty on older POs
     * and only used as a fallback name. excludePoId = the PO being edited / extended, so it doesn't list itself.
     */
    public List<Map<String, Object>> openPurchaseOrders(List<Long> productIds, String excludePoId) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyList();
        String m = schemaConfig.getMasterSchema();
        Map<String, String> projectNames = projectNames();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows(
            "SELECT iie.productId, po.purchase_order_id, ii.tenant, MAX(po.project_name), MAX(s.name)," +
            "       DATE_FORMAT(po.po_date, '%d-%m-%Y'), MAX(iie.measurement_unit)," +
            "       ROUND(SUM(iie.quantity - COALESCE(iie.quantity_received, 0)), 2), ROUND(SUM(COALESCE(iie.quantity_received, 0)), 2)" +
            " FROM " + m + ".indent_inventory_entries iie" +
            " JOIN " + m + ".indent_inventory ii ON ii.indent_id = iie.indent_id AND ii.is_deleted = 0" +
            " JOIN " + m + ".purchase_order po ON po.purchase_order_id = iie.purchaseOrderId AND po.is_deleted = 0" +
            "   AND po.status IN ('NEW','PARTIAL')" +
            " LEFT JOIN " + m + ".contacts s ON s.contactId = po.supplier_id" +
            " WHERE iie.is_deleted = 0 AND iie.productId IN (:pids) AND po.purchase_order_id <> :exclude" +
            "   AND iie.line_item_status IN ('PO CREATED','INWARD PARTIAL')" +
            " GROUP BY iie.productId, po.purchase_order_id, ii.tenant, po.po_date" +
            " HAVING SUM(iie.quantity - COALESCE(iie.quantity_received, 0)) > 0" +
            " ORDER BY po.po_date DESC, po.purchase_order_id DESC",
            "pids", productIds, "exclude", excludePoId == null ? "" : excludePoId)) {
            String tenant = (String) r[2];
            String fallback = r[3] == null || r[3].toString().trim().isEmpty() ? tenant : r[3].toString();
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("productId", ((Number) r[0]).longValue());
            o.put("po", r[1]);
            o.put("project", projectNames.getOrDefault(tenant, fallback));
            o.put("supplier", r[4]);
            o.put("date", r[5]);
            o.put("unit", r[6]);
            o.put("pending", r[7]);
            o.put("received", r[8]);
            out.add(o);
        }
        return out;
    }

    /**
     * Global Dashboard "Needs attention" banner: counts + the oldest example per item, limited to the user's projects.
     * showApprovals = can approve indents; showPurchasing = buys (admin / purchase-manager). Thresholds: 7 / 30 / 60 days.
     */
    public List<Map<String, Object>> attention(List<String> projects, boolean showApprovals, boolean showPurchasing) {
        List<Map<String, Object>> lines = new ArrayList<>();
        if (projects == null || projects.isEmpty()) return lines;
        String m = schemaConfig.getMasterSchema();
        Map<String, String> names = projectNames();
        // PO has no reliable project column (older POs), so a PO belongs to the projects of its indent lines.
        String poInProjects = " AND EXISTS (SELECT 1 FROM " + m + ".indent_inventory_entries e JOIN " + m + ".indent_inventory i" +
                " ON i.indent_id = e.indent_id WHERE e.purchaseOrderId = po.purchase_order_id AND e.is_deleted = 0 AND i.tenant IN (:t))";
        String poProject = "(SELECT MIN(i.tenant) FROM " + m + ".indent_inventory_entries e JOIN " + m + ".indent_inventory i" +
                " ON i.indent_id = e.indent_id WHERE e.purchaseOrderId = po.purchase_order_id AND e.is_deleted = 0)";

        if (showApprovals) {
            addAttention(lines, "INDENT_APPROVAL", names, rows(
                "SELECT indent_id, tenant, DATEDIFF(CURDATE(), indent_date), COUNT(*) OVER () FROM " + m + ".indent_inventory" +
                " WHERE is_deleted = 0 AND indent_status = 'NEW' AND tenant IN (:t) ORDER BY indent_date, indent_id LIMIT 1", "t", projects));
        }
        if (showPurchasing) {
            addAttention(lines, "INDENT_NO_PO", names, rows(
                "SELECT indent_id, tenant, DATEDIFF(CURDATE(), last_status_updated_at), COUNT(*) OVER () FROM " + m + ".indent_inventory" +
                " WHERE is_deleted = 0 AND indent_status = 'APPROVED' AND tenant IN (:t)" +
                "   AND DATEDIFF(CURDATE(), last_status_updated_at) > 7 ORDER BY last_status_updated_at LIMIT 1", "t", projects));
            addAttention(lines, "PO_NOTHING_RECEIVED", names, rows(
                "SELECT po.purchase_order_id, " + poProject + ", DATEDIFF(CURDATE(), po.po_date), COUNT(*) OVER (), s.name" +
                " FROM " + m + ".purchase_order po LEFT JOIN " + m + ".contacts s ON s.contactId = po.supplier_id" +
                " WHERE po.is_deleted = 0 AND po.status = 'NEW' AND DATEDIFF(CURDATE(), po.po_date) > 30" + poInProjects +
                " ORDER BY po.po_date, po.purchase_order_id LIMIT 1", "t", projects));
            addAttention(lines, "PO_NO_PROGRESS", names, rows(
                "SELECT po.purchase_order_id, " + poProject + ", DATEDIFF(CURDATE(), po.last_status_updated_at), COUNT(*) OVER (), s.name" +
                " FROM " + m + ".purchase_order po LEFT JOIN " + m + ".contacts s ON s.contactId = po.supplier_id" +
                " WHERE po.is_deleted = 0 AND po.status = 'PARTIAL' AND DATEDIFF(CURDATE(), po.last_status_updated_at) > 60" + poInProjects +
                " ORDER BY po.last_status_updated_at, po.purchase_order_id LIMIT 1", "t", projects));
        }
        return lines;
    }

    private void addAttention(List<Map<String, Object>> lines, String kind, Map<String, String> names, List<Object[]> found) {
        if (found.isEmpty()) return;
        Object[] r = found.get(0);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("kind", kind);
        line.put("count", ((Number) r[3]).intValue());
        line.put("oldestId", r[0]);
        line.put("oldestProject", r[1] == null ? null : names.getOrDefault((String) r[1], (String) r[1]));
        line.put("oldestDays", ((Number) r[2]).intValue());
        line.put("oldestSupplier", r.length > 4 ? r[4] : null);
        line.put("text", SmartSuggestionWriter.attention(line));
        lines.add(line);
    }

    // ── internals ─────────────────────────────────────────────────────────────

    /** BOQ remaining per item, reusing BOQService (tenant-scoped via ThreadLocal) — tenant switched only for these calls. */
    private void addBoq(Map<Long, Map<String, Object>> items, String tenant) {
        if (!schemaConfig.isValidSchema(tenant)) return;
        String original = ThreadLocalStorage.getTenantName();
        try {
            ThreadLocalStorage.setTenantName(tenant);
            for (Map.Entry<Long, Map<String, Object>> e : items.entrySet()) {
                ProductBOQSummaryDto boq = boqService.getProductBOQSummary(e.getKey());
                if (boq == null || !boq.isHasBOQ()) continue;
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("planned", round(boq.getTotalPlanned()));
                b.put("alreadyIndented", round(boq.getTotalConsumed()));
                b.put("remaining", round(boq.getRemaining()));
                e.getValue().put("boq", b);
            }
        } catch (Exception ex) {
            log.warn("BOQ lookup failed for tenant {}: {}", tenant, ex.getMessage());   // suggestion still useful without BOQ
        } finally {
            ThreadLocalStorage.setTenantName(original);
        }
    }

    /** tenant code → display name; falls back to codes if common.tenant is not readable. */
    private Map<String, String> projectNames() {
        Map<String, String> names = new HashMap<>();
        try {
            for (Object[] r : rows("SELECT name, tenant_long_name FROM common.tenant")) {
                if (r[0] != null && r[1] != null) names.put(r[0].toString(), r[1].toString());
            }
        } catch (Exception e) {
            log.warn("Could not read project names: {}", e.getMessage());
        }
        return names;
    }

    private Map<String, Object> project(Map<String, String> names, String code, Object qty) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("project", names.getOrDefault(code, code));
        p.put("qty", qty);
        return p;
    }

    @SuppressWarnings("unchecked")
    private void addTo(Map<String, Object> item, String key, Object value) {
        if (item == null) return;
        ((List<Object>) item.computeIfAbsent(key, k -> new ArrayList<>())).add(value);
    }

    /** Native query with alternating name/value params; single-column results are wrapped as Object[]. */
    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Object... params) {
        Query q = em.createNativeQuery(sql);
        for (int i = 0; i < params.length; i += 2) q.setParameter((String) params[i], params[i + 1]);
        List<Object[]> out = new ArrayList<>();
        for (Object row : (List<Object>) q.getResultList()) {
            out.add(row instanceof Object[] ? (Object[]) row : new Object[]{row});
        }
        return out;
    }

    private static double toDouble(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static Double round(Double d) {
        return d == null ? null : Math.round(d * 100.0) / 100.0;
    }
}
