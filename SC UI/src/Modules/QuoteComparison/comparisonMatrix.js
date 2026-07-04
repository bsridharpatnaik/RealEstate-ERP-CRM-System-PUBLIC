import React, { useState, useMemo } from "react";
import Chip from "@material-ui/core/Chip";
import Tooltip from "@material-ui/core/Tooltip";
import Checkbox from "@material-ui/core/Checkbox";
import Button from "@material-ui/core/Button";
import RadioButtonCheckedIcon from "@material-ui/icons/RadioButtonChecked";
import RadioButtonUncheckedIcon from "@material-ui/icons/RadioButtonUnchecked";
import EmojiEventsIcon from "@material-ui/icons/EmojiEvents";

// Above this many vendors, the side-by-side table gets unwieldy — default the detailed
// table to the top N (by rank) and let the user pick others in via the checkboxes on the cards.
const AUTO_NARROW_THRESHOLD = 5;
const AUTO_NARROW_TO = 3;

const LINE_STATUS_COLORS = {
  OPEN:      { bg: "#e3f2fd", color: "#1565c0" },
  FINALIZED: { bg: "#e8f5e9", color: "#2e7d32" },
  PO_LINKED: { bg: "#ede7f6", color: "#4527a0" },
  CLOSED:    { bg: "#f5f5f5", color: "#666" },
};

const inr = (v) => `₹${Number(v || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;

function ComparisonMatrix({ matrix, criteria, supplierSummary, onSelectWinner, canFinalize }) {
  const hasData = !!matrix && matrix.length > 0;

  // Header-scoped criteria (one value per vendor) are shown once in the summary cards above.
  // Line-scoped criteria (can vary per product) are shown per vendor in the detail views.
  const lineCriteria = (criteria || []).filter(c => c.criteriaScope !== "HEADER");

  // Collect all suppliers across all rows, in supplierSummary order when available (rank order).
  const supplierMap = {};
  const summaryById = {};
  (supplierSummary || []).forEach(s => { summaryById[String(s.supplierQuoteId)] = s; });

  (matrix || []).forEach(row => {
    (row.supplierResponses || []).forEach(sr => {
      const id = String(sr.supplierQuoteId);
      if (!supplierMap[id]) {
        const summary = summaryById[id];
        supplierMap[id] = summary?.label || (sr.supplierName + (sr.revisionLabel ? ` (${sr.revisionLabel})` : ""));
      }
    });
  });
  let supplierIds = Object.keys(supplierMap);
  supplierIds = supplierIds.sort((a, b) => {
    const ra = summaryById[a]?.rank ?? Infinity;
    const rb = summaryById[b]?.rank ?? Infinity;
    return ra - rb;
  });

  const lowestRank1Id = supplierIds.find(id => summaryById[id]?.rank === 1);

  // Hooks must run on every render regardless of the hasData early-return below.
  const [view, setView] = useState("cards"); // "cards" (readable, default) | "table" (dense grid)
  const [visibleIds, setVisibleIds] = useState(() =>
    supplierIds.length > AUTO_NARROW_THRESHOLD ? supplierIds.slice(0, AUTO_NARROW_TO) : supplierIds
  );
  const effectiveVisibleIds = useMemo(
    () => visibleIds.filter(id => supplierIds.includes(id)),
    [visibleIds, supplierIds]
  );

  const toggleVisible = (sqId) => {
    setVisibleIds(prev => prev.includes(sqId) ? prev.filter(id => id !== sqId) : [...prev, sqId]);
  };

  if (!hasData) {
    return <div style={{ padding: 32, textAlign: "center", color: "#aaa" }}>No data. Add supplier quotes first.</div>;
  }

  return (
    <div>
      {/* View toggle */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 12 }}>
        <div style={{ display: "inline-flex", border: "1px solid #c5cae9", borderRadius: 8, overflow: "hidden" }}>
          {["cards", "table"].map(v => (
            <button key={v} onClick={() => setView(v)}
              style={{
                border: "none", cursor: "pointer", padding: "6px 18px", fontSize: 13, fontWeight: 600,
                background: view === v ? "#3949ab" : "#fff", color: view === v ? "#fff" : "#3949ab",
              }}>
              {v === "cards" ? "Cards" : "Table"}
            </button>
          ))}
        </div>
        <span style={{ fontSize: 12, color: "#999" }}>
          {view === "cards" ? "One product per card — vendors listed cheapest-first" : "Side-by-side grid — scroll right for more vendors"}
        </span>
      </div>

      {/* Vendor summary cards (shared by both views) */}
      {supplierSummary && supplierSummary.length > 0 && (
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 16 }}>
          {supplierIds.map(sqId => {
            const s = summaryById[sqId];
            if (!s) return null;
            const isBest = s.rank === 1;
            const isVisible = effectiveVisibleIds.includes(sqId);
            const headerCv = (s.headerCriteriaValues || []).filter(v => v.value);
            const showCheckbox = view === "table" && supplierIds.length > AUTO_NARROW_THRESHOLD;
            return (
              <div key={sqId} style={{
                minWidth: 220, flex: "1 1 220px", border: isBest ? "2px solid #2e7d32" : "1px solid #e0e0e0",
                borderRadius: 8, padding: 12, background: isBest ? "#f1f8e9" : "#fff",
                opacity: view === "table" && !isVisible ? 0.55 : 1,
              }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
                  <span style={{ display: "flex", alignItems: "center", gap: 4 }}>
                    {showCheckbox && (
                      <Checkbox size="small" checked={isVisible} onChange={() => toggleVisible(sqId)}
                        style={{ padding: 2 }} title="Show in detailed table" />
                    )}
                    <span style={{ fontWeight: 700, fontSize: 14 }}>{s.label}</span>
                  </span>
                  {s.rank != null && (
                    <Chip label={isBest ? "🏆 L-1" : `L-${s.rank}`} size="small"
                      style={{ background: isBest ? "#2e7d32" : "#eee", color: isBest ? "#fff" : "#555", fontWeight: 600, fontSize: 11 }} />
                  )}
                </div>
                <div style={{ fontSize: 20, fontWeight: 700, color: isBest ? "#2e7d32" : "#333" }}>
                  {inr(s.totalLandedCost)}
                </div>
                <div style={{ fontSize: 12, color: "#888", marginBottom: 6 }}>
                  Total across {s.respondedLines} quoted line{s.respondedLines === 1 ? "" : "s"}
                </div>
                {(s.paymentTerms || s.deliveryLeadDays) && (
                  <div style={{ fontSize: 12, color: "#555", marginBottom: 4 }}>
                    {s.paymentTerms && <div>Payment: {s.paymentTerms}</div>}
                    {s.deliveryLeadDays && <div>Lead time: {s.deliveryLeadDays}d</div>}
                  </div>
                )}
                {headerCv.length > 0 && (
                  <div style={{ display: "flex", flexWrap: "wrap", gap: 4, marginTop: 6 }}>
                    {headerCv.map(v => (
                      <Tooltip key={v.criteriaId} title={v.criteriaName}>
                        <Chip label={v.value} size="small" variant="outlined" style={{ fontSize: 11 }} />
                      </Tooltip>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {view === "cards"
        ? renderCards({ matrix, lineCriteria, supplierMap, summaryById, onSelectWinner, canFinalize })
        : renderTable({
            matrix, lineCriteria, supplierMap, summaryById, supplierIds, effectiveVisibleIds,
            setVisibleIds, lowestRank1Id, onSelectWinner, canFinalize,
          })}
    </div>
  );
}

// ─── Cards view (default, readable — one card per product line) ────────────────
function renderCards({ matrix, lineCriteria, supplierMap, summaryById, onSelectWinner, canFinalize }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      {matrix.map(row => {
        const statusStyle = LINE_STATUS_COLORS[row.lineStatus] || LINE_STATUS_COLORS.OPEN;

        // Vendors that actually responded, cheapest (landed) first; non-responders after.
        const responded = (row.supplierResponses || []).filter(sr => sr.hasResponse);
        responded.sort((a, b) => {
          const la = a.landedCost != null ? Number(a.landedCost) : Infinity;
          const lb = b.landedCost != null ? Number(b.landedCost) : Infinity;
          return la - lb;
        });
        const nonResponders = (row.supplierResponses || []).filter(sr => !sr.hasResponse);
        const isOpen = row.lineStatus === "OPEN";

        return (
          <div key={row.lineId} style={{ border: "1px solid #e0e0e0", borderRadius: 10, overflow: "hidden" }}>
            {/* Product header */}
            <div style={{
              display: "flex", alignItems: "center", justifyContent: "space-between",
              padding: "12px 16px", background: "#f7f9fc", borderBottom: "1px solid #eceff1",
            }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 15 }}>{row.productName}</div>
                <div style={{ fontSize: 13, color: "#607d8b", marginTop: 2 }}>
                  {row.requiredQty} {row.unit}
                  {row.specifications ? <span> · {row.specifications}</span> : null}
                </div>
              </div>
              <Chip label={row.lineStatus?.replace(/_/g, " ")} size="small"
                style={{ background: statusStyle.bg, color: statusStyle.color, fontWeight: 600, fontSize: 11 }} />
            </div>

            {/* Vendor rows */}
            <div>
              {responded.length === 0 && (
                <div style={{ padding: "14px 16px", color: "#aaa", fontSize: 13 }}>No vendor has quoted this line yet.</div>
              )}
              {responded.map((resp, i) => {
                const id = String(resp.supplierQuoteId);
                const isWinner = row.finalizedSupplierQuoteLineId === resp.supplierQuoteLineId;
                const isLowest = resp.isLowest || i === 0;
                const cvs = (resp.criteriaValues || []).filter(v => v.value);
                return (
                  <div key={id} style={{
                    display: "flex", alignItems: "center", gap: 16, padding: "10px 16px",
                    borderBottom: "1px solid #f4f4f4",
                    background: isWinner ? "#e8f5e9" : isLowest ? "#f9fdf9" : "#fff",
                  }}>
                    {/* Vendor name + badges */}
                    <div style={{ width: 240, flexShrink: 0 }}>
                      <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
                        {isLowest && <Tooltip title="Lowest landed cost"><EmojiEventsIcon style={{ fontSize: 16, color: "#f9a825" }} /></Tooltip>}
                        <span style={{ fontWeight: 700, fontSize: 14 }}>{supplierMap[id]}</span>
                      </span>
                      {isWinner && (
                        <span style={{ fontSize: 11, color: "#2e7d32", fontWeight: 700 }}>✓ AWARDED</span>
                      )}
                    </div>

                    {/* Numbers — readable, generously spaced */}
                    <Metric label="Rate" value={resp.quotedRate != null ? `₹${resp.quotedRate}` : "—"} strong />
                    <Metric label="Landed" value={resp.landedCost != null ? inr(resp.landedCost) : "—"}
                      color={isLowest ? "#2e7d32" : "#333"} strong />
                    <Metric label="Delivery" value={getLeadDays(resp)} />
                    {lineCriteria.length > 0 && (
                      <div style={{ display: "flex", flexWrap: "wrap", gap: 6, flex: 1 }}>
                        {cvs.length === 0
                          ? <span style={{ fontSize: 12, color: "#ccc" }}>—</span>
                          : cvs.map(cv => (
                            <span key={cv.criteriaId} style={{ fontSize: 12, color: "#555", background: "#f1f3f8", borderRadius: 4, padding: "2px 8px" }}>
                              <span style={{ color: "#90a4ae" }}>{cv.criteriaName}: </span>{cv.value}
                            </span>
                          ))}
                      </div>
                    )}

                    {/* Award action */}
                    <div style={{ marginLeft: "auto", flexShrink: 0 }}>
                      {canFinalize && isOpen && resp.supplierQuoteLineId ? (
                        <Button size="small" variant={isLowest ? "contained" : "outlined"}
                          color="primary"
                          onClick={() => onSelectWinner && onSelectWinner(row.lineId, resp.supplierQuoteLineId, resp.quotedRate, row)}>
                          Award
                        </Button>
                      ) : isWinner ? (
                        <RadioButtonCheckedIcon fontSize="small" style={{ color: "#2e7d32" }} />
                      ) : null}
                    </div>
                  </div>
                );
              })}

              {/* Non-responders shown muted so the buyer sees who was asked but hasn't quoted */}
              {nonResponders.length > 0 && (
                <div style={{ padding: "8px 16px", fontSize: 12, color: "#b0bec5" }}>
                  No response: {nonResponders.map(sr => supplierMap[String(sr.supplierQuoteId)]).join(", ")}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function Metric({ label, value, strong, color }) {
  return (
    <div style={{ minWidth: 90 }}>
      <div style={{ fontSize: 11, color: "#90a4ae", textTransform: "uppercase", letterSpacing: 0.3 }}>{label}</div>
      <div style={{ fontSize: 14, fontWeight: strong ? 700 : 500, color: color || "#333" }}>{value}</div>
    </div>
  );
}

// ─── Table view (dense grid — improved readability) ────────────────────────────
function renderTable({
  matrix, lineCriteria, supplierMap, summaryById, supplierIds, effectiveVisibleIds,
  setVisibleIds, lowestRank1Id, onSelectWinner, canFinalize,
}) {
  return (
    <>
      {supplierIds.length > AUTO_NARROW_THRESHOLD && (
        <div style={{
          display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8,
          fontSize: 12, color: "#666", background: "#fff8e1", border: "1px solid #ffe082",
          borderRadius: 6, padding: "6px 12px",
        }}>
          <span>
            {supplierIds.length} quotes received — showing the {effectiveVisibleIds.length} cheapest.
            Tick a card above to add it in, or untick to drop it out.
          </span>
          <span style={{ display: "flex", gap: 8 }}>
            <Button size="small" onClick={() => setVisibleIds(supplierIds)}>Show all</Button>
            <Button size="small" onClick={() => setVisibleIds(supplierIds.slice(0, AUTO_NARROW_TO))}>Reset</Button>
          </span>
        </div>
      )}
      <div style={{ overflowX: "auto" }}>
        <table style={{ borderCollapse: "collapse", fontSize: 13, minWidth: "100%" }}>
          <thead>
            <tr style={{ background: "#f5f6fa" }}>
              <th style={stickyTh(0)}>Product</th>
              <th style={stickyTh(180)}>Req Qty</th>
              <th style={stickyTh(280)}>Unit</th>
              <th style={stickyTh(340)}>Status</th>
              {effectiveVisibleIds.map(sqId => (
                <th key={sqId} colSpan={3 + lineCriteria.length}
                  style={{ padding: "10px 12px", borderBottom: "2px solid #e0e0e0",
                    borderLeft: "2px solid #bdbdbd", textAlign: "center", fontSize: 13,
                    background: sqId === lowestRank1Id ? "#e8f5e9" : "#e8eaf6",
                    color: sqId === lowestRank1Id ? "#2e7d32" : "#3949ab" }}>
                  {supplierMap[sqId]}
                </th>
              ))}
              {canFinalize && <th style={{ padding: "10px 12px", borderBottom: "2px solid #e0e0e0" }}>Award</th>}
            </tr>
            <tr style={{ background: "#fafafa", fontSize: 12 }}>
              <th style={stickyTh(0)} />
              <th style={stickyTh(180)} />
              <th style={stickyTh(280)} />
              <th style={stickyTh(340)} />
              {effectiveVisibleIds.map(sqId => (
                <React.Fragment key={sqId}>
                  <th style={{ ...subTh, borderLeft: "2px solid #bdbdbd" }}>Rate</th>
                  <th style={subTh}>Landed</th>
                  <th style={subTh}>Delivery</th>
                  {lineCriteria.map(c => <th key={c.id} style={subTh}>{c.criteriaName}</th>)}
                </React.Fragment>
              ))}
              {canFinalize && <th />}
            </tr>
          </thead>
          <tbody>
            {matrix.map(row => {
              const statusStyle = LINE_STATUS_COLORS[row.lineStatus] || LINE_STATUS_COLORS.OPEN;
              return (
                <tr key={row.lineId} style={{ borderBottom: "1px solid #f0f0f0" }}>
                  <td style={stickyTd(0)}>
                    <Tooltip title={row.productName || ""}><strong>{row.productName}</strong></Tooltip>
                  </td>
                  <td style={stickyTd(180)}>{row.requiredQty}</td>
                  <td style={stickyTd(280)}>{row.unit}</td>
                  <td style={stickyTd(340)}>
                    <Chip label={row.lineStatus?.replace(/_/g, " ")} size="small"
                      style={{ background: statusStyle.bg, color: statusStyle.color, fontWeight: 600, fontSize: 11 }} />
                  </td>

                  {effectiveVisibleIds.map(sqId => {
                    const resp = (row.supplierResponses || []).find(sr => String(sr.supplierQuoteId) === sqId);
                    if (!resp || !resp.hasResponse) {
                      return (
                        <React.Fragment key={sqId}>
                          <td style={{ ...td, borderLeft: "2px solid #bdbdbd", color: "#ccc" }}>—</td>
                          <td style={td} />
                          <td style={td} />
                          {lineCriteria.map(c => <td key={c.id} style={td} />)}
                        </React.Fragment>
                      );
                    }
                    const isLowest = resp.isLowest;
                    const isWinner = row.finalizedSupplierQuoteLineId === resp.supplierQuoteLineId;
                    return (
                      <React.Fragment key={sqId}>
                        <td style={{
                          ...td, borderLeft: "2px solid #bdbdbd", fontWeight: 600,
                          color: isLowest ? "#2e7d32" : "#333",
                          background: isLowest ? "#f1f8e9" : isWinner ? "#e8f5e9" : "transparent",
                        }}>
                          {isLowest && <span title="Lowest" style={{ color: "#2e7d32", marginRight: 2 }}>★</span>}
                          {resp.quotedRate != null ? `₹${resp.quotedRate}` : "—"}
                        </td>
                        <td style={{ ...td, color: "#555" }}>
                          {resp.landedCost != null ? inr(resp.landedCost) : "—"}
                        </td>
                        <td style={td}>{getLeadDays(resp)}</td>
                        {lineCriteria.map(c => {
                          const cv = (resp.criteriaValues || []).find(v => v.criteriaId === c.id);
                          return <td key={c.id} style={td}>{cv?.value || "—"}</td>;
                        })}
                      </React.Fragment>
                    );
                  })}

                  {canFinalize && (
                    <td style={{ padding: "6px 8px", textAlign: "center" }}>
                      {row.lineStatus === "OPEN" && (
                        <div style={{ display: "flex", gap: 4, justifyContent: "center" }}>
                          {effectiveVisibleIds.map(sqId => {
                            const resp = (row.supplierResponses || []).find(sr => String(sr.supplierQuoteId) === sqId);
                            if (!resp || !resp.hasResponse || !resp.supplierQuoteLineId) return null;
                            return (
                              <Tooltip key={sqId} title={`Award ${supplierMap[sqId]}`}>
                                <span style={{ cursor: "pointer", color: "#1565c0" }}
                                  onClick={() => onSelectWinner && onSelectWinner(row.lineId, resp.supplierQuoteLineId, resp.quotedRate, row)}>
                                  <RadioButtonUncheckedIcon fontSize="small" />
                                </span>
                              </Tooltip>
                            );
                          })}
                        </div>
                      )}
                      {row.lineStatus === "FINALIZED" && (
                        <RadioButtonCheckedIcon fontSize="small" style={{ color: "#2e7d32" }} />
                      )}
                    </td>
                  )}
                </tr>
              );
            })}

            {supplierSummaryTotalRow({ matrix, summaryById, effectiveVisibleIds, lineCriteria, lowestRank1Id, canFinalize })}
          </tbody>
        </table>
      </div>
    </>
  );
}

function supplierSummaryTotalRow({ summaryById, effectiveVisibleIds, lineCriteria, lowestRank1Id, canFinalize }) {
  const anySummary = effectiveVisibleIds.some(id => summaryById[id]);
  if (!anySummary) return null;
  return (
    <tr style={{ borderTop: "2px solid #bdbdbd", background: "#fafafa" }}>
      <td style={{ ...stickyTd(0), fontWeight: 700, background: "#fafafa" }}>TOTAL</td>
      <td style={{ ...stickyTd(180), background: "#fafafa" }} />
      <td style={{ ...stickyTd(280), background: "#fafafa" }} />
      <td style={{ ...stickyTd(340), background: "#fafafa" }} />
      {effectiveVisibleIds.map(sqId => {
        const s = summaryById[sqId];
        return (
          <td key={sqId} colSpan={3 + lineCriteria.length}
            style={{
              padding: "10px 12px", borderLeft: "2px solid #bdbdbd", fontWeight: 700,
              color: sqId === lowestRank1Id ? "#2e7d32" : "#333",
            }}>
            {s ? inr(s.totalLandedCost) : "—"}
            {s?.rank != null && <span style={{ fontWeight: 500, color: "#888", marginLeft: 6 }}>(L-{s.rank})</span>}
          </td>
        );
      })}
      {canFinalize && <td style={{ background: "#fafafa" }} />}
    </tr>
  );
}

// Helpers — sticky-column widths must match the `left` offsets passed in.
const STICKY_WIDTHS = { 0: 180, 180: 100, 280: 60, 340: 110 };
function stickyTh(left) {
  return {
    position: "sticky", left, width: STICKY_WIDTHS[left], minWidth: STICKY_WIDTHS[left], maxWidth: STICKY_WIDTHS[left],
    background: "#f5f6fa", zIndex: 2,
    padding: "10px 12px", borderBottom: "2px solid #e0e0e0",
    borderRight: "1px solid #e0e0e0", textAlign: "left", fontWeight: 600,
    whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis",
  };
}
function stickyTd(left) {
  return {
    position: "sticky", left, width: STICKY_WIDTHS[left], minWidth: STICKY_WIDTHS[left], maxWidth: STICKY_WIDTHS[left],
    background: "#fff", zIndex: 1,
    padding: "10px 12px", borderRight: "1px solid #f0f0f0", whiteSpace: "nowrap",
    overflow: "hidden", textOverflow: "ellipsis",
  };
}
const td = { padding: "10px 12px", whiteSpace: "nowrap" };
const subTh = { padding: "8px 12px", borderBottom: "2px solid #e0e0e0", fontWeight: 500, color: "#555" };

function getLeadDays(resp) {
  if (resp.expectedDeliveryDate) {
    const d = new Date(resp.expectedDeliveryDate);
    return d.toLocaleDateString("en-IN");
  }
  return "—";
}

export default ComparisonMatrix;
