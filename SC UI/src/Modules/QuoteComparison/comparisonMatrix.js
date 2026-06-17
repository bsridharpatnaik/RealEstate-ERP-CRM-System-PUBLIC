import React from "react";
import Chip from "@material-ui/core/Chip";
import Tooltip from "@material-ui/core/Tooltip";
import RadioButtonCheckedIcon from "@material-ui/icons/RadioButtonChecked";
import RadioButtonUncheckedIcon from "@material-ui/icons/RadioButtonUnchecked";

const LINE_STATUS_COLORS = {
  OPEN:      { bg: "#e3f2fd", color: "#1565c0" },
  FINALIZED: { bg: "#e8f5e9", color: "#2e7d32" },
  PO_LINKED: { bg: "#ede7f6", color: "#4527a0" },
  CLOSED:    { bg: "#f5f5f5", color: "#666" },
};

function ComparisonMatrix({ matrix, criteria, onSelectWinner, canFinalize }) {
  if (!matrix || matrix.length === 0) {
    return <div style={{ padding: 32, textAlign: "center", color: "#aaa" }}>No data. Add supplier quotes first.</div>;
  }

  // Collect all suppliers across all rows
  const supplierMap = {};
  matrix.forEach(row => {
    (row.supplierResponses || []).forEach(sr => {
      if (!supplierMap[sr.supplierQuoteId]) {
        supplierMap[sr.supplierQuoteId] = sr.supplierName;
      }
    });
  });
  const supplierIds = Object.keys(supplierMap);

  return (
    <div style={{ overflowX: "auto" }}>
      <table style={{ borderCollapse: "collapse", fontSize: 12, minWidth: "100%" }}>
        <thead>
          <tr style={{ background: "#f5f6fa" }}>
            {/* Sticky left columns */}
            <th style={stickyTh(0)}>Product</th>
            <th style={stickyTh(120)}>Required Qty</th>
            <th style={stickyTh(220)}>Unit</th>
            <th style={stickyTh(270)}>Spec</th>
            <th style={stickyTh(370)}>Status</th>
            {/* Per supplier blocks */}
            {supplierIds.map(sqId => (
              <th key={sqId} colSpan={3 + (criteria?.length || 0)}
                style={{ padding: "8px 12px", borderBottom: "2px solid #e0e0e0",
                  borderLeft: "2px solid #bdbdbd", textAlign: "center",
                  background: "#e8eaf6", color: "#3949ab" }}>
                {supplierMap[sqId]}
              </th>
            ))}
            {canFinalize && <th style={{ padding: "8px 12px", borderBottom: "2px solid #e0e0e0" }}>Award</th>}
          </tr>
          <tr style={{ background: "#fafafa", fontSize: 11 }}>
            <th style={stickyTh(0)} />
            <th style={stickyTh(120)} />
            <th style={stickyTh(220)} />
            <th style={stickyTh(270)} />
            <th style={stickyTh(370)} />
            {supplierIds.map(sqId => (
              <React.Fragment key={sqId}>
                <th style={{ ...subTh, borderLeft: "2px solid #bdbdbd" }}>Rate</th>
                <th style={subTh}>Landed</th>
                <th style={subTh}>Lead (d)</th>
                {(criteria || []).map(c => <th key={c.id} style={subTh}>{c.criteriaName}</th>)}
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
                <td style={stickyTd(0)}><strong>{row.productName}</strong></td>
                <td style={stickyTd(120)}>{row.requiredQty}</td>
                <td style={stickyTd(220)}>{row.unit}</td>
                <td style={{ ...stickyTd(270), fontSize: 11, color: "#666", maxWidth: 100 }}>
                  <Tooltip title={row.specifications || ""}>
                    <span>{truncate(row.specifications, 40)}</span>
                  </Tooltip>
                </td>
                <td style={stickyTd(370)}>
                  <Chip label={row.lineStatus?.replace(/_/g, " ")} size="small"
                    style={{ background: statusStyle.bg, color: statusStyle.color, fontWeight: 600, fontSize: 10 }} />
                </td>

                {/* Per supplier columns */}
                {supplierIds.map(sqId => {
                  const resp = (row.supplierResponses || []).find(sr => String(sr.supplierQuoteId) === sqId);
                  if (!resp || !resp.hasResponse) {
                    return (
                      <React.Fragment key={sqId}>
                        <td style={{ ...td, borderLeft: "2px solid #bdbdbd", color: "#ccc" }}>—</td>
                        <td style={td} />
                        <td style={td} />
                        {(criteria || []).map(c => <td key={c.id} style={td} />)}
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
                      <td style={td}>{getLeadDays(resp, supplierMap, sqId, matrix)}</td>
                      {(criteria || []).map(c => {
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
                        {supplierIds.map(sqId => {
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
        </tbody>
      </table>
    </div>
  );
}

// Helpers
function stickyTh(left) {
  return {
    position: "sticky", left, background: "#f5f6fa", zIndex: 2,
    padding: "8px 12px", borderBottom: "2px solid #e0e0e0",
    borderRight: "1px solid #e0e0e0", textAlign: "left", fontWeight: 600,
    whiteSpace: "nowrap",
  };
}
function stickyTd(left) {
  return {
    position: "sticky", left, background: "#fff", zIndex: 1,
    padding: "8px 12px", borderRight: "1px solid #f0f0f0", whiteSpace: "nowrap",
  };
}
const td = { padding: "8px 10px", whiteSpace: "nowrap" };
const subTh = { padding: "6px 10px", borderBottom: "2px solid #e0e0e0", fontWeight: 500, color: "#555" };

function truncate(str, n) {
  if (!str) return "—";
  return str.length > n ? str.substring(0, n) + "…" : str;
}

function getLeadDays(resp, supplierMap, sqId, matrix) {
  // deliveryLeadDays is on the SupplierQuote header, not line — show from line's expectedDeliveryDate
  if (resp.expectedDeliveryDate) {
    const d = new Date(resp.expectedDeliveryDate);
    return d.toLocaleDateString("en-IN");
  }
  return "—";
}

export default ComparisonMatrix;
