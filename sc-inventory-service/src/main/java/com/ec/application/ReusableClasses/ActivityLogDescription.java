package com.ec.application.ReusableClasses;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds structured JSON descriptions for activity log entries.
 * Frontend parses and renders as a mini-table.
 * Falls back to plain summary string on serialisation failure.
 */
public final class ActivityLogDescription {

    private static final ObjectMapper mapper = new ObjectMapper();

    private ActivityLogDescription() {}

    /** Summary only — DELETE, header-only UPDATE */
    public static String of(String summary) {
        try {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("summary", summary);
            return mapper.writeValueAsString(d);
        } catch (Exception e) {
            return summary;
        }
    }

    /** Summary + inward type badge + items — INWARD CREATE */
    public static String withItemsAndType(String summary, String inwardType, List<Map<String, Object>> items) {
        try {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("summary", summary);
            if (inwardType != null && !inwardType.isEmpty()) {
                d.put("inwardType", inwardType);
            }
            if (items != null && !items.isEmpty()) {
                d.put("items", items);
            }
            return mapper.writeValueAsString(d);
        } catch (Exception e) {
            return summary;
        }
    }

    /** Summary + items — CREATE, REJECT, RETURN */
    public static String withItems(String summary, List<Map<String, Object>> items) {
        try {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("summary", summary);
            if (items != null && !items.isEmpty()) {
                d.put("items", items);
            }
            return mapper.writeValueAsString(d);
        } catch (Exception e) {
            return summary;
        }
    }

    /** Single-quantity item (CREATE / REJECT / RETURN) */
    public static Map<String, Object> item(String product, double qty) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", product);
        m.put("qty", fmt(qty));
        return m;
    }

    /** Old → new quantity item (UPDATE) */
    public static Map<String, Object> itemChanged(String product, double oldQty, double newQty) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", product);
        m.put("oldQty", fmt(oldQty));
        m.put("newQty", fmt(newQty));
        return m;
    }

    public static List<Map<String, Object>> list() {
        return new ArrayList<>();
    }

    private static String fmt(double qty) {
        return qty == Math.floor(qty) ? String.valueOf((int) qty) : String.valueOf(qty);
    }
}
