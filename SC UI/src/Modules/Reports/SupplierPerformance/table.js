import React from "react";
import {
  Table, TableHead, TableBody, TableRow, TableCell,
  TableSortLabel, Tooltip, Chip
} from "@material-ui/core";
import WarningIcon from "@material-ui/icons/Warning";

const COLUMNS = [
  { key: "supplierName",    label: "Supplier",       sortable: true  },
  { key: "totalPos",        label: "Total POs",      sortable: true,  align: "right" },
  { key: "completedPos",    label: "Completed",      sortable: false, align: "right" },
  { key: "pendingPos",      label: "Pending",        sortable: false, align: "right" },
  { key: "overduePos",      label: "Overdue",        sortable: true,  align: "right" },
  { key: "shortClosedPos",  label: "Short-closed",   sortable: false, align: "right" },
  { key: "cancelledPos",    label: "Cancelled",      sortable: false, align: "right" },
  { key: "onTimeRate",      label: "On-time %",      sortable: true,  align: "right" },
  { key: "totalOrderValue", label: "Order Value (₹)", sortable: true, align: "right" },
  { key: "lastPoDate",      label: "Last PO Date",    sortable: true,  align: "right" },
];

function OnTimeBar({ rate }) {
  if (rate === null || rate === undefined) return <span style={{ color: "#bbb", fontSize: 12 }}>No lead time data</span>;
  const color = rate >= 90 ? "#2e7d32" : rate >= 70 ? "#e65100" : "#b71c1c";
  const bg    = rate >= 90 ? "#e8f5e9"  : rate >= 70 ? "#fff3e0"  : "#ffebee";
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 6, justifyContent: "flex-end" }}>
      <div style={{ width: 60, height: 6, background: "#e0e0e0", borderRadius: 3, overflow: "hidden" }}>
        <div style={{ width: `${Math.min(rate, 100)}%`, height: "100%", background: color, borderRadius: 3 }} />
      </div>
      <span style={{ fontWeight: 700, color, minWidth: 42, textAlign: "right", background: bg, padding: "1px 6px", borderRadius: 4, fontSize: 12 }}>
        {rate.toFixed(1)}%
      </span>
    </div>
  );
}

function fmtValue(val) {
  if (val === null || val === undefined) return "—";
  if (val >= 10000000) return `₹${(val / 10000000).toFixed(2)}Cr`;
  if (val >= 100000)   return `₹${(val / 100000).toFixed(2)}L`;
  if (val >= 1000)     return `₹${(val / 1000).toFixed(1)}K`;
  return `₹${val.toFixed(0)}`;
}

function SupplierPerformanceTable({ rows, sortBy, sortDir, onSort, onSelectFirm }) {
  const createSortHandler = (key) => () => { if (onSort) onSort(key); };

  return (
    <div>
      <Table size="small">
        <TableHead>
          <TableRow style={{ background: "#f5f5f5" }}>
            {COLUMNS.map(col => (
              <TableCell
                key={col.key}
                align={col.align || "left"}
                style={{ fontWeight: 700, padding: "8px 6px" }}
              >
                {col.sortable ? (
                  <TableSortLabel
                    active={sortBy === col.key}
                    direction={sortBy === col.key ? sortDir : "asc"}
                    onClick={createSortHandler(col.key)}
                  >
                    {col.label}
                  </TableSortLabel>
                ) : col.label}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {(!rows || rows.length === 0) && (
            <TableRow>
              <TableCell colSpan={COLUMNS.length} align="center" style={{ padding: 40, color: "#999" }}>
                No data found
              </TableCell>
            </TableRow>
          )}
          {(rows || []).map((row, idx) => (
            <TableRow
              key={row.supplierId || idx}
              hover
              style={{ background: idx % 2 === 0 ? "#fff" : "#fafafa" }}
            >
              {/* Supplier name — clickable for price comparison */}
              <TableCell style={{ padding: "8px 12px" }}>
                <span
                  style={{ color: "#1565c0", fontWeight: 600, cursor: "pointer", textDecoration: "underline" }}
                  onClick={() => onSelectFirm && onSelectFirm(row)}
                >
                  {row.supplierName || "—"}
                </span>
              </TableCell>
              <TableCell align="right" style={{ fontWeight: 600 }}>{row.totalPos ?? "—"}</TableCell>
              <TableCell align="right" style={{ color: "#2e7d32" }}>{row.completedPos ?? "—"}</TableCell>
              <TableCell align="right" style={{ color: "#555" }}>{row.pendingPos ?? "—"}</TableCell>
              {/* Overdue — highlighted when > 0 */}
              <TableCell align="right">
                {row.overduePos > 0 ? (
                  <Tooltip title={`${row.overduePos} open PO(s) past expected lead time`}>
                    <Chip
                      size="small"
                      icon={<WarningIcon style={{ fontSize: 14 }} />}
                      label={row.overduePos}
                      style={{ background: "#ffebee", color: "#b71c1c", fontWeight: 700, height: 22 }}
                    />
                  </Tooltip>
                ) : (
                  <span style={{ color: "#aaa" }}>0</span>
                )}
              </TableCell>
              <TableCell align="right" style={{ color: row.shortClosedPos > 0 ? "#e65100" : "#aaa" }}>
                {row.shortClosedPos ?? "—"}
              </TableCell>
              <TableCell align="right" style={{ color: row.cancelledPos > 0 ? "#888" : "#aaa" }}>
                {row.cancelledPos ?? "—"}
              </TableCell>
              <TableCell align="right">
                <OnTimeBar rate={row.onTimeRate} />
              </TableCell>
              <TableCell align="right" style={{ fontWeight: 500 }}>
                <Tooltip title={row.totalOrderValue != null ? `₹${row.totalOrderValue.toLocaleString("en-IN", { maximumFractionDigits: 0 })}` : ""}>
                  <span>{row.totalOrderValue != null ? fmtValue(row.totalOrderValue) : "—"}</span>
                </Tooltip>
              </TableCell>
              <TableCell align="right" style={{ color: "#555", whiteSpace: "nowrap" }}>
                {row.lastPoDate || "—"}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

export default SupplierPerformanceTable;
