package com.ec.application.service;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SmartSuggestionWriterTest {

    @Test
    void numbersAndLists() {
        assertEquals("12,34,567", SmartSuggestionWriter.num(1234567, 2));
        assertEquals("385.5", SmartSuggestionWriter.num(385.50, 2));
        assertEquals("120", SmartSuggestionWriter.num(120.0, 2));
        assertEquals("₹1,00,000", SmartSuggestionWriter.money(100000));
        assertEquals("A, B and C", SmartSuggestionWriter.joinAnd(Arrays.asList("A", "B", "C")));
        assertEquals("A and B", SmartSuggestionWriter.joinAnd(Arrays.asList("A", "B")));
    }

    @Test
    void indentFlagsOnlyRealConcerns() {
        Map<String, Object> cement = item("Cement", "Bags", 100);
        cement.put("stockInThisProject", 120.0);
        cement.put("recentSimilarIndents", Collections.singletonList(map("indent", "IN-9", "date", "21-Sep-2026", "qty", 80.0, "status", "APPROVED")));
        Map<String, Object> sand = item("Sand", "Brass", 10);
        sand.put("stockInOtherProjects", Collections.singletonList(map("project", "Site B", "qty", 4.0)));   // info only

        String text = SmartSuggestionWriter.indent(map("indentNumber", "IND-10", "items", Arrays.asList(cement, sand)));
        System.out.println(text);
        assertTrue(text.startsWith("**1 of 2 items needs a look before approving.**"), text);
        assertTrue(text.contains("120 Bags in stock here, more than requested."), text);
        assertTrue(text.contains("IN-9 on 21-Sep-2026 (80 Bags)"), text);
        assertFalse(text.contains("₹"), "indent approvers (project managers) must not see prices: " + text);
        assertTrue(text.contains("Looks fine:\n- Sand (10 Brass) — Also in stock at Site B (4 Brass)."), text);
        assertTrue(text.contains("**Suggestions:**\n1. Check with the site that this isn't a repeat of indent IN-9.\n"
                + "2. Confirm the need for Cement with the site — stock here is more than requested."), text);
    }

    @Test
    void indentAllFine() {
        String text = SmartSuggestionWriter.indent(map("indentNumber", "IND-11", "items", Collections.singletonList(item("Bricks", "Nos", 500))));
        assertTrue(text.contains("stock, BOQ and recent indents checked"), text);
        assertTrue(text.contains("- Bricks (500 Nos) — Not in stock at any project, so it needs to be bought."), text);
        assertTrue(text.contains("approve."), text);
        assertFalse(text.contains("\n\n\n"), "no stray blank lines: " + text);
    }

    @Test
    void partialStockIsInfoOnly() {
        String text = SmartSuggestionWriter.indent(map("indentNumber", "IND-13", "items",
                Collections.singletonList(item("Wall Putty", "BAG", 400, "stockInThisProject", 195.0, "stockAgeDays", 128))));
        assertTrue(text.contains("looks fine") || text.contains("Nothing stands out"), text);
        assertTrue(text.contains("195 BAG in stock here; some of it has been sitting for 128 days."), text);
        assertFalse(text.contains("reducing"), text);
    }

    @Test
    void freshStockMayBeForOtherWork() {
        String text = SmartSuggestionWriter.indent(map("indentNumber", "IND-14", "items",
                Collections.singletonList(item("Red Bricks", "NOS", 5000, "stockInThisProject", 8000.0, "stockAgeDays", 12))));
        assertTrue(text.contains("8,000 NOS in stock here, more than requested, but all of it arrived in the last 12 days (it may be meant for other work)."), text);
        assertTrue(text.contains("**Suggestion:** check with the site whether the stock of Red Bricks here is already set aside for other work."), text);
    }

    @Test
    void indentAlreadyOnOrder() {
        Map<String, Object> cement = item("Cement", "Bags", 200);
        cement.put("openPurchaseOrders", Collections.singletonList(map("po", "PO-120", "date", "01-Sep-2026", "pending", 300.0, "received", 0.0)));
        String text = SmartSuggestionWriter.indent(map("indentNumber", "IND-12", "items", Collections.singletonList(cement)));
        assertTrue(text.contains("- **Cement (200 Bags)** — Not in stock at any project. 300 Bags still to arrive on PO-120 (raised 01-Sep-2026, nothing received yet)."), text);
        assertTrue(text.contains("**Suggestion:** check whether PO-120 already covers this need."), text);
    }

    @Test
    void poPartiallyReceived() {
        Map<String, Object> data = map("poNumber", "PO-5", "status", "PARTIAL", "poDate", "01-Sep-2026",
                "supplier", "Shree Traders", "project", "Bhaav Bhumi", "grandTotalRupees", 1234567,
                "lines", Arrays.asList(
                        map("item", "Cement", "unit", "Bags", "ordered", 100.0, "received", 60.0),
                        map("item", "Sand", "unit", "Brass", "ordered", 10.0, "received", 10.0)),
                "statusHistory", Collections.singletonList(map("date", "10-Sep-2026", "from", "NEW", "to", "PARTIAL", "by", "ravi")));
        String text = SmartSuggestionWriter.po(data);
        System.out.println(text);
        assertTrue(text.startsWith("**1 of 2 items fully received; 1 still pending.**"), text);
        assertTrue(text.contains("60 of 100 Bags received, **40 Bags pending**"), text);
        assertTrue(text.contains("Fully received: Sand."), text);
        assertTrue(text.contains("PO value **₹12,34,567** with Shree Traders for Bhaav Bhumi, raised on 01-Sep-2026."), text);
        assertTrue(text.contains("Last status change 10-Sep-2026 by ravi: New → Partial."), text);
    }

    @Test
    void poWithoutValueForPriceRestrictedViewers() {
        Map<String, Object> data = map("poNumber", "PO-6", "status", "NEW", "poDate", "01-Sep-2026", "supplier", "Shree Traders",
                "project", "Bhaav Bhumi", "grandTotalRupees", null,
                "lines", Collections.singletonList(map("item", "Cement", "unit", "Bags", "ordered", 100.0, "received", 0.0)),
                "statusHistory", Collections.emptyList());
        String text = SmartSuggestionWriter.po(data);
        assertTrue(text.contains("PO with Shree Traders for Bhaav Bhumi, raised on 01-Sep-2026."), text);
        assertFalse(text.contains("₹"), text);
    }

    @Test
    void attentionLines() {
        assertEquals("**2 indents are waiting for approval** — oldest IN-1540 (Mangalam City, 1 day).",
                SmartSuggestionWriter.attention(map("kind", "INDENT_APPROVAL", "count", 2, "oldestId", "IN-1540", "oldestProject", "Mangalam City", "oldestDays", 1)));
        assertEquals("**1 PO has nothing received after 30+ days** — PO-418 (Sai Bricks, City Center, 185 days).",
                SmartSuggestionWriter.attention(map("kind", "PO_NOTHING_RECEIVED", "count", 1, "oldestId", "PO-418", "oldestSupplier", "Sai Bricks", "oldestProject", "City Center", "oldestDays", 185)));
        assertTrue(SmartSuggestionWriter.attention(map("kind", "PO_NO_PROGRESS", "count", 117, "oldestId", "PO-9", "oldestDays", 190))
                .endsWith("Consider following up or short-closing."));
    }

    private static Map<String, Object> item(String name, String unit, double qty, Object... extra) {
        Map<String, Object> m = map("item", name, "unit", unit, "requestedQty", qty);
        m.putAll(map(extra));
        return m;
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
