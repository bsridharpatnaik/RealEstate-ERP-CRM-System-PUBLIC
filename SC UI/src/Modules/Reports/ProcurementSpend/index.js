import React from "react";
import { withSnackbar } from "notistack";
import { API } from "../../../axios";
import { apiEndpoints } from "../../../endpoints";

// ── formatting helpers ──────────────────────────────────────────────────────
const fmtFull = (n) => "₹" + Number(n || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 });
const fmtShort = (n) => {
  n = Number(n || 0);
  if (n >= 1e7) return "₹" + (n / 1e7).toFixed(2) + " Cr";
  if (n >= 1e5) return "₹" + (n / 1e5).toFixed(2) + " L";
  return "₹" + n.toLocaleString("en-IN", { maximumFractionDigits: 0 });
};
const PALETTE = ["#7f8c8d", "#f39c12", "#e67e22", "#c0392b"]; // grey, amber, orange, red
const bucketColor = (amount, bands) => {
  if (!bands || !bands.length) return PALETTE[0];
  for (let i = 0; i < bands.length; i++) {
    if (Number(amount) < Number(bands[i])) return PALETTE[Math.min(i, PALETTE.length - 1)];
  }
  return PALETTE[PALETTE.length - 1];
};

const isoToday = () => new Date().toISOString().slice(0, 10);
const isoMonthsAgo = (m) => { const d = new Date(); d.setMonth(d.getMonth() - m); return d.toISOString().slice(0, 10); };
const DEFAULT_RANGE = () => ({ startDate: isoMonthsAgo(3), endDate: isoToday() });

const LIST_TABS = [
  { key: "highValue", label: "High-Value POs" },
  { key: "open", label: "Open High-Value" },
  { key: "recent", label: "Recent (90d)" },
  { key: "all", label: "All POs" },
];

const STATUSES = ["NEW", "PARTIAL", "COMPLETED", "SHORT CLOSED"];

class ProcurementSpend extends React.Component {
  state = {
    summary: null,
    byProject: [],
    bySupplier: [],
    byFirm: [],
    trend: [],
    poList: [],
    listTab: "highValue",
    loading: false,
    detailPo: null,
    detailLoading: false,
    projects: [],
    suppliers: [],
    firms: [],
    filters: { ...DEFAULT_RANGE(), project: "", supplier: "", firm: "", status: "", minValue: "", maxValue: "" },
  };

  componentDidMount() {
    this.fetchDropdowns();
    this.fetchAll();
  }

  async fetchDropdowns() {
    const [p, s, f] = await Promise.all([
      API.GET(apiEndpoints.poSpendProjects),
      API.GET(apiEndpoints.poSpendSuppliers),
      API.GET(apiEndpoints.poSpendFirms),
    ]);
    this.setState({
      projects: (p?.success && Array.isArray(p.data)) ? p.data : [],
      suppliers: (s?.success && Array.isArray(s.data)) ? s.data : [],
      firms: (f?.success && Array.isArray(f.data)) ? f.data : [],
    });
  }

  buildFilterData() {
    const f = this.state.filters;
    const fd = [];
    if (f.startDate) fd.push({ attrName: "startDate", attrValue: [f.startDate] });
    if (f.endDate) fd.push({ attrName: "endDate", attrValue: [f.endDate] });
    if (f.project) fd.push({ attrName: "projects", attrValue: [f.project] });
    if (f.supplier) fd.push({ attrName: "suppliers", attrValue: [f.supplier] });
    if (f.firm) fd.push({ attrName: "firms", attrValue: [f.firm] });
    if (f.status) fd.push({ attrName: "statuses", attrValue: [f.status] });
    if (f.minValue) fd.push({ attrName: "minValue", attrValue: [String(f.minValue)] });
    if (f.maxValue) fd.push({ attrName: "maxValue", attrValue: [String(f.maxValue)] });
    return { filterData: fd };
  }

  async fetchAll() {
    this.setState({ loading: true });
    const body = this.buildFilterData();
    // Trend is a 12-month history widget — keep it independent of the page date range.
    const trendBody = { filterData: body.filterData.filter((x) => x.attrName !== "startDate" && x.attrName !== "endDate") };
    try {
      const [summary, byProject, bySupplier, byFirm, trend] = await Promise.all([
        API.POST(apiEndpoints.poSpendSummary, body),
        API.POST(apiEndpoints.poSpendByProject, body),
        API.POST(apiEndpoints.poSpendBySupplier, body),
        API.POST(apiEndpoints.poSpendByFirm, body),
        API.POST(apiEndpoints.poSpendTrend, trendBody),
      ]);
      this.setState({
        summary: summary?.success ? summary.data : null,
        byProject: byProject?.success ? byProject.data : [],
        bySupplier: bySupplier?.success ? bySupplier.data : [],
        byFirm: byFirm?.success ? byFirm.data : [],
        trend: trend?.success ? trend.data : [],
      });
      await this.fetchList(this.state.listTab);
    } catch (e) {
      this.props.enqueueSnackbar("Failed to load procurement spend data", { variant: "error" });
    } finally {
      this.setState({ loading: false });
    }
  }

  listRequest(tab) {
    const body = this.buildFilterData();
    let url = apiEndpoints.poSpendListHighValue;
    if (tab === "all") {
      url = apiEndpoints.poSpendList;
    } else if (tab === "open") {
      body.filterData = body.filterData.filter((x) => x.attrName !== "statuses");
      body.filterData.push({ attrName: "statuses", attrValue: ["NEW", "PARTIAL"] });
    } else if (tab === "recent") {
      const d = new Date();
      d.setDate(d.getDate() - 90);
      body.filterData = body.filterData.filter((x) => x.attrName !== "startDate");
      body.filterData.push({ attrName: "startDate", attrValue: [d.toISOString().slice(0, 10)] });
    }
    return { url, body };
  }

  async fetchList(tab) {
    const { url, body } = this.listRequest(tab);
    const res = await API.POST(url, body);
    this.setState({
      listTab: tab,
      poList: (res?.success && res.data && res.data.content) ? res.data.content : [],
    });
  }

  async exportExcel() {
    const highValueOnly = this.state.listTab !== "all";
    const url = highValueOnly ? apiEndpoints.poSpendExportHighValue : apiEndpoints.poSpendExport;
    const res = await API.POSTBlob(url, this.buildFilterData());
    if (res?.success) {
      const blobUrl = window.URL.createObjectURL(new Blob([res.data]));
      const a = document.createElement("a");
      a.href = blobUrl;
      a.download = "po-spend.xlsx";
      a.click();
    } else {
      this.props.enqueueSnackbar("Export failed", { variant: "error" });
    }
  }

  async openDetail(poId) {
    if (!poId) return;
    this.setState({ detailLoading: true, detailPo: { purchaseOrderId: poId } });
    const res = await API.GET(apiEndpoints.getPurchaseOrderDetail(poId));
    if (res?.success && res.data) {
      this.setState({ detailPo: res.data, detailLoading: false });
    } else {
      this.setState({ detailLoading: false, detailPo: null });
      this.props.enqueueSnackbar("Failed to load PO details", { variant: "error" });
    }
  }

  closeDetail = () => this.setState({ detailPo: null, detailLoading: false });

  setFilter = (key, value) => this.setState((s) => ({ filters: { ...s.filters, [key]: value } }));

  resetFilters = () => {
    this.setState(
      { filters: { ...DEFAULT_RANGE(), project: "", supplier: "", firm: "", status: "", minValue: "", maxValue: "" } },
      () => this.fetchAll()
    );
  };

  // ── render ────────────────────────────────────────────────────────────────

  renderTiles() {
    const s = this.state.summary;
    if (!s) return null;
    const tiles = [
      { label: "Total Spend", value: fmtShort(s.totalSpend), sub: `${s.totalPoCount} POs`, color: "#5e81f4", bg: "#eef1fe" },
      { label: "Open Commitment", value: fmtShort(s.openCommitment), sub: `${s.openPoCount} open POs`, color: "#e67e22", bg: "#fdf2e9" },
      { label: "High-Value POs", value: s.highValueCount, sub: `${fmtShort(s.highValueSpend)} · > ${fmtShort(s.highValueThreshold)}`, color: "#c0392b", bg: "#fdedec" },
      { label: "Largest PO", value: fmtShort(s.largestPoValue), sub: "single PO", color: "#8e44ad", bg: "#f5eef8" },
      { label: "Avg PO Value", value: fmtShort(s.avgPoValue), sub: "per PO", color: "#16a085", bg: "#e8f8f5" },
      { label: "Short-Closed", value: fmtShort(s.shortClosedValue), sub: `${s.shortClosedCount} POs · leakage`, color: "#7f8c8d", bg: "#f4f6f6" },
    ];
    return (
      <div style={{ display: "flex", flexWrap: "wrap", gap: 12, marginBottom: 20 }}>
        {tiles.map((t, i) => (
          <div key={i} style={{ flex: "1 1 150px", minWidth: 150, background: t.bg, borderRadius: 8, padding: "12px 14px", borderLeft: `4px solid ${t.color}` }}>
            <div style={{ fontSize: 12, color: "#555", marginBottom: 4 }}>{t.label}</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: t.color }}>{t.value}</div>
            <div style={{ fontSize: 11, color: "#888", marginTop: 2 }}>{t.sub}</div>
          </div>
        ))}
      </div>
    );
  }

  renderBreakdown(title, rows) {
    const max = rows.reduce((m, r) => Math.max(m, r.totalValue), 0) || 1;
    return (
      <div style={{ flex: "1 1 300px", minWidth: 300, background: "#fff", border: "1px solid #e6e9ef", borderRadius: 8, padding: 14 }}>
        <div style={{ fontWeight: 600, marginBottom: 10, color: "#323c47" }}>{title}</div>
        {rows.length === 0 && <div style={{ fontSize: 12, color: "#999" }}>No data</div>}
        {rows.slice(0, 10).map((r, i) => (
          <div key={i} style={{ marginBottom: 8 }}>
            <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginBottom: 2 }}>
              <span style={{ color: "#333", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 180 }} title={r.label}>{r.label}</span>
              <span style={{ fontWeight: 600 }}>{fmtShort(r.totalValue)} <span style={{ color: "#aaa", fontWeight: 400 }}>({r.poCount})</span></span>
            </div>
            <div style={{ height: 6, background: "#eef1f6", borderRadius: 3 }}>
              <div style={{ width: `${(r.totalValue / max) * 100}%`, height: "100%", background: "#5e81f4", borderRadius: 3 }} />
            </div>
          </div>
        ))}
      </div>
    );
  }

  renderTrend() {
    const rows = this.state.trend;
    const max = rows.reduce((m, r) => Math.max(m, r.totalValue), 0) || 1;
    return (
      <div style={{ background: "#fff", border: "1px solid #e6e9ef", borderRadius: 8, padding: 14, marginBottom: 20 }}>
        <div style={{ fontWeight: 600, marginBottom: 12, color: "#323c47" }}>Monthly Spend (last 12 months)</div>
        {rows.length === 0 ? (
          <div style={{ fontSize: 12, color: "#999" }}>No data</div>
        ) : (
          <div style={{ display: "flex", alignItems: "flex-end", gap: 8, height: 160 }}>
            {rows.map((r, i) => (
              <div key={i} style={{ flex: 1, textAlign: "center" }} title={`${r.month}: ${fmtFull(r.totalValue)} (${r.poCount} POs)`}>
                <div style={{ fontSize: 10, color: "#666", marginBottom: 4 }}>{fmtShort(r.totalValue)}</div>
                <div style={{ height: `${(r.totalValue / max) * 120}px`, background: "#5e81f4", borderRadius: "3px 3px 0 0", minHeight: 2 }} />
                <div style={{ fontSize: 10, color: "#888", marginTop: 4 }}>{r.month}</div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  renderList() {
    const bands = this.state.summary ? this.state.summary.colorBands : [];
    return (
      <div style={{ background: "#fff", border: "1px solid #e6e9ef", borderRadius: 8, padding: 14 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12, flexWrap: "wrap" }}>
          {LIST_TABS.map((t) => (
            <button key={t.key} onClick={() => this.fetchList(t.key)}
              style={{ padding: "5px 12px", borderRadius: 16, border: "1px solid", cursor: "pointer", fontSize: 12,
                borderColor: this.state.listTab === t.key ? "#5e81f4" : "#dfe3ea",
                background: this.state.listTab === t.key ? "#5e81f4" : "#fff",
                color: this.state.listTab === t.key ? "#fff" : "#555" }}>
              {t.label}
            </button>
          ))}
          <div style={{ marginLeft: "auto" }}>
            <button onClick={() => this.exportExcel()} style={{ padding: "5px 12px", borderRadius: 4, border: "1px solid #27ae60", background: "#27ae60", color: "#fff", cursor: "pointer", fontSize: 12 }}>Export Excel</button>
          </div>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
            <thead>
              <tr style={{ background: "#f5f7fa", color: "#323c47", textAlign: "left" }}>
                <th style={thStyle}>PO No</th>
                <th style={thStyle}>Date</th>
                <th style={thStyle}>Project</th>
                <th style={thStyle}>Supplier</th>
                <th style={thStyle}>Firm</th>
                <th style={thStyle}>Status</th>
                <th style={{ ...thStyle, textAlign: "right" }}>Grand Total</th>
              </tr>
            </thead>
            <tbody>
              {this.state.poList.length === 0 && (
                <tr><td colSpan={7} style={{ padding: 16, textAlign: "center", color: "#999" }}>No POs</td></tr>
              )}
              {this.state.poList.map((r, i) => (
                <tr key={i} style={{ borderBottom: "1px solid #eef1f6" }}>
                  <td style={tdStyle}>
                    <span onClick={() => this.openDetail(r.purchaseOrderId)}
                      style={{ color: "#5e81f4", cursor: "pointer", fontWeight: 600, textDecoration: "underline" }}
                      title="View PO details">
                      {r.purchaseOrderId}
                    </span>
                    {r.specialPo ? <span style={{ marginLeft: 4, fontSize: 10, color: "#8e44ad" }}>★</span> : null}
                  </td>
                  <td style={tdStyle}>{r.poDate}</td>
                  <td style={tdStyle}>{r.project}</td>
                  <td style={tdStyle}>{r.supplierName}</td>
                  <td style={tdStyle}>{r.firmName}</td>
                  <td style={tdStyle}>{r.status}</td>
                  <td style={{ ...tdStyle, textAlign: "right", fontWeight: 700, color: bucketColor(r.grandTotal, bands) }}>{fmtFull(r.grandTotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  renderFilters() {
    const f = this.state.filters;
    return (
      <div style={{ background: "#fff", border: "1px solid #e6e9ef", borderRadius: 8, padding: 12, marginBottom: 16, display: "flex", flexWrap: "wrap", gap: 10, alignItems: "flex-end" }}>
        {field("From", <input type="date" value={f.startDate} onChange={(e) => this.setFilter("startDate", e.target.value)} style={inputStyle} />)}
        {field("To", <input type="date" value={f.endDate} onChange={(e) => this.setFilter("endDate", e.target.value)} style={inputStyle} />)}
        {field("Project", selectEl(f.project, this.state.projects, (v) => this.setFilter("project", v)))}
        {field("Supplier", selectEl(f.supplier, this.state.suppliers, (v) => this.setFilter("supplier", v)))}
        {field("Firm", selectEl(f.firm, this.state.firms, (v) => this.setFilter("firm", v)))}
        {field("Status", selectEl(f.status, STATUSES, (v) => this.setFilter("status", v)))}
        {field("Min ₹", <input type="number" value={f.minValue} onChange={(e) => this.setFilter("minValue", e.target.value)} style={{ ...inputStyle, width: 90 }} />)}
        {field("Max ₹", <input type="number" value={f.maxValue} onChange={(e) => this.setFilter("maxValue", e.target.value)} style={{ ...inputStyle, width: 90 }} />)}
        <button onClick={() => this.fetchAll()} style={{ padding: "7px 16px", borderRadius: 4, border: "1px solid #5e81f4", background: "#5e81f4", color: "#fff", cursor: "pointer" }}>Apply</button>
        <button onClick={this.resetFilters} style={{ padding: "7px 16px", borderRadius: 4, border: "1px solid #cfd6e0", background: "#fff", color: "#555", cursor: "pointer" }}>Reset</button>
      </div>
    );
  }

  renderDetailModal() {
    const po = this.state.detailPo;
    if (!po) return null;
    const loading = this.state.detailLoading;
    const lines = Array.isArray(po.lines) ? po.lines : [];
    const kv = (label, value) => (
      <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <span style={{ fontSize: 11, color: "#8a94a6" }}>{label}</span>
        <span style={{ fontSize: 13, color: "#323c47", fontWeight: 600 }}>{value || "-"}</span>
      </div>
    );
    const poDate = po.poDate || "-";
    return (
      <div onClick={this.closeDetail}
        style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.45)", zIndex: 1300, display: "flex", justifyContent: "center", alignItems: "flex-start", overflowY: "auto", padding: "40px 16px" }}>
        <div onClick={(e) => e.stopPropagation()}
          style={{ background: "#fff", borderRadius: 8, width: "min(820px, 100%)", boxShadow: "0 8px 30px rgba(0,0,0,0.2)" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 18px", borderBottom: "1px solid #e6e9ef" }}>
            <div style={{ fontWeight: 700, color: "#323c47" }}>Purchase Order · {po.purchaseOrderId}</div>
            <button onClick={this.closeDetail} style={{ border: "none", background: "transparent", fontSize: 20, cursor: "pointer", color: "#888", lineHeight: 1 }}>×</button>
          </div>
          {loading ? (
            <div style={{ padding: 30, textAlign: "center", color: "#888" }}>Loading…</div>
          ) : (
            <div style={{ padding: 18 }}>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 14, marginBottom: 18 }}>
                {kv("PO Date", poDate)}
                {kv("Status", po.status)}
                {kv("Project", po.projectName || "Unassigned")}
                {kv("Supplier", po.supplier?.name)}
                {kv("Firm", po.firm?.firmName)}
                {kv("Subject", po.subject)}
              </div>
              <div style={{ overflowX: "auto", border: "1px solid #eef1f6", borderRadius: 6 }}>
                <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
                  <thead>
                    <tr style={{ background: "#f5f7fa", textAlign: "left", color: "#323c47" }}>
                      <th style={thStyle}>Product</th>
                      <th style={thStyle}>Unit</th>
                      <th style={{ ...thStyle, textAlign: "right" }}>Qty</th>
                      <th style={{ ...thStyle, textAlign: "right" }}>Received</th>
                      <th style={{ ...thStyle, textAlign: "right" }}>Pending</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lines.length === 0 && (
                      <tr><td colSpan={5} style={{ padding: 14, textAlign: "center", color: "#999" }}>No line items</td></tr>
                    )}
                    {lines.map((item, i) => (
                      <tr key={i} style={{ borderBottom: "1px solid #eef1f6" }}>
                        <td style={tdStyle}>
                          {item.product?.productName || "-"}
                          {item.product?.productCode && <div style={{ fontSize: 10, color: "#999" }}>{item.product.productCode}</div>}
                        </td>
                        <td style={tdStyle}>{item.measurementUnit || "-"}</td>
                        <td style={{ ...tdStyle, textAlign: "right" }}>{item.quantity != null ? Number(item.quantity).toFixed(2) : "-"}</td>
                        <td style={{ ...tdStyle, textAlign: "right" }}>{item.quantityReceived != null ? Number(item.quantityReceived).toFixed(2) : "-"}</td>
                        <td style={{ ...tdStyle, textAlign: "right" }}>{item.quantityPending != null ? Number(item.quantityPending).toFixed(2) : "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}>
                <div style={{ textAlign: "right" }}>
                  <div style={{ fontSize: 11, color: "#8a94a6" }}>Grand Total</div>
                  <div style={{ fontSize: 22, fontWeight: 700, color: "#323c47" }}>{fmtFull(po.grandTotal)}</div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  render() {
    return (
      <div style={{ padding: "16px 20px" }}>
        {this.renderDetailModal()}
        <h2 style={{ margin: "0 0 4px", color: "#323c47" }}>Procurement Spend Dashboard</h2>
        <p style={{ margin: "0 0 10px", color: "#8a94a6", fontSize: 13 }}>
          PO value = grand total (incl. freight &amp; GST). Cancelled POs excluded. POs with no project shown as “Unassigned”.
        </p>
        {this.renderFilters()}
        {this.state.loading && <div style={{ fontSize: 13, color: "#888", marginBottom: 8 }}>Loading…</div>}
        {this.renderTiles()}
        {this.renderTrend()}
        <div style={{ display: "flex", flexWrap: "wrap", gap: 16, marginBottom: 20 }}>
          {this.renderBreakdown("Spend by Project", this.state.byProject)}
          {this.renderBreakdown("Spend by Supplier", this.state.bySupplier)}
          {this.renderBreakdown("Spend by Firm", this.state.byFirm)}
        </div>
        {this.renderList()}
      </div>
    );
  }
}

const thStyle = { padding: "8px 10px", borderBottom: "2px solid #e6e9ef", whiteSpace: "nowrap" };
const tdStyle = { padding: "7px 10px", color: "#333", whiteSpace: "nowrap" };
const inputStyle = { padding: "6px 8px", border: "1px solid #cfd6e0", borderRadius: 4, fontSize: 12 };
const field = (label, el) => (
  <div style={{ display: "flex", flexDirection: "column", gap: 3 }}>
    <label style={{ fontSize: 11, color: "#8a94a6" }}>{label}</label>
    {el}
  </div>
);
const selectEl = (value, options, onChange) => (
  <select value={value} onChange={(e) => onChange(e.target.value)} style={{ ...inputStyle, minWidth: 130 }}>
    <option value="">All</option>
    {(options || []).map((o, i) => <option key={i} value={o}>{o}</option>)}
  </select>
);

export default withSnackbar(ProcurementSpend);
