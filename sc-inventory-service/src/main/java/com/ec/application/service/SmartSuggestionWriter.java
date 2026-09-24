package com.ec.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Turns the facts SmartSuggestionService gathers into a few plain-English lines (markdown).
 * Pure code, no AI: same data in → same text out. Wording varies a little per indent/PO number
 * so it doesn't read like a form letter, but never changes on refresh.
 */
final class SmartSuggestionWriter {

    private SmartSuggestionWriter() {
    }

    // ── Indent: shown next to Approve ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    static String indent(Map<String, Object> data) {
        String seed = String.valueOf(data.get("indentNumber"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

        List<String> concernLines = new ArrayList<>(), fineLines = new ArrayList<>();
        // Collected for the closing suggestions, most serious first: duplicate > already on order > stock covers > transfer > BOQ.
        Set<String> dupIndents = new LinkedHashSet<>(), openPos = new LinkedHashSet<>();
        // Stock isn't reserved in the ERP, so stock here may already be meant for other work: state facts, don't conclude.
        List<String> covered = new ArrayList<>(), coveredFresh = new ArrayList<>(), transfers = new ArrayList<>(), overBoq = new ArrayList<>();

        for (Map<String, Object> it : items) {
            String name = str(it.get("item"));
            String unit = str(it.get("unit"));
            double req = d(it.get("requestedQty"));
            List<String> notes = new ArrayList<>();
            // Info only, not a concern: partial stock here (sites keep topping up consumables), stock elsewhere, nothing anywhere.
            // No prices here: approvers are project managers.
            boolean concern = false;

            Double here = it.get("stockInThisProject") == null ? null : d(it.get("stockInThisProject"));
            Integer age = it.get("stockAgeDays") == null ? null : ((Number) it.get("stockAgeDays")).intValue();
            boolean fresh = age != null && age < 30;   // everything on hand arrived recently — likely for other indents
            String ageNote = age == null ? "" : fresh ? "all of it arrived in the last " + days(age) : "some of it has been sitting for " + days(age);
            if (here != null && here >= req) {
                notes.add(qty(here, unit) + " in stock here, more than requested"
                        + (age == null ? "." : fresh ? ", but " + ageNote + " (it may be meant for other work)." : "; " + ageNote + "."));
                (fresh ? coveredFresh : covered).add(name);
                concern = true;
            } else if (here != null && here > 0) {
                notes.add(qty(here, unit) + " in stock here" + (age == null ? "." : "; " + ageNote + "."));
            }

            List<Map<String, Object>> onOrder = (List<Map<String, Object>>) it.get("openPurchaseOrders");
            List<Map<String, Object>> dead = (List<Map<String, Object>>) it.get("deadStockInOtherProjects");
            List<Map<String, Object>> other = (List<Map<String, Object>>) it.get("stockInOtherProjects");
            if (dead != null && !dead.isEmpty()) {
                notes.add(pick(seed, 5, "unused stock at other projects: ", "lying unused elsewhere: ")
                        + projectQtys(dead, unit) + " — could be transferred instead of buying.");
                transfers.add(name + " from " + str(dead.get(0).get("project")));
                concern = true;
            } else if (other != null && !other.isEmpty() && (here == null || here < req)) {
                notes.add("also in stock at " + projectQtys(other, unit) + ".");   // usually in use there
            } else if (here == null) {
                notes.add(onOrder == null ? "not in stock at any project, so it needs to be bought." : "not in stock at any project.");
            }

            if (onOrder != null && !onOrder.isEmpty()) {
                List<String> refs = new ArrayList<>();
                for (Map<String, Object> o : onOrder) {
                    refs.add(qty(d(o.get("pending")), unit) + " on " + o.get("po") + " (raised " + o.get("date")
                            + (d(o.get("received")) > 0 ? ", partly received)" : ", nothing received yet)"));
                    openPos.add(str(o.get("po")));
                }
                notes.add(onOrder.size() == 1
                        ? refs.get(0).replaceFirst(" on ", " still to arrive on ") + "."
                        : "still to arrive on open POs: " + firstFew(refs, "POs") + ".");
                concern = true;
            }

            Map<String, Object> boq = (Map<String, Object>) it.get("boq");
            if (boq != null && d(boq.get("remaining")) < 0) {
                double planned = d(boq.get("planned"));
                double over = -d(boq.get("remaining"));
                notes.add(planned > 0
                        ? "takes the project " + num(over / planned * 100, 0) + "% over its BOQ (" + qty(planned, unit)
                          + " planned, " + qty(d(boq.get("alreadyIndented")), unit) + " indented including this one)."
                        : "not planned in the BOQ (" + qty(d(boq.get("alreadyIndented")), unit) + " indented so far).");
                overBoq.add(name);
                concern = true;
            }

            List<Map<String, Object>> similar = (List<Map<String, Object>>) it.get("recentSimilarIndents");
            if (similar != null && !similar.isEmpty()) {
                List<String> refs = new ArrayList<>();
                for (Map<String, Object> sim : similar) {
                    refs.add(sim.get("indent") + " on " + sim.get("date") + " (" + qty(d(sim.get("qty")), unit) + ")");
                    dupIndents.add(str(sim.get("indent")));
                }
                notes.add(pick(seed, 2,
                        (similar.size() == 1 ? "a similar indent was" : "similar indents were") + " raised for this project recently: ",
                        "this project already indented it in the last 30 days: ")
                        + firstFew(refs, "indents") + ".");
                concern = true;
            }

            String label = name + " (" + qty(req, unit) + ")";
            if (concern) concernLines.add("- **" + label + "** — " + capitalizeSentences(notes));
            else fineLines.add("- " + label + " — " + capitalizeSentences(notes));
        }

        List<String> blocks = new ArrayList<>();
        blocks.add("**" + headline(seed, items.size(), concernLines.size()) + "**");
        if (!concernLines.isEmpty()) blocks.add(String.join("\n", concernLines));
        if (!fineLines.isEmpty()) {
            blocks.add((concernLines.isEmpty() ? "" : (fineLines.size() == 1 ? "Looks fine:" : "These look fine:") + "\n")
                    + String.join("\n", fineLines));
        }

        List<String> actions = new ArrayList<>();
        if (!dupIndents.isEmpty()) {
            actions.add("check with the site that this isn't a repeat of indent" + (dupIndents.size() == 1 ? " " : "s ")
                    + firstFew(new ArrayList<>(dupIndents), "indents"));
        }
        if (!openPos.isEmpty()) {
            actions.add("check whether " + firstFew(new ArrayList<>(openPos), "POs")
                    + (openPos.size() == 1 ? " already covers" : " already cover") + " this need");
        }
        if (!covered.isEmpty()) {
            actions.add("confirm the need for " + firstFew(covered, "items") + " with the site — stock here is more than requested");
        }
        if (!coveredFresh.isEmpty()) {
            actions.add("check with the site whether the stock of " + firstFew(coveredFresh, "items")
                    + " here is already set aside for other work");
        }
        if (!transfers.isEmpty()) actions.add("consider transferring " + firstFew(transfers, "items") + " before buying");
        if (!overBoq.isEmpty()) actions.add("get a reason for the BOQ overrun on " + firstFew(overBoq, "items"));
        if (actions.size() > 3) actions = actions.subList(0, 3);   // ponytail: top 3 keeps it readable; the rest are in the list above

        if (actions.isEmpty()) {
            blocks.add("**Suggestion:** " + pick(seed, 3, "looks fine to approve.", "good to approve."));
        } else if (actions.size() == 1) {
            blocks.add("**Suggestion:** " + actions.get(0) + ".");
        } else {
            StringBuilder sb = new StringBuilder("**Suggestions:**");
            for (int i = 0; i < actions.size(); i++) {
                sb.append("\n").append(i + 1).append(". ").append(Character.toUpperCase(actions.get(i).charAt(0)))
                        .append(actions.get(i).substring(1)).append('.');
            }
            blocks.add(sb.toString());
        }
        return String.join("\n\n", blocks);
    }

    private static String headline(String seed, int n, int f) {
        if (f == 0) {
            String what = n == 1 ? pick(seed, 4, "This item looks fine", "Nothing stands out for this item")
                    : n == 2 ? pick(seed, 4, "Both items look fine", "Nothing stands out for either item")
                    : pick(seed, 4, "All " + n + " items look fine", "Nothing stands out across the " + n + " items");
            return what + " — stock, BOQ and recent indents checked.";
        }
        if (n == 1) return "This item needs a look before approving.";
        if (f == n) return (n == 2 ? "Both items" : "All " + n + " items") + " need a look before approving.";
        return f + " of " + n + " items " + (f == 1 ? "needs" : "need") + " a look before approving.";
    }

    /** Biggest holders first, top 3: "Green Valley (150 Bags), Lake View (40 Bags) and 2 more projects". */
    private static String projectQtys(List<Map<String, Object>> rows, String unit) {
        List<Map<String, Object>> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> Double.compare(d(b.get("qty")), d(a.get("qty"))));
        List<String> out = new ArrayList<>();
        for (Map<String, Object> r : sorted) out.add(r.get("project") + " (" + qty(d(r.get("qty")), unit) + ")");
        return firstFew(out, "projects");
    }

    // ── PO: shown on PO details ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    static String po(Map<String, Object> data) {
        String seed = String.valueOf(data.get("poNumber"));
        String status = str(data.get("status")).toUpperCase();
        List<Map<String, Object>> lines = (List<Map<String, Object>>) data.get("lines");
        List<Map<String, Object>> history = (List<Map<String, Object>>) data.get("statusHistory");
        boolean shortClosed = "SHORT CLOSED".equals(status), cancelled = "CANCELLED".equals(status);

        List<String> bullets = new ArrayList<>();
        List<String> done = new ArrayList<>();
        int received = 0, untouched = 0;
        for (Map<String, Object> l : lines) {
            String name = str(l.get("item")), unit = str(l.get("unit"));
            double ordered = d(l.get("ordered")), got = d(l.get("received"));
            if (got >= ordered) {
                received++;
                if (got <= ordered) done.add(name);
                else bullets.add("- **" + name + "** — " + qty(got, unit) + " received against " + qty(ordered, unit) + " ordered (" + qty(got - ordered, unit) + " extra).");
                continue;
            }
            if (got == 0) untouched++;
            String pending = shortClosed ? " not supplied (closed short)" : cancelled ? " not supplied (cancelled)" : " pending";
            bullets.add("- **" + name + "** — " + (got == 0
                    ? qty(ordered, unit) + pending + "."
                    : num(got, 2) + " of " + qty(ordered, unit) + " received, **" + qty(ordered - got, unit) + pending + "**."));
        }

        int n = lines.size();
        String poDate = str(data.get("poDate"));
        String head;
        if (cancelled) {
            head = "This PO was cancelled" + lastChange(history, "CANCELLED") + ".";
        } else if (shortClosed) {
            head = "This PO was short-closed" + lastChange(history, "SHORT CLOSED")
                    + (data.get("shortCloseReason") == null ? "" : " — " + str(data.get("shortCloseReason")).replaceAll("[.\\s]+$", "")) + ".";
        } else if (n > 0 && received == n) {
            head = n == 1 ? "The item has been received in full." : pick(seed, 1, "All " + n + " items have been received in full.", "Everything on this PO has been received.");
        } else if (untouched == n) {
            head = pick(seed, 2, "Nothing has been received yet", "No material received so far") + (poDate.isEmpty() ? "." : " — pending since " + poDate + ".");
        } else if (received == 0) {
            head = n == 1 ? "The item has been partly received." : n == 2 ? "Neither item is fully received yet." : "None of the " + n + " items is fully received yet.";
        } else {
            head = received + " of " + n + " items fully received; " + (n - received) + " still pending.";
        }

        List<String> blocks = new ArrayList<>();
        blocks.add("**" + head + "**");
        if (!bullets.isEmpty()) blocks.add(String.join("\n", bullets));
        if (!done.isEmpty() && received < n) blocks.add("Fully received: " + joinAnd(done) + ".");

        StringBuilder value = new StringBuilder(data.get("grandTotalRupees") == null
                ? "PO"   // price-restricted viewer: no money
                : "PO value **" + money(data.get("grandTotalRupees")) + "**");
        if (data.get("supplier") != null) value.append(" with ").append(data.get("supplier"));
        if (data.get("project") != null) value.append(" for ").append(data.get("project"));
        if (!poDate.isEmpty()) value.append(", raised on ").append(poDate);
        blocks.add(value.append('.').toString());

        // Last real status change; history notes are mostly system text, so they're left out.
        Map<String, Object> last = null;
        for (Map<String, Object> h : history == null ? Collections.<Map<String, Object>>emptyList() : history) {
            if (!str(h.get("from")).equalsIgnoreCase(str(h.get("to")))) last = h;
        }
        if (last != null && last.get("from") != null) {
            blocks.add("Last status change " + last.get("date") + by(last) + ": " + words(last.get("from")) + " → " + words(last.get("to")) + ".");
        }
        return String.join("\n\n", blocks);
    }

    private static String lastChange(List<Map<String, Object>> history, String toStatus) {
        if (history == null) return "";
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> h = history.get(i);
            if (toStatus.equalsIgnoreCase(str(h.get("to")))) {
                return " on " + h.get("date") + by(h);
            }
        }
        return "";
    }

    // ── Global Dashboard "Needs attention" ────────────────────────────────────

    /** One banner line: "**2 indents are waiting for approval** — oldest IN-1540 (Mangalam City, 1 day)." */
    static String attention(Map<String, Object> a) {
        int n = ((Number) a.get("count")).intValue();
        boolean one = n == 1;
        String head;
        String tail = "";
        switch (str(a.get("kind"))) {
            case "INDENT_APPROVAL":
                head = one ? "1 indent is waiting for approval" : n + " indents are waiting for approval";
                break;
            case "INDENT_NO_PO":
                head = one ? "1 approved indent has no PO after 7+ days" : n + " approved indents have no PO after 7+ days";
                break;
            case "PO_NOTHING_RECEIVED":
                head = one ? "1 PO has nothing received after 30+ days" : n + " POs have nothing received after 30+ days";
                break;
            default: // PO_NO_PROGRESS
                head = one ? "1 partly received PO has had no progress for 60+ days" : n + " partly received POs have had no progress for 60+ days";
                tail = " Consider following up or short-closing.";
        }
        List<String> about = new ArrayList<>();
        if (a.get("oldestSupplier") != null) about.add(str(a.get("oldestSupplier")));
        if (a.get("oldestProject") != null) about.add(str(a.get("oldestProject")));
        about.add(days(((Number) a.get("oldestDays")).intValue()));
        return "**" + head + "** — " + (one ? "" : "oldest ") + a.get("oldestId") + " (" + String.join(", ", about) + ")." + tail;
    }

    // ── wording helpers ───────────────────────────────────────────────────────

    /** "1 day", "126 days". */
    private static String days(int n) {
        return n <= 1 ? "1 day" : n + " days";
    }

    /** " by ravi"; nothing for automatic changes. */
    private static String by(Map<String, Object> h) {
        String who = str(h.get("by"));
        return who.isEmpty() || who.equalsIgnoreCase("System") ? "" : " by " + who;
    }

    /** Stable per record: the same indent/PO always gets the same wording. */
    private static String pick(String seed, int salt, String... options) {
        return options[Math.floorMod(seed.hashCode() * 31 + salt, options.length)];
    }

    /** At most 3 named, rest counted: "A, B, C and 4 more items". */
    static String firstFew(List<String> parts, String noun) {
        if (parts.size() <= 3) return joinAnd(parts);
        return String.join(", ", parts.subList(0, 3)) + " and " + (parts.size() - 3) + " more " + noun;
    }

    /** "A", "A and B", "A, B and C". */
    static String joinAnd(List<String> parts) {
        if (parts.size() <= 1) return parts.isEmpty() ? "" : parts.get(0);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }

    /** Notes are written as fragments; join them as sentences with a capital first letter. */
    private static String capitalizeSentences(List<String> notes) {
        StringBuilder sb = new StringBuilder();
        for (String s : notes) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        }
        return sb.toString();
    }

    private static String qty(double v, String unit) {
        return num(v, 2) + (unit == null || unit.isEmpty() ? "" : " " + unit);
    }

    static String money(Object v) {
        return "₹" + num(d(v), 2);
    }

    /** Indian digit grouping (12,34,567), at most maxDecimals, trailing zeros dropped. */
    static String num(double v, int maxDecimals) {
        String plain = BigDecimal.valueOf(v).setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        String sign = plain.startsWith("-") ? "-" : "";
        if (!sign.isEmpty()) plain = plain.substring(1);
        int dot = plain.indexOf('.');
        String whole = dot < 0 ? plain : plain.substring(0, dot);
        String frac = dot < 0 ? "" : plain.substring(dot);
        if (whole.length() > 3) {
            String head = whole.substring(0, whole.length() - 3);
            StringBuilder g = new StringBuilder();
            for (int i = head.length(); i > 0; i -= 2) g.insert(0, head.substring(Math.max(0, i - 2), i) + (g.length() > 0 ? "," : ""));
            whole = g + "," + whole.substring(whole.length() - 3);
        }
        return sign + whole + frac;
    }

    /** "SHORT CLOSED" → "Short closed". */
    private static String words(Object status) {
        String s = str(status).toLowerCase();
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static double d(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
