import React from "react";

const BUCKET_PRIORITY = ["GT_30_DAYS", "GT_15_DAYS", "GT_7_DAYS", "GT_3_DAYS"];

const BUCKET_COLOR = {
  GT_30_DAYS: { bar: "#E24B4A", label: "GT 30 days" },
  GT_15_DAYS: { bar: "#EF9F27", label: "GT 15 days" },
  GT_7_DAYS:  { bar: "#378ADD", label: "GT 7 days"  },
  GT_3_DAYS:  { bar: "#639922", label: "GT 3 days"  },
};

export function buildHorizontalStaleData(buckets = [], countsKey, keyLabel) {
  if (!Array.isArray(buckets) || !buckets.length) return [];

  const projectMap = {};

  buckets.forEach((bucket) => {
    const counts = bucket[countsKey] || {};
    Object.entries(counts).forEach(([key, count]) => {
      if (!projectMap[key]) {
        projectMap[key] = { key, name: keyLabel ? keyLabel(key) : key, total: 0, worstBucket: null };
      }
      projectMap[key].total += count || 0;
      if (count > 0) {
        const currentPriority = BUCKET_PRIORITY.indexOf(projectMap[key].worstBucket);
        const newPriority = BUCKET_PRIORITY.indexOf(bucket.bucket);
        if (projectMap[key].worstBucket === null || newPriority < currentPriority) {
          projectMap[key].worstBucket = bucket.bucket;
        }
      }
    });
  });

  return Object.values(projectMap)
    .filter((p) => p.total > 0)
    .sort((a, b) => b.total - a.total);
}

export function StaleChartLegend({ clickable }) {
  return (
    <div style={{ display: "flex", flexWrap: "wrap", gap: "4px 12px", marginBottom: 8, paddingBottom: 8, borderBottom: "1px solid #f0f0f0" }}>
      {BUCKET_PRIORITY.map((b) => (
        <span key={b} style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 11, color: "#777" }}>
          <span style={{ width: 9, height: 9, borderRadius: 2, background: BUCKET_COLOR[b].bar, display: "inline-block", flexShrink: 0 }} />
          {BUCKET_COLOR[b].label}
        </span>
      ))}
      {clickable && (
        <span style={{ fontSize: 11, color: "#aaa", marginLeft: "auto" }}>Click row to filter list →</span>
      )}
    </div>
  );
}

export default function StaleHorizontalBarChart({ rows = [], onRowClick }) {
  if (!rows.length) return null;

  const maxTotal = Math.max(...rows.map((r) => r.total), 1);
  const clickable = typeof onRowClick === "function";

  return (
    <div style={{ padding: "4px 0 0" }}>
      {rows.map((row) => {
        const color = (row.worstBucket && BUCKET_COLOR[row.worstBucket]?.bar) || "#888";
        const pct = Math.max((row.total / maxTotal) * 100, 4);
        return (
          <div
            key={row.key || row.name}
            onClick={clickable ? () => onRowClick(row) : undefined}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 8,
              marginBottom: 5,
              cursor: clickable ? "pointer" : "default",
              borderRadius: 4,
              padding: "2px 0",
            }}
            title={clickable ? `Click to view ${row.name} in list` : undefined}
          >
            <div style={{
              width: 110, flexShrink: 0,
              fontSize: 11, color: "#555",
              textAlign: "right",
              overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
              fontWeight: 500,
            }} title={row.name}>
              {row.name}
            </div>
            <div style={{
              flex: 1, height: 20,
              background: "#f0f0f0",
              borderRadius: 4,
              overflow: "hidden",
            }}>
              <div style={{
                width: `${pct}%`, height: "100%",
                background: color,
                borderRadius: 4,
                display: "flex", alignItems: "center",
                paddingLeft: 7,
                boxSizing: "border-box",
                transition: "width 0.3s ease",
              }}>
                <span style={{ fontSize: 11, fontWeight: 600, color: "#fff", whiteSpace: "nowrap" }}>
                  {row.total}
                </span>
              </div>
            </div>
          </div>
        );
      })}

    </div>
  );
}
