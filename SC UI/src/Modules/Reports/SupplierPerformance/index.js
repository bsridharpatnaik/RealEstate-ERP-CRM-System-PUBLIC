import React, { Component } from "react";
import { withSnackbar } from "notistack";
import Popper from "@material-ui/core/Popper";
import {
  Paper, Grid, Typography, CircularProgress,
  TablePagination, Tooltip, Divider,
  Drawer, LinearProgress
} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import IconButton from "@material-ui/core/IconButton";
import TrendingDownIcon from "@material-ui/icons/TrendingDown";
import TrendingUpIcon from "@material-ui/icons/TrendingUp";
import RemoveIcon from "@material-ui/icons/Remove";
import IconButtons from "../../../Shared/Button/IconButtons";

import { API } from "../../../axios";
import { apiEndpoints } from "../../../endpoints";
import Filter from "./filter";
import SupplierPerformanceTable from "./table";

const SORT_COL_MAP = {
  supplierName:    "supplierName",
  totalPos:        "totalPos",
  completedPos:    "completedPos",
  overduePos:      "overduePos",
  onTimeRate:      "onTimeRate",
  totalOrderValue: "totalOrderValue",
  lastPoDate:      "lastPoDate",
};

// ── Price comparison scatter chart ────────────────────────────────────────────

function PriceScatterChart({ items, supplierName }) {
  const [tooltip, setTooltip] = React.useState(null);
  const PAD_L = 160, PAD_R = 100, PAD_T = 24, ROW_H = 50;
  const W = 500, H = PAD_T + items.length * ROW_H + 16;

  if (items.length === 0) return null;

  const fmtRate = (v) => v >= 1000 ? `₹${(v / 1000).toFixed(1)}K` : `₹${v}`;

  return (
    <div style={{ overflowX: "auto", padding: "0 20px" }}>
      <svg width="100%" viewBox={`0 0 ${W} ${H}`} style={{ fontFamily: "inherit", display: "block" }}>
        <text x={PAD_L} y={14} fontSize={10} fill="#999" textAnchor="middle">Min</text>
        <text x={PAD_L + (W - PAD_L - PAD_R) * 0.5} y={14} fontSize={10} fill="#1565c0" textAnchor="middle">Market Avg</text>
        <text x={W - PAD_R} y={14} fontSize={10} fill="#999" textAnchor="middle">Max</text>

        {items.map((item, i) => {
          const y = PAD_T + i * ROW_H;
          const cy = y + ROW_H / 2;

          const allVals = [item.marketMin, item.marketMax, item.thisRate].filter(v => v != null && v > 0);
          if (allVals.length === 0) return null;
          const lo = Math.min(...allVals) * 0.88;
          const hi = Math.max(...allVals) * 1.12;
          const scale = (v) => PAD_L + ((v - lo) / (hi - lo)) * (W - PAD_L - PAD_R);

          const cheaper = item.thisRate <= (item.marketAvg || item.thisRate);
          const dotColor = cheaper ? "#2e7d32" : "#c62828";

          const xMin = item.marketMin != null ? scale(item.marketMin) : null;
          const xAvg = item.marketAvg != null ? scale(item.marketAvg) : null;
          const xMax = item.marketMax != null ? scale(item.marketMax) : null;
          const xMe  = scale(item.thisRate);

          const idx = item.priceIndex;
          const idxText = idx != null ? (idx <= 95 ? `↓${100 - idx}% below avg` : idx >= 105 ? `↑${idx - 100}% above avg` : "≈ avg") : "";
          const idxColor = idx != null ? (idx <= 95 ? "#2e7d32" : idx >= 105 ? "#c62828" : "#1565c0") : "#888";

          return (
            <g key={item.productId}>
              {i > 0 && <line x1={0} y1={y} x2={W} y2={y} stroke="#f0f0f0" strokeWidth={1} />}

              <text x={PAD_L - 8} y={cy - 6} fontSize={11} fill="#333" textAnchor="end" fontWeight="600">
                {item.productName.length > 20 ? item.productName.slice(0, 19) + "…" : item.productName}
              </text>
              <text x={PAD_L - 8} y={cy + 7} fontSize={9} fill="#999" textAnchor="end">
                {item.unit} · {item.poCount} PO{item.poCount !== 1 ? "s" : ""}
              </text>

              {/* Market range band */}
              {xMin != null && xMax != null && (
                <rect x={xMin} y={cy - 6} width={xMax - xMin} height={12}
                  fill="rgba(33,150,243,0.15)" rx={3} />
              )}

              {/* Market avg line */}
              {xAvg != null && (
                <line x1={xAvg} y1={cy - 11} x2={xAvg} y2={cy + 11}
                  stroke="#1565c0" strokeWidth={2} strokeDasharray="3,2" />
              )}

              {/* Axis labels */}
              {xMin != null && (
                <text x={xMin} y={cy + 22} fontSize={8} fill="#aaa" textAnchor="middle">{fmtRate(item.marketMin)}</text>
              )}
              {xAvg != null && (
                <text x={xAvg} y={cy + 22} fontSize={8} fill="#1565c0" textAnchor="middle" fontWeight="bold">{fmtRate(item.marketAvg)}</text>
              )}
              {xMax != null && (
                <text x={xMax} y={cy + 22} fontSize={8} fill="#aaa" textAnchor="middle">{fmtRate(item.marketMax)}</text>
              )}

              {/* This supplier dot */}
              <circle cx={xMe} cy={cy - 2} r={7} fill={dotColor} stroke="white" strokeWidth={2}
                style={{ cursor: "pointer", filter: "drop-shadow(0 1px 3px rgba(0,0,0,0.3))" }}
                onMouseEnter={() => setTooltip({ x: xMe, y: cy - 20, text: `${supplierName}: ₹${item.thisRate}` })}
                onMouseLeave={() => setTooltip(null)}
              />
              <text x={xMe} y={cy - 13} fontSize={9} fill={dotColor} textAnchor="middle" fontWeight="bold">
                {fmtRate(item.thisRate)}
              </text>

              {/* Price index */}
              {idxText && (
                <text x={W - PAD_R + 6} y={cy} fontSize={9} fill={idxColor} fontWeight="bold">{idxText}</text>
              )}
            </g>
          );
        })}

        {tooltip && (
          <g>
            <rect x={tooltip.x - 65} y={tooltip.y - 14} width={130} height={18} rx={4} fill="rgba(0,0,0,0.75)" />
            <text x={tooltip.x} y={tooltip.y - 1} fontSize={10} fill="white" textAnchor="middle">{tooltip.text}</text>
          </g>
        )}
      </svg>

      <div style={{ display: "flex", gap: 16, marginTop: 4, marginBottom: 4, fontSize: 11, color: "#666" }}>
        <span>
          <svg width="12" height="12" style={{ verticalAlign: "middle", marginRight: 4 }}>
            <circle cx="6" cy="6" r="5" fill="#2e7d32" stroke="white" strokeWidth="1.5" />
          </svg>
          At / below market avg
        </span>
        <span>
          <svg width="12" height="12" style={{ verticalAlign: "middle", marginRight: 4 }}>
            <circle cx="6" cy="6" r="5" fill="#c62828" stroke="white" strokeWidth="1.5" />
          </svg>
          Above market avg
        </span>
        <span>
          <svg width="24" height="12" style={{ verticalAlign: "middle", marginRight: 4 }}>
            <rect x="0" y="3" width="24" height="6" rx="2" fill="rgba(33,150,243,0.2)" />
          </svg>
          Market range (min–max)
        </span>
      </div>
      <div style={{ fontSize: 11, color: "#aaa", marginBottom: 8 }}>All rates are net of discount</div>
    </div>
  );
}

function PriceComparisonPanel({ firm, onClose }) {
  const [items, setItems] = React.useState([]);
  const [loading, setLoading] = React.useState(false);

  React.useEffect(() => {
    if (!firm) return;
    setLoading(true);
    API.GET(apiEndpoints.supplierPriceComparison(firm.supplierId))
      .then(res => setItems(res.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [firm]);

  if (!firm) return null;

  return (
    <div style={{ width: 560, paddingBottom: 80 }}>
      <div style={{
        display: "flex", alignItems: "center", justifyContent: "space-between",
        padding: "16px 20px", background: "#1565c0", color: "#fff",
        position: "sticky", top: 0, zIndex: 1
      }}>
        <div>
          <Typography variant="h6" style={{ color: "#fff", fontWeight: 700 }}>{firm.supplierName}</Typography>
          <Typography style={{ color: "rgba(255,255,255,0.8)", fontSize: 13 }}>
            Price vs market — {items.length} product(s) · net of discount
          </Typography>
        </div>
        <IconButton size="small" onClick={onClose} style={{ color: "#fff" }}>
          <CloseIcon />
        </IconButton>
      </div>

      {loading && <LinearProgress />}

      {!loading && items.length === 0 && (
        <div style={{ padding: 40, textAlign: "center", color: "#999" }}>
          No price data available. Rates may not be entered for POs from this supplier.
        </div>
      )}

      {!loading && items.length > 0 && (
        <div style={{ paddingTop: 16 }}>
          <PriceScatterChart items={items} supplierName={firm.supplierName} />
        </div>
      )}
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

class SupplierPerformanceReport extends Component {
  filterData = {};

  state = {
    rows: [],
    tiles: {},
    tilesLoading: true,
    loading: false,
    filterOpen: false,
    page: 0,
    size: 50,
    totalRecords: 0,
    sortBy: "onTimeRate",
    sortDir: "asc",
    exporting: false,
    selectedFirm: null,
    categories: [],
  };

  componentDidMount() {
    this.filterRef = React.createRef();
    this.fetchCategories();
    this.fetchTiles();
    this.fetchData();
  }

  async fetchCategories() {
    const res = await API.GET(apiEndpoints.getCategoryIdAndNames);
    if (res.success && Array.isArray(res.data)) {
      const categories = res.data
        .map((c) => c.categoryName ?? c.name)
        .filter(Boolean)
        .sort();
      this.setState({ categories });
    }
  }

  buildRequestBody() {
    const fd = this.filterData || {};
    const filterData = [];
    if (fd.startDate)    filterData.push({ attrName: "startDate",    attrValue: [fd.startDate] });
    if (fd.endDate)      filterData.push({ attrName: "endDate",      attrValue: [fd.endDate] });
    if (fd.supplierName) filterData.push({ attrName: "supplierName", attrValue: [fd.supplierName] });
    if (fd.categoryName) filterData.push({ attrName: "categoryName", attrValue: [fd.categoryName] });
    return { filterData };
  }

  async fetchTiles() {
    this.setState({ tilesLoading: true });
    try {
      const res = await API.POST(apiEndpoints.supplierPerformanceTiles, this.buildRequestBody());
      this.setState({ tiles: res.data || {} });
    } catch (e) {
      this.props.enqueueSnackbar("Failed to load tiles", { variant: "error" });
    } finally {
      this.setState({ tilesLoading: false });
    }
  }

  async fetchData(pageOverride) {
    const { page, size, sortBy, sortDir } = this.state;
    const p = pageOverride !== undefined ? pageOverride : page;
    const col = SORT_COL_MAP[sortBy] || "onTimeRate";
    const url = `${apiEndpoints.supplierPerformanceList}&page=${p}&size=${size}&sort=${col},${sortDir}`;
    this.setState({ loading: true });
    try {
      const res = await API.POST(url, this.buildRequestBody());
      const d = res.data || {};
      this.setState({ rows: d.content || [], totalRecords: d.totalElements || 0 });
    } catch (e) {
      this.props.enqueueSnackbar("Failed to load data", { variant: "error" });
    } finally {
      this.setState({ loading: false });
    }
  }

  handleSort = (key) => {
    const { sortBy, sortDir } = this.state;
    const newDir = sortBy === key && sortDir === "asc" ? "desc" : "asc";
    this.setState({ sortBy: key, sortDir: newDir, page: 0 }, () => this.fetchData(0));
  };

  exportExcel = async () => {
    this.setState({ exporting: true });
    try {
      const res = await API.POST(
        apiEndpoints.supplierPerformanceExport,
        this.buildRequestBody(),
        { responseType: "blob" }
      );
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "supplier_performance.xlsx");
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (e) {
      this.props.enqueueSnackbar("Export failed", { variant: "error" });
    } finally {
      this.setState({ exporting: false });
    }
  };

  renderTile(label, value, color, bg, tooltip = "") {
    const { tilesLoading } = this.state;
    const inner = (
      <Paper elevation={2} style={{
        background: bg, borderLeft: `4px solid ${color}`,
        padding: "14px 18px", borderRadius: 8, minWidth: 155,
        cursor: tooltip ? "help" : "default"
      }}>
        <Typography style={{ fontSize: 11, color: "#666", marginBottom: 4, textTransform: "uppercase", letterSpacing: 0.5 }}>
          {label}
        </Typography>
        {tilesLoading
          ? <CircularProgress size={20} style={{ color }} />
          : <Typography style={{ fontSize: 24, fontWeight: 700, color }}>{value ?? "—"}</Typography>
        }
      </Paper>
    );
    return tooltip ? <Tooltip title={tooltip}>{inner}</Tooltip> : inner;
  }

  fmtValue(val) {
    if (val === null || val === undefined) return "—";
    if (val >= 10000000) return `₹${(val / 10000000).toFixed(2)} Cr`;
    if (val >= 100000)   return `₹${(val / 100000).toFixed(2)} L`;
    return `₹${Math.round(val).toLocaleString("en-IN")}`;
  }

  render() {
    const { rows, loading, filterOpen, page, size, totalRecords,
            sortBy, sortDir, tiles, exporting, selectedFirm, categories } = this.state;

    const onTimeColor = tiles.avgOnTimeRate >= 90 ? "#2e7d32" : tiles.avgOnTimeRate >= 70 ? "#e65100" : "#b71c1c";
    const onTimeBg    = tiles.avgOnTimeRate >= 90 ? "#e8f5e9"  : tiles.avgOnTimeRate >= 70 ? "#fff3e0"  : "#ffebee";

    return (
      <div className="page">
        <div className="header-info">
          <div>
            <h2 className="page-title">Supplier Performance</h2>
          </div>
        </div>

        <div className="list-section">
          {/* Summary Tiles */}
          <Grid container spacing={2} style={{ marginBottom: 20 }}>
            <Grid item>
              {this.renderTile("Total Suppliers", tiles.totalSuppliers, "#5e81f4", "#eef1fe")}
            </Grid>
            <Grid item>
              {this.renderTile("Total POs", tiles.totalPos, "#27ae60", "#eafaf1")}
            </Grid>
            <Grid item>
              {this.renderTile(
                "Avg On-time Rate",
                tiles.avgOnTimeRate != null ? `${tiles.avgOnTimeRate.toFixed(1)}%` : "—",
                onTimeColor, onTimeBg,
                "% of non-cancelled POs not overdue their expected lead time"
              )}
            </Grid>
            <Grid item>
              {this.renderTile(
                "Overdue POs",
                tiles.totalOverdue,
                tiles.totalOverdue > 0 ? "#b71c1c" : "#27ae60",
                tiles.totalOverdue > 0 ? "#ffebee" : "#eafaf1",
                "Open POs past their expected delivery date"
              )}
            </Grid>
            <Grid item>
              {this.renderTile("Total Order Value", this.fmtValue(tiles.totalOrderValue), "#7b1fa2", "#f3e5f5")}
            </Grid>
          </Grid>

          <div className="filter-section">
            <div style={{ display: "flex", gap: 16, fontSize: 12, color: "#666", alignItems: "center" }}>
              <span><span style={{ color: "#2e7d32", fontWeight: 700 }}>■</span> ≥90% Excellent</span>
              <span><span style={{ color: "#e65100", fontWeight: 700 }}>■</span> 70–89% Average</span>
              <span><span style={{ color: "#b71c1c", fontWeight: 700 }}>■</span> &lt;70% Poor</span>
              <span style={{ color: "#aaa", fontSize: 11 }}>· Click supplier name for price comparison</span>
            </div>
            <div className="top-button-wrapper" style={{ display: "flex", gap: 8 }}>
              <IconButtons
                onClick={this.exportExcel}
                buttonClass="filterIcon"
                label="Export Excel"
                icon="DownloadSVG"
              />
              <IconButtons
                onClick={() => this.setState({ filterOpen: true })}
                buttonClass="filterIcon"
                label="Filter"
                icon="FilterSVG"
                innerRef={this.filterRef}
              />
            </div>
          </div>

          {/* Filter Popper */}
          <Popper
            open={filterOpen}
            anchorEl={this.filterRef && this.filterRef.current}
            placement="bottom-end"
            style={{ zIndex: 1300 }}
          >
            <Filter
              filterData={this.filterData}
              options={{ categories }}
              search={(data) => {
                this.filterData = data;
                this.setState({ filterOpen: false, page: 0 }, () => {
                  this.fetchTiles();
                  this.fetchData(0);
                });
              }}
              close={() => this.setState({ filterOpen: false })}
            />
          </Popper>

          {/* Table */}
          <Paper elevation={2}>
            {loading ? (
              <div style={{ textAlign: "center", padding: 40 }}><CircularProgress /></div>
            ) : (
              <SupplierPerformanceTable
                rows={rows}
                sortBy={sortBy}
                sortDir={sortDir}
                onSort={this.handleSort}
                onSelectFirm={(firm) => this.setState({ selectedFirm: firm })}
              />
            )}
            <Divider />
            <TablePagination
              component="div"
              count={totalRecords}
              page={page}
              onChangePage={(e, p) => this.setState({ page: p }, () => this.fetchData(p))}
              rowsPerPage={size}
              onChangeRowsPerPage={(e) => this.setState({ size: parseInt(e.target.value, 10), page: 0 }, () => this.fetchData(0))}
              rowsPerPageOptions={[25, 50, 100]}
            />
          </Paper>

          {/* Price comparison drawer */}
          <Drawer
            anchor="right"
            open={!!selectedFirm}
            onClose={() => this.setState({ selectedFirm: null })}
            PaperProps={{ style: { width: 560 } }}
          >
            <PriceComparisonPanel
              firm={selectedFirm}
              onClose={() => this.setState({ selectedFirm: null })}
            />
          </Drawer>
        </div>
      </div>
    );
  }
}

export default withSnackbar(SupplierPerformanceReport);
