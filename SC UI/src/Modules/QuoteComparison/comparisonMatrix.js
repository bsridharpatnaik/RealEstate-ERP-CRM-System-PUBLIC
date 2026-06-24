import React, { useState, useMemo } from "react";
import Chip from "@material-ui/core/Chip";
import Tooltip from "@material-ui/core/Tooltip";
import Checkbox from "@material-ui/core/Checkbox";
import Button from "@material-ui/core/Button";
import RadioButtonCheckedIcon from "@material-ui/icons/RadioButtonChecked";
import RadioButtonUncheckedIcon from "@material-ui/icons/RadioButtonUnchecked";

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

function ComparisonMatrix({ matrix, criteria, supplierSummary, onSelectWinner, canFinalize }) {
  const hasData = !!matrix && matrix.length > 0;

  // Header-scoped criteria (one value per vendor) are shown once in the summary cards above the table.
  // Line-scoped criteria (can vary per product) are shown as extra columns under each vendor in the table.
  const lineCriteria = (criteria || []).filter(c => c.criteriaScope !== "HEADER");

  // Collect all suppliers across all rows, in supplierSummary order when available (keeps rank order stable).
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
  // Cheapest first, unranked (no response yet) last — makes scanning easy once there are many vendors.
  let supplierIds = Object.keys(supplierMap);
  supplierIds = supplierIds.sort((a, b) => {
    const ra = summaryById[a]?.rank ?? Infinity;
    const rb = summaryById[b]?.rank ?? Infinity;
    return ra - rb;
  });

  const lowestRank1Id = supplierIds.find(id => summaryById[id]?.rank === 1);

  // Hooks must run on every render regardless of the hasData early-return below.
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
      {supplierSummary && supplierSummary.length > 0 && (
        <>
          {supplierIds.length > AUTO_NARROW_THRESHOLD && (
            <div style={{
              display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8,
              fontSize: 12, color: "#666", background: "#fff8e1", border: "1px solid #ffe082",
              borderRadius: 6, padding: "6px 12px",
            }}>
              <span>
                {supplierIds.length} quotes received — showing the {effectiveVisibleIds.length} cheapest in the table below.
                Tick a card to add it in, or untick to drop it out.
              </span>
              <span style={{ display: "flex", gap: 8 }}>
                <Button size="small" onClick={() => setVisibleIds(supplierIds)}>Show all</Button>
                <Button size="small" onClick={() => setVisibleIds(supplierIds.slice(0, AUTO_NARROW_TO))}>Reset</Button>
              </span>
            </div>
          )}
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 16 }}>
            {supplierIds.map(sqId => {
              const s = summaryById[sqId];
              if (!s) return null;
              const isBest = s.rank === 1;
              const isVisible = effectiveVisibleIds.includes(sqId);
              const headerCv = (s.headerCriteriaValues || []).filter(v => v.value);
              return (
                <div key={sqId} style={{
                  minWidth: 220, flex: "1 1 220px", border: isBest ? "2px solid #2e7d32" : "1px solid #e0e0e0",
                  borderRadius: 8, padding: 12, background: isBest ? "#f1f8e9" : "#fff",
                  opacity: isVisible ? 1 : 0.55,
                }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
                    <span style={{ display: "flex", alignItems: "center", gap: 4 }}>
                      {supplierIds.length > AUTO_NARROW_THRESHOLD && (
                        <Checkbox size="small" checked={isVisible} onChange={() => toggleVisible(sqId)}
                          style={{ padding: 2 }} title="Show in detailed table" />
                      )}
                      <span style={{ fontWeight: 700, fontSize: 13 }}>{s.label}</span>
                    </span>
                    {s.rank != null && (
                      <Chip label={isBest ? "🏆 Lowest — L-1" : `L-${s.rank}`} size="small"
                        style={{ background: isBest ? "#2e7d32" : "#eee", color: isBest ? "#fff" : "#555", fontWeight: 600, fontSize: 10 }} />
                    )}
                  </div>
                  <div style={{ fontSize: 18, fontWeight: 700, color: isBest ? "#2e7d32" : "#333" }}>
                    ₹{Number(s.totalLandedCost || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}
                  </div>
                  <div style={{ fontSize: 11, color: "#888", marginBottom: 6 }}>
                    Total across {s.respondedLines} quoted line{s.respondedLines === 1 ? "" : "s"}
                  </div>
                  {(s.paymentTerms || s.deliveryLeadDays) && (
                    <div style={{ fontSize: 11, color: "#555", marginBottom: 4 }}>
                      {s.paymentTerms && <div>Payment: {s.paymentTerms}</div>}
                      {s.deliveryLeadDays && <div>Lead time: {s.deliveryLeadDays}d</div>}
                    </div>
                  )}
                  {headerCv.length > 0 && (
                    <div style={{ display: "flex", flexWrap: "wrap", gap: 4, marginTop: 6 }}>
                      {headerCv.map(v => (
                        <Tooltip key={v.criteriaId} title={v.criteriaName}>
                          <Chip label={v.value} size="small" variant="outlined" style={{ fontSize: 10 }} />
                        </Tooltip>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </>
      )}

      <div style={{ overflowX: "auto" }}>
        <table style={{ borderCollapse: "collapse", fontSize: 12, minWidth: "100%" }}>
          <thead>
            <tr style={{ background: "#f5f6fa" }}>
              {/* Sticky left columns */}
              <th style={stickyTh(0)}>Product</th>
              <th style={stickyTh(160)}>Required Qty</th>
              <th style={stickyTh(260)}>Unit</th>
              <th style={stickyTh(310)}>Spec</th>
              <th style={stickyTh(410)}>Status</th>
              {/* Per supplier blocks */}
              {effectiveVisibleIds.map(sqId => (
                <th key={sqId} colSpan={3 + lineCriteria.length}
                  style={{ padding: "8px 12px", borderBottom: "2px solid #e0e0e0",
                    borderLeft: "2px solid #bdbdbd", textAlign: "center",
                    background: sqId === lowestRank1Id ? "#e8f5e9" : "#e8eaf6",
                    color: sqId === lowestRank1Id ? "#2e7d32" : "#3949ab" }}>
                  {supplierMap[sqId]}
                </th>
              ))}
              {canFinalize && <th style={{ padding: "8px 12px", borderBottom: "2px solid #e0e0e0" }}>Award</th>}
            </tr>
            <tr style={{ background: "#fafafa", fontSize: 11 }}>
              <th style={stickyTh(0)} />
              <th style={stickyTh(160)} />
              <th style={stickyTh(260)} />
              <th style={stickyTh(310)} />
              <th style={stickyTh(410)} />
              {effectiveVisibleIds.map(sqId => (
                <React.Fragment key={sqId}>
                  <th style={{ ...subTh, borderLeft: "2px solid #bdbdbd" }}>Rate</th>
                  <th style={subTh}>Landed</th>
                  <th style={subTh}>Lead (d)</th>
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
                  {/* Fixed left */}
                  <td style={stickyTd(0)}>
                    <Tooltip title={row.productName || ""}>
                      <strong>{row.productName}</strong>
                    </Tooltip>
                  </td>
                  <td style={stickyTd(160)}>{row.requiredQty}</td>
                  <td style={stickyTd(260)}>{row.unit}</td>
                  <td style={{ ...stickyTd(310), fontSize: 11, color: "#666" }}>
                    <Tooltip title={row.specifications || ""}>
                      <span>{truncate(row.specifications, 40)}</span>
                    </Tooltip>
                  </td>
                  <td style={stickyTd(410)}>
                    <Chip label={row.lineStatus?.replace(/_/g, " ")} size="small"
                      style={{ background: statusStyle.bg, color: statusStyle.color, fontWeight: 600, fontSize: 10 }} />
                  </td>

                  {/* Per supplier columns */}
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
                          {resp.landedCost != null ? `₹${parseFloat(resp.landedCost).toFixed(0)}` : "—"}
                        </td>
                        <td style={td}>{getLeadDays(resp)}</td>
                        {lineCriteria.map(c => {
                          const cv = (resp.criteriaValues || []).find(v => v.criteriaId === c.id);
                          return <td key={c.id} style={td}>{cv?.value || "—"}</td>;
                        })}
                      </React.Fragment>
                    );
                  })}

                  {/* Award column */}
                  {canFinalize && (
                    <td style={{ padding: "6px 8px", textAlign: "center" }}>
                      {row.lineStatus === "OPEN" && (
                        <div style={{ display: "flex", gap: 4, justifyContent: "center" }}>
                          {effectiveVisibleIds.map(sqId => {
                            const resp = (row.supplierResponses || []).find(sr => String(sr.supplierQuoteId) === sqId);
                            if (!resp || !resp.hasResponse || !resp.supplierQuoteLineId) return null;
                            return (
                              <Tooltip key={sqId} title={`Select ${supplierMap[sqId]}`}>
                                <span
                                  style={{ cursor: "pointer", color: "#1565c0" }}
                                  onClick={() => onSelectWinner && onSelectWinner(row.lineId, resp.supplierQuoteLineId, resp.quotedRate, row)}
                                >
                                  <RadioButtonUncheckedIcon fontSize="small" />
                                </span>
                              </Tooltip>
                            );
                          })}
                        </div>
                      )}
                      {row.lineStatus === "FINALIZED" && (
                        <Tooltip title="Reopen line">
                          <RadioButtonCheckedIcon fontSize="small" style={{ color: "#2e7d32" }} />
                        </Tooltip>
                      )}
                    </td>
                  )}
                </tr>
              );
            })}

            {/* Totals row — sum of landed cost per vendor across all quoted lines */}
            {supplierSummary && supplierSummary.length > 0 && (
              <tr style={{ borderTop: "2px solid #bdbdbd", background: "#fafafa" }}>
                <td style={{ ...stickyTd(0), fontWeight: 700, background: "#fafafa" }}>TOTAL</td>
                <td style={{ ...stickyTd(160), background: "#fafafa" }} />
                <td style={{ ...stickyTd(260), background: "#fafafa" }} />
                <td style={{ ...stickyTd(310), background: "#fafafa" }} />
                <td style={{ ...stickyTd(410), background: "#fafafa" }} />
                {effectiveVisibleIds.map(sqId => {
                  const s = summaryById[sqId];
                  return (
                    <td key={sqId} colSpan={3 + lineCriteria.length}
                      style={{
                        padding: "8px 12px", borderLeft: "2px solid #bdbdbd", fontWeight: 700,
                        color: sqId === lowestRank1Id ? "#2e7d32" : "#333",
                      }}>
                      {s ? `₹${Number(s.totalLandedCost || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}` : "—"}
                      {s?.rank != null && <span style={{ fontWeight: 500, color: "#888", marginLeft: 6 }}>(L-{s.rank})</span>}
                    </td>
                  );
                })}
                {canFinalize && <td style={{ background: "#fafafa" }} />}
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// Helpers
// Sticky-column widths must match the `left` offsets passed in (Product 160, Required Qty 100, Unit 50, Spec 100, Status 100).
// Content is constrained (overflow hidden + ellipsis) so it can never bleed into the next sticky column or the scrollable area.
const STICKY_WIDTHS = { 0: 160, 160: 100, 260: 50, 310: 100, 410: 100 };
function stickyTh(left) {
  return {
    position: "sticky", left, width: STICKY_WIDTHS[left], minWidth: STICKY_WIDTHS[left], maxWidth: STICKY_WIDTHS[left],
    background: "#f5f6fa", zIndex: 2,
    padding: "8px 12px", borderBottom: "2px solid #e0e0e0",
    borderRight: "1px solid #e0e0e0", textAlign: "left", fontWeight: 600,
    whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis",
  };
}
function stickyTd(left) {
  return {
    position: "sticky", left, width: STICKY_WIDTHS[left], minWidth: STICKY_WIDTHS[left], maxWidth: STICKY_WIDTHS[left],
    background: "#fff", zIndex: 1,
    padding: "8px 12px", borderRight: "1px solid #f0f0f0", whiteSpace: "nowrap",
    overflow: "hidden", textOverflow: "ellipsis",
  };
}
const td = { padding: "8px 10px", whiteSpace: "nowrap" };
const subTh = { padding: "6px 10px", borderBottom: "2px solid #e0e0e0", fontWeight: 500, color: "#555" };

function truncate(str, n) {
  if (!str) return "—";
  return str.length > n ? str.substring(0, n) + "…" : str;
}

function getLeadDays(resp) {
  // deliveryLeadDays is on the SupplierQuote header, not line — show from line's expectedDeliveryDate
  if (resp.expectedDeliveryDate) {
    const d = new Date(resp.expectedDeliveryDate);
    return d.toLocaleDateString("en-IN");
  }
  return "—";
}

export default ComparisonMatrix;
