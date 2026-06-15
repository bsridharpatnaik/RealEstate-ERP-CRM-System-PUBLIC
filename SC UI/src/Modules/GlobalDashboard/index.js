import React, { Component } from "react";
import { connect } from "react-redux";
import {
  FormControl,
  FormControlLabel,
  Radio,
  RadioGroup,
} from "@material-ui/core";
import * as am4core from "@amcharts/amcharts4/core";
import Chart from "../../Shared/Chart";
import SemiPieChart from "../../Shared/Chart/semiPieChart";
import IndentTrendChart from "../../Shared/Chart/IndentTrendChart";
import SupplierLeadTimeHeatmap from "../../Shared/Chart/SupplierLeadTimeHeatmap";
import StaleHorizontalBarChart, { buildHorizontalStaleData, StaleChartLegend } from "../../Shared/Chart/StaleHorizontalBarChart";
import moment from "moment";
import { API } from "../../axios";
import { apiEndpoints, appRoutes } from "../../endpoints";
import { constants } from "../../messages";
import { setSession } from "../../helper";
import Skeleton from "@material-ui/lab/Skeleton";
import IconButton from "@material-ui/core/IconButton";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import NavigateBeforeIcon from "@material-ui/icons/NavigateBefore";
import InfoIcon from "@material-ui/icons/Info";
import "./style.scss";

const STOCK_PIE_COLORS = [
  "#f9b571",
  "#a2a2c3",
  "#FF808B",
  "#5e81f4",
  "#44c4a1",
  "#4D4CAC",
  "#E8A87C",
  "#85CDCA",
];

const SLICE_DATA_KEYS = [
  "newIndents",
  "approvedIndents",
  "poCompletedIndents",
  "closedIndents",
  "cancelledIndents",
  "poCreated",
  "poCompleted",
  "poShortClosed",
  "poCancelled",
];

const LIVE_DATA_KEYS = [
  "awaitingApprovalIndents",
  "zeroPOIndents",
  "partialPOIndents",
  "inwardPartialIndents",
  "statusNewPO",
  "statusPartialPO",
];

const METRIC_LABELS = {
  newIndents: "New Indents",
  approvedIndents: "Approved Indents",
  poCompletedIndents: "PO Completed Indents",
  closedIndents: "Closed Indents",
  poCreated: "PO Created",
  poCompleted: "PO Completed",
  poShortClosed: "PO Short Closed",
  cancelledIndents: "Cancelled Indents",
  poCancelled: "PO Cancelled",
  awaitingApprovalIndents: "Awaiting Approval Indents",
  zeroPOIndents: "Indents pending for PO",
  partialPOIndents: "Partial PO Indents",
  inwardPartialIndents: "Inward Partial Indents",
  statusNewPO: "Status New PO",
  statusPartialPO: "Status Partial PO",
};

/** Slice metric key -> status value for Indent (path: /globalIndent). */
const INDENT_SLICE_STATUS = {
  newIndents: "NEW",
  approvedIndents: "APPROVED",
  poCompletedIndents: "PO COMPLETED",
  closedIndents: "CLOSED",
  cancelledIndents: "CANCELLED",
};
/** Slice metric key -> status value for PO (path: purchaseOrder). */
const PO_SLICE_STATUS = {
  poCreated: "NEW",
  poCompleted: "COMPLETED",
  poShortClosed: "SHORT CLOSED",
  poCancelled: "CANCELLED",
};
/** Live metric key -> preset { path, filterData } (filterData array). */
const LIVE_PRESETS = {
  awaitingApprovalIndents: {
    path: "/globalIndent",
    filterData: [{ attrName: "indentStatus", attrValue: ["NEW"] }],
  },
  zeroPOIndents: {
    path: "/globalIndent",
    filterData: [{ attrName: "indentStatus", attrValue: ["APPROVED"] }],
  },
  partialPOIndents: {
    path: "/globalIndent",
    filterData: [{ attrName: "indentStatus", attrValue: ["PO PARTIAL"] }],
  },
  inwardPartialIndents: {
    path: "/globalIndent",
    filterData: [{ attrName: "indentStatus", attrValue: ["INWARD PARTIAL"] }],
  },
  statusNewPO: {
    path: appRoutes.purchaseOrder,
    filterData: [{ attrName: "status", attrValue: ["NEW"] }],
  },
  statusPartialPO: {
    path: appRoutes.purchaseOrder,
    filterData: [{ attrName: "status", attrValue: ["PARTIAL"] }],
  },
};

const height = 35;

class GlobalDashboard extends Component {
  format = constants.dateFormat;
  state = {
    selectedDate: moment(),
    selected: SLICE_DATA_KEYS[0],
    selectedText: METRIC_LABELS[SLICE_DATA_KEYS[0]],
    isLoaded: false,
    isChartLoaded: false,
    chartData: [],
    navigationType: "monthly",
    productsList: [],
    selectedProduct: null,
    isProductsLoaded: false,
    indentTrendData: null,
    isIndentTrendLoaded: false,
    poTrendData: null,
    isPOTrendLoaded: false,
    suppliersHeatmapData: [],
    isSuppliersHeatmapLoaded: false,
    overdueLines: [],
    overdueLinesTotalPages: 0,
    overdueLinesTotalElements: 0,
    overdueLinesCurPage: 0,
    isOverdueLinesLoaded: false,
    indentStaleBuckets: [],
    poStaleBuckets: [],
    isStaleLoaded: false,
    isMobile: window.innerWidth < 768,
  };

  data = {};

  getStartOfMonth(date = moment()) {
    return date.clone().startOf("month");
  }
  getEndOfMonth(date = moment()) {
    return date.clone().endOf("month");
  }
  getStartOfWeek(date = moment()) {
    return date.clone().startOf("week");
  }
  getEndOfWeek(date = moment()) {
    return date.clone().endOf("week");
  }

  handleResize = () => {
    this.setState({ isMobile: window.innerWidth < 768 });
  };

  async componentDidMount() {
    window.addEventListener("resize", this.handleResize);
    await this.fetchCharts(
      this.getStartOfMonth(moment()),
      this.getEndOfMonth(moment())
    );
    this.fetchProductsStock();
    this.fetchIndentTrend();
    this.fetchPOTrend();
    this.fetchSuppliersHeatmap();
    this.fetchOverdueLines(0);
    this.fetchStaleCharts();
  }

  componentWillUnmount() {
    window.removeEventListener("resize", this.handleResize);
  }

  /**
   * Build chart data for stale-bucket charts.
   * @param {Array}    buckets    - Raw bucket objects from API.
   * @param {string}   countsKey  - Key inside each bucket holding a { key: count } map.
   * @param {Function} [keyLabel] - Optional fn(originalKey) -> displayLabel. Used to
   *                                resolve tenant codes to tenant names for indent stale.
   */
  buildStaleChartData(buckets = [], countsKey, keyLabel) {
    if (!Array.isArray(buckets) || !buckets.length) {
      return { data: [], seriesKeys: [] };
    }
    const nameSet = new Set();
    buckets.forEach((bucket) => {
      const counts = bucket[countsKey] || {};
      Object.keys(counts).forEach((name) => nameSet.add(name));
    });
    const originalKeys = Array.from(nameSet);

    if (!originalKeys.length) {
      return { data: [], seriesKeys: [] };
    }

    // Map original key -> display label (resolves tenant codes when keyLabel provided)
    const keyMap = {};
    originalKeys.forEach((k) => { keyMap[k] = keyLabel ? keyLabel(k) : k; });
    const seriesKeys = originalKeys.map((k) => keyMap[k]);

    const STALE_BUCKET_LABELS = {
      GT_3_DAYS: "GT 3 Days",
      GT_7_DAYS: "GT 7 Days",
      GT_15_DAYS: "GT 15 Days",
      GT_30_DAYS: "GT 30 Days",
    };

    const data = buckets.map((bucket) => {
      const counts = bucket[countsKey] || {};
      const row = {
        bucket: bucket.bucket,
        bucketLabel: STALE_BUCKET_LABELS[bucket.bucket] || bucket.bucket,
      };
      originalKeys.forEach((k) => {
        row[keyMap[k]] = counts[k] || 0;
      });
      return row;
    });

    return { data, seriesKeys };
  }

  async fetchSuppliersHeatmap() {
    this.setState({ isSuppliersHeatmapLoaded: false });
    const response = await API.GET(apiEndpoints.getGlobalDashboardSuppliersLeadTimeHeatmap);
    if (response.success && Array.isArray(response.data)) {
      this.setState({
        suppliersHeatmapData: response.data,
        isSuppliersHeatmapLoaded: true,
      });
    } else {
      this.setState({ isSuppliersHeatmapLoaded: true });
    }
  }

  async fetchOverdueLines(page = 0) {
    this.setState({ isOverdueLinesLoaded: false });
    const response = await API.GET(apiEndpoints.getOverduePOLines(page, 5));
    if (response.success && response.data) {
      this.setState({
        overdueLines: response.data.content || [],
        overdueLinesTotalPages: response.data.totalPages || 0,
        overdueLinesTotalElements: response.data.totalElements || 0,
        overdueLinesCurPage: page,
        isOverdueLinesLoaded: true,
      });
    } else {
      this.setState({ isOverdueLinesLoaded: true });
    }
  }

  async fetchStaleCharts() {
    this.setState({ isStaleLoaded: false });
    const response = await API.GET(apiEndpoints.getGlobalDashboardStaleCharts);
    if (response.success && response.data) {
      const { indentStaleBuckets = [], poStaleBuckets = [] } = response.data;
      this.setState({
        indentStaleBuckets,
        poStaleBuckets,
        isStaleLoaded: true,
      });
    } else {
      this.setState({ isStaleLoaded: true });
    }
  }

  async fetchIndentTrend() {
    this.setState({ isIndentTrendLoaded: false });
    const response = await API.GET(apiEndpoints.getGlobalDashboardIndentTrend);
    if (response.success && response.data) {
      this.setState({
        indentTrendData: response.data,
        isIndentTrendLoaded: true,
      });
    } else {
      this.setState({ isIndentTrendLoaded: true });
    }
  }

  async fetchPOTrend() {
    this.setState({ isPOTrendLoaded: false });
    const response = await API.GET(apiEndpoints.getGlobalDashboardPOTrend);
    if (response.success && response.data) {
      this.setState({
        poTrendData: response.data,
        isPOTrendLoaded: true,
      });
    } else {
      this.setState({ isPOTrendLoaded: true });
    }
  }

  async fetchProductsStock() {
    this.setState({ isProductsLoaded: false });
    const response = await API.GET(apiEndpoints.getGlobalDashboardProductsStock);
    if (response.success && Array.isArray(response.data)) {
      const productsList = response.data
        .sort((a, b) => (a.productName || "").localeCompare(b.productName || ""))
        .slice(0, 20);
      this.setState({
        productsList,
        selectedProduct: productsList[0] || null,
        isProductsLoaded: true,
      });
    } else {
      this.setState({ isProductsLoaded: true });
    }
  }

  /** Resolve a tenant schema/code to its display name. */
  resolveTenantName(code) {
    const allTenant = this.props.allTenant || [];
    const found = allTenant.find((t) => t.tenantCode === code);
    return found ? found.tenantName : code;
  }

  getStockChartData(product) {
    if (!product || !product.tenantWiseStock || !product.tenantWiseStock.length)
      return [];
    return product.tenantWiseStock.map(({ tenantSchema, quantity }, i) => ({
      leadStatus: this.resolveTenantName(tenantSchema),
      value: Number(quantity),
      color: am4core.color(STOCK_PIE_COLORS[i % STOCK_PIE_COLORS.length]),
    }));
  }

  async fetchCharts(startDate, endDate) {
    this.setState({ isLoaded: false, isChartLoaded: false });
    const startStr = startDate.format(this.format);
    const endStr = endDate.format(this.format);
    const url = apiEndpoints.getGlobalDashboardCharts(startStr, endStr);
    const response = await API.GET(url);
    if (response.success) {
      this.data = response.data || {};
      this.setChartData(this.state.selected);
      this.setState({ isLoaded: true });
    } else {
      this.setState({ isLoaded: true, isChartLoaded: true });
    }
  }

  setChartData(selection) {
    const item = this.data[selection];
    const chartData = (item?.tenantCounts || []).map(({ tenant, count }) => ({
      userName: this.resolveTenantName(tenant),
      value: count,
    }));
    this.setState({ chartData, isChartLoaded: true });
  }

  handleNavigationChange(navigationType) {
    this.setState({ navigationType });
    const date = this.state.selectedDate;
    switch (navigationType) {
      case "daily":
        this.fetchCharts(date, date);
        break;
      case "weekly":
        this.fetchCharts(this.getStartOfWeek(date), this.getEndOfWeek(date));
        break;
      case "monthly":
        this.fetchCharts(this.getStartOfMonth(date), this.getEndOfMonth(date));
        break;
      default:
        break;
    }
  }

  handlePrev() {
    let date = this.state.selectedDate.clone();
    switch (this.state.navigationType) {
      case "daily":
        date = date.add(-1, "d");
        break;
      case "weekly":
        date = date.add(-1, "w");
        break;
      case "monthly":
        date = date.add(-1, "M");
        break;
      default:
        break;
    }
    this.setState({ selectedDate: date });
    switch (this.state.navigationType) {
      case "daily":
        this.fetchCharts(date, date);
        break;
      case "weekly":
        this.fetchCharts(this.getStartOfWeek(date), this.getEndOfWeek(date));
        break;
      case "monthly":
        this.fetchCharts(this.getStartOfMonth(date), this.getEndOfMonth(date));
        break;
      default:
        break;
    }
  }

  handleNext() {
    let date = this.state.selectedDate.clone();
    switch (this.state.navigationType) {
      case "daily":
        date = date.add(1, "d");
        break;
      case "weekly":
        date = date.add(1, "w");
        break;
      case "monthly":
        date = date.add(1, "M");
        break;
      default:
        break;
    }
    this.setState({ selectedDate: date });
    switch (this.state.navigationType) {
      case "daily":
        this.fetchCharts(date, date);
        break;
      case "weekly":
        this.fetchCharts(this.getStartOfWeek(date), this.getEndOfWeek(date));
        break;
      case "monthly":
        this.fetchCharts(this.getStartOfMonth(date), this.getEndOfMonth(date));
        break;
      default:
        break;
    }
  }

  onSelectMetric(name, title) {
    this.setState({
      selected: name,
      selectedText: title,
      isChartLoaded: false,
    });
    this.setChartData(name);
  }

  /** Returns { path, filterData } for the metric key; filterData is API-format array. */
  getPresetForMetric(key) {
    const live = LIVE_PRESETS[key];
    if (live) return live;
    const indentStatus = INDENT_SLICE_STATUS[key];
    if (indentStatus) {
      const date = this.state.selectedDate;
      const nav = this.state.navigationType;
      let start, end;
      if (nav === "daily") {
        start = end = date.format(this.format);
      } else if (nav === "weekly") {
        start = this.getStartOfWeek(date).format(this.format);
        end = this.getEndOfWeek(date).format(this.format);
      } else {
        start = this.getStartOfMonth(date).format(this.format);
        end = this.getEndOfMonth(date).format(this.format);
      }
      return {
        path: "/globalIndent",
        filterData: [
          { attrName: "statusChangedTo", attrValue: [indentStatus] },
          { attrName: "statusChangedAfterDate", attrValue: [start] },
          { attrName: "statusChangedBeforeDate", attrValue: [end] },
        ],
      };
    }
    const poStatus = PO_SLICE_STATUS[key];
    if (poStatus) {
      const date = this.state.selectedDate;
      const nav = this.state.navigationType;
      let start, end;
      if (nav === "daily") {
        start = end = date.format(this.format);
      } else if (nav === "weekly") {
        start = this.getStartOfWeek(date).format(this.format);
        end = this.getEndOfWeek(date).format(this.format);
      } else {
        start = this.getStartOfMonth(date).format(this.format);
        end = this.getEndOfMonth(date).format(this.format);
      }
      return {
        path: appRoutes.purchaseOrder,
        filterData: [
          { attrName: "statusChangedTo", attrValue: [poStatus] },
          { attrName: "statusChangedAfterDate", attrValue: [start] },
          { attrName: "statusChangedBeforeDate", attrValue: [end] },
        ],
      };
    }
    return null;
  }

  onInfoClick = (key, e) => {
    if (e) e.stopPropagation();
    const preset = this.getPresetForMetric(key);
    if (!preset || !this.props.history) return;
    const sessionKey = preset.path === "/globalIndent" ? "indentPresetFilterData" : "poPresetFilterData";
    setSession(sessionKey, preset.filterData);
    this.props.history.push(preset.path);
  };

  renderLoader() {
    return (
      <React.Fragment>
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
      </React.Fragment>
    );
  }

  renderMetricList(keys) {
    const URGENT_KEYS = ["awaitingApprovalIndents", "zeroPOIndents", "partialPOIndents"];
    return keys.map((key) => {
          const item = this.data[key];
          const count = item?.totalCount ?? 0;
          const title = METRIC_LABELS[key] || key;
          const isActive = this.state.selected === key;
          const isUrgent = URGENT_KEYS.includes(key);
          const classname =
            "pipeline-detail" + (isActive ? " active" : "") + (isUrgent ? " urgent" : "");
          return (
            <div key={key} className={classname}>
              <div
                className="header"
                onClick={() => this.onSelectMetric(key, title)}
              >
                {title}
              </div>
              <div className="count">{count}</div>
              <div className="infoIcon">
                <IconButton
                  aria-label="info"
                  disabled={!count}
                  onClick={(e) => count && this.onInfoClick(key, e)}
                >
                  <InfoIcon fontSize="small" />
                </IconButton>
              </div>
              <div className="selected"></div>
            </div>
          );
    });
  }

  renderPipeline() {
    return (
      <div className="leftPanel">
        <div className="pipeline">
          <span className="heading">Slice Data</span>
          {this.renderMetricList(SLICE_DATA_KEYS)}
        </div>
        <hr />
        <div className="activities">
          <span className="heading">Live Data</span>
          {this.renderMetricList(LIVE_DATA_KEYS)}
        </div>
      </div>
    );
  }

  renderStockTable() {
    const { productsList, isProductsLoaded, isMobile } = this.state;

    // Derive all unique tenant schemas from data (preserves insertion order)
    const tenantSet = new Set();
    productsList.forEach((p) => {
      (p.tenantWiseStock || []).forEach((t) => tenantSet.add(t.tenantSchema));
    });
    const tenants = Array.from(tenantSet);

    const getTenantStock = (product, schema) => {
      const found = (product.tenantWiseStock || []).find(
        (t) => t.tenantSchema === schema
      );
      return found || { quantity: 0, deadStock: 0, reorderLevel: null };
    };

    // Mobile: show only totals (sum across all tenants)
    if (isMobile) {
      return (
        <div className="notification stock-table-card">
          <div className="dashboard-heading customer-pipeline">Stocks</div>
          {!isProductsLoaded ? (
            this.renderLoader()
          ) : !productsList.length ? (
            <div className="notification-content empty-table">No products</div>
          ) : (
            <div className="stock-table-scroll-wrapper">
              <table className="stock-tenant-table">
                <thead>
                  <tr>
                    <th className="sticky-col col-name">Product</th>
                    <th className="sticky-col col-unit">Unit</th>
                    <th style={{ textAlign: "right", whiteSpace: "nowrap" }}>Total Stock</th>
                    <th style={{ textAlign: "right", whiteSpace: "nowrap" }}>Dead Stock</th>
                  </tr>
                </thead>
                <tbody>
                  {productsList.map((row) => {
                    const totalStock = (row.tenantWiseStock || []).reduce((sum, t) => sum + (t.quantity || 0), 0);
                    const totalDead  = (row.tenantWiseStock || []).reduce((sum, t) => sum + (t.deadStock || 0), 0);
                    const belowReorder = row.reorderLevel != null && totalStock <= row.reorderLevel;
                    return (
                      <tr key={row.productId} className={belowReorder ? "row-below-reorder" : ""}>
                        <td className="sticky-col col-name">
                          {row.productName}
                          {belowReorder && (
                            <span className="reorder-alert-badge" title="Stock at or below reorder level">⚠</span>
                          )}
                        </td>
                        <td className="sticky-col col-unit">{row.measurementUnit || "-"}</td>
                        <td className="stock-val" style={{ textAlign: "right" }}>{totalStock}</td>
                        <td className="dead-stock-val" style={{ textAlign: "right" }}>{totalDead}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      );
    }

    // Desktop: full tenant-wise table
    return (
      <div className="notification stock-table-card">
        <div className="dashboard-heading customer-pipeline">Stocks</div>
        {!isProductsLoaded ? (
          this.renderLoader()
        ) : !productsList.length ? (
          <div className="notification-content empty-table">No products</div>
        ) : (
          <div className="stock-table-scroll-wrapper">
            <table className="stock-tenant-table">
              <thead>
                {/* Row 1: fixed cols + tenant group headers */}
                <tr>
                  <th rowSpan={2} className="sticky-col col-name">Product Name</th>
                  <th rowSpan={2} className="sticky-col col-unit">Unit</th>
                  <th rowSpan={2} className="sticky-col col-reorder">Reorder Level</th>
                  {tenants.map((tenant) => (
                    <th key={tenant} colSpan={2} className="tenant-group-header">
                      {this.resolveTenantName(tenant)}
                    </th>
                  ))}
                </tr>
                {/* Row 2: Total Stock / Dead Stock per tenant */}
                <tr>
                  {tenants.map((tenant) => (
                    <React.Fragment key={tenant}>
                      <th className="sub-header">Total Stock</th>
                      <th className="sub-header">Dead Stock</th>
                    </React.Fragment>
                  ))}
                </tr>
              </thead>
              <tbody>
                {productsList.map((row) => {
                  const anyTenantBelowReorder = (row.tenantWiseStock || []).some(
                    (t) => t.reorderLevel != null && t.quantity != null && t.quantity <= t.reorderLevel
                  );
                  return (
                    <tr key={row.productId} className={anyTenantBelowReorder ? "row-below-reorder" : ""}>
                      <td className="sticky-col col-name">
                        {row.productName}
                        {anyTenantBelowReorder && (
                          <span className="reorder-alert-badge" title="Stock at or below reorder level in one or more projects">⚠</span>
                        )}
                      </td>
                      <td className="sticky-col col-unit">{row.measurementUnit || "-"}</td>
                      <td className="sticky-col col-reorder">
                        {row.reorderLevel != null ? (
                          <span>
                            {row.reorderLevel}
                            {row.reorderOverridden && (
                              <span className="override-badge" title="One or more projects have a custom reorder level">*</span>
                            )}
                          </span>
                        ) : "—"}
                      </td>
                      {tenants.map((tenant) => {
                        const s = getTenantStock(row, tenant);
                        const tenantBelowReorder =
                          s.reorderLevel != null && s.quantity != null && s.quantity <= s.reorderLevel;
                        return (
                          <React.Fragment key={tenant}>
                            <td className={`stock-val${tenantBelowReorder ? " tenant-below-reorder" : ""}`}>
                              {s.quantity ?? 0}
                              {tenantBelowReorder && (
                                <span className="reorder-alert-badge" title={`Stock at or below reorder level (${s.reorderLevel})`}>⚠</span>
                              )}
                            </td>
                            <td className="dead-stock-val">{s.deadStock ?? 0}</td>
                          </React.Fragment>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }

  renderIndentTrend() {
    const { indentTrendData, isIndentTrendLoaded } = this.state;
    if (!isIndentTrendLoaded) {
      return (
        <div className="indent-trend-card">
          <div className="dashboard-heading indent-trend-title">Indent Lifecycle Trend</div>
          {this.renderLoader()}
        </div>
      );
    }
    if (!indentTrendData || !indentTrendData.periods || !indentTrendData.series) {
      return (
        <div className="indent-trend-card">
          <div className="dashboard-heading indent-trend-title">
            {indentTrendData?.title || "Indent Lifecycle Trend"}
          </div>
          <div className="indent-trend-empty">No trend data</div>
        </div>
      );
    }
    return (
      <div className="indent-trend-card">
        <div className="dashboard-heading indent-trend-title">
          {indentTrendData.title}
        </div>
        <div className="indent-trend-chart-wrap">
          <IndentTrendChart
            title={indentTrendData.title}
            periods={indentTrendData.periods}
            series={indentTrendData.series}
          />
        </div>
      </div>
    );
  }

  renderPOTrend() {
    const { poTrendData, isPOTrendLoaded } = this.state;
    if (!isPOTrendLoaded) {
      return (
        <div className="indent-trend-card">
          <div className="dashboard-heading indent-trend-title">PO Lifecycle Trend</div>
          {this.renderLoader()}
        </div>
      );
    }
    if (!poTrendData || !poTrendData.periods || !poTrendData.series) {
      return (
        <div className="indent-trend-card">
          <div className="dashboard-heading indent-trend-title">
            {poTrendData?.title || "PO Lifecycle Trend"}
          </div>
          <div className="indent-trend-empty">No trend data</div>
        </div>
      );
    }
    return (
      <div className="indent-trend-card">
        <div className="dashboard-heading indent-trend-title">
          {poTrendData.title}
        </div>
        <div className="indent-trend-chart-wrap">
          <IndentTrendChart
            chartId="poTrendChartDiv"
            title={poTrendData.title}
            periods={poTrendData.periods}
            series={poTrendData.series}
          />
        </div>
      </div>
    );
  }

  renderOverduePOLines() {
    const {
      overdueLines, overdueLinesTotalPages, overdueLinesTotalElements,
      overdueLinesCurPage, isOverdueLinesLoaded
    } = this.state;

    return (
      <div className="heatmap-card overdue-po-widget">
        <div className="dashboard-heading indent-trend-title">
          ⚠ Overdue PO Lines
          {overdueLinesTotalElements > 0 && (
            <span className="overdue-total-badge">{overdueLinesTotalElements}</span>
          )}
        </div>
        {!isOverdueLinesLoaded ? (
          this.renderLoader()
        ) : overdueLines.length === 0 ? (
          <div className="indent-trend-empty" style={{ color: '#27ae60' }}>✓ No overdue PO lines</div>
        ) : (
          <>
            <div className="overdue-po-table-wrap">
              <table className="overdue-po-table">
                <thead>
                  <tr>
                    <th>PO #</th>
                    <th>Project</th>
                    <th>Supplier</th>
                    <th>Product</th>
                    <th>Lead Time</th>
                    <th>Days Overdue</th>
                  </tr>
                </thead>
                <tbody>
                  {overdueLines.map((row, i) => (
                    <tr key={i}>
                      <td>{row.purchaseOrderId}</td>
                      <td>{row.projectName || '—'}</td>
                      <td>{row.supplierName || '—'}</td>
                      <td>{row.productName} <span style={{ color: '#888', fontSize: '11px' }}>({row.measurementUnit})</span></td>
                      <td>{row.leadTimeDays != null ? `${row.leadTimeDays}d` : '—'}</td>
                      <td><span className="overdue-badge">+{row.daysOverdue}d</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {overdueLinesTotalPages > 1 && (
              <div className="overdue-po-pagination">
                <button
                  className="overdue-page-btn"
                  disabled={overdueLinesCurPage === 0}
                  onClick={() => this.fetchOverdueLines(overdueLinesCurPage - 1)}
                >‹ Prev</button>
                <span className="overdue-page-info">
                  Page {overdueLinesCurPage + 1} of {overdueLinesTotalPages}
                </span>
                <button
                  className="overdue-page-btn"
                  disabled={overdueLinesCurPage >= overdueLinesTotalPages - 1}
                  onClick={() => this.fetchOverdueLines(overdueLinesCurPage + 1)}
                >Next ›</button>
              </div>
            )}
          </>
        )}
      </div>
    );
  }

  renderIndentStaleChart() {
    const { indentStaleBuckets, isStaleLoaded } = this.state;
    if (!isStaleLoaded) {
      return (
        <div className="indent-trend-card">
          <div className="dashboard-heading indent-trend-title">Stale Indents</div>
          {this.renderLoader()}
        </div>
      );
    }
    const rows = buildHorizontalStaleData(
      indentStaleBuckets,
      "tenantCounts",
      (code) => this.resolveTenantName(code)
    );
    if (!rows.length) {
      return (
        <div className="indent-trend-card">
          <div className="dashboard-heading indent-trend-title">Stale Indents</div>
          <div className="indent-trend-empty">No stale indents</div>
        </div>
      );
    }
    return (
      <div className="indent-trend-card">
        <div className="dashboard-heading indent-trend-title">Stale Indents</div>
        <StaleChartLegend clickable />
        <div className="stale-chart-scroll-wrap">
          <StaleHorizontalBarChart
            rows={rows}
            onRowClick={(row) => {
              setSession("indentPresetFilterData", [
                { attrName: "tenants", attrValue: [row.key] },
                { attrName: "staleBuckets", attrValue: [{ id: "GT_3_DAYS", name: "More than 3 days" }] },
              ]);
              this.props.history.push("/globalIndent");
            }}
          />
        </div>
      </div>
    );
  }

  renderPOStaleChart() {
    const { poStaleBuckets, isStaleLoaded } = this.state;
    if (!isStaleLoaded) {
      return (
        <div className="indent-trend-card">
          <div className="dashboard-heading indent-trend-title">Stale POs</div>
          {this.renderLoader()}
        </div>
      );
    }
    const rows = buildHorizontalStaleData(poStaleBuckets, "supplierCounts");
    if (!rows.length) {
      return (
        <div className="indent-trend-card">
          <div className="dashboard-heading indent-trend-title">Stale POs</div>
          <div className="indent-trend-empty">No stale POs</div>
        </div>
      );
    }
    return (
      <div className="indent-trend-card">
        <div className="dashboard-heading indent-trend-title">Stale POs</div>
        <StaleChartLegend clickable />
        <div className="stale-chart-scroll-wrap">
          <StaleHorizontalBarChart
            rows={rows}
            onRowClick={(row) => {
              setSession("poPresetFilterData", [
                { attrName: "suppliers", attrValue: [row.name] },
                { attrName: "staleBuckets", attrValue: [{ id: "GT_3_DAYS", name: "More than 3 days" }] },
              ]);
              this.props.history.push(appRoutes.purchaseOrder);
            }}
          />
        </div>
      </div>
    );
  }

  renderStockSnapshot() {
    const { selectedProduct, isProductsLoaded } = this.state;
    const chartData = this.getStockChartData(selectedProduct);
    if (!isProductsLoaded) {
      return (
        <div className="notification stock-snapshot">
          <div className="dashboard-heading performer">Stock Snapshot</div>
          {this.renderLoader()}
        </div>
      );
    }
    return (
      <div className="notification stock-snapshot">
        <div className="dashboard-heading performer">Stock Snapshot</div>
        <div className="stock-snapshot-chart">
          {chartData.length > 0 ? (
            <SemiPieChart
              key={selectedProduct?.productId ?? "none"}
              data={chartData}
            />
          ) : (
            <div className="stock-snapshot-empty">
              {selectedProduct
                ? `No tenant-wise stock for ${selectedProduct.productName}`
                : "Select a product"}
            </div>
          )}
        </div>
      </div>
    );
  }

  renderChart() {
    return (
      <div className="chartwrapper">
        <div className="topchart">
          <div className="leftPanel heading">{this.state.selectedText}</div>
          <div className="rightPanel">
            <FormControl className="navigation-type" component="fieldset">
              <RadioGroup
                className="radio-group"
                name="navigationType"
                value={this.state.navigationType}
                onChange={(e) =>
                  this.handleNavigationChange(e.target.value)
                }
              >
                <FormControlLabel
                  className="fullform"
                  value="daily"
                  control={<Radio color="primary" size="small" />}
                  label="Daily"
                />
                <FormControlLabel
                  className="fullform"
                  value="weekly"
                  control={<Radio color="primary" size="small" />}
                  label="Weekly"
                />
                <FormControlLabel
                  className="fullform"
                  value="monthly"
                  control={<Radio color="primary" size="small" />}
                  label="Monthly"
                />
                <FormControlLabel
                  className="shortform"
                  value="daily"
                  control={<Radio color="primary" size="small" />}
                  label="D"
                />
                <FormControlLabel
                  className="shortform"
                  value="weekly"
                  control={<Radio color="primary" size="small" />}
                  label="W"
                />
                <FormControlLabel
                  className="shortform"
                  value="monthly"
                  control={<Radio color="primary" size="small" />}
                  label="M"
                />
              </RadioGroup>
            </FormControl>
            <div className="navigation">
              {this.state.navigationType === "daily" && (
                <span className="month">
                  {this.state.selectedDate.format("DD MMMM'YY")}
                </span>
              )}
              {this.state.navigationType === "weekly" && (
                <span className="month">
                  Week {this.state.selectedDate.week()}
                </span>
              )}
              {this.state.navigationType === "monthly" && (
                <span className="month">
                  {this.state.selectedDate.format("MMMM YYYY")}
                </span>
              )}
              <IconButton aria-label="previous" onClick={() => this.handlePrev()}>
                <NavigateBeforeIcon fontSize="medium" />
              </IconButton>
              <IconButton aria-label="next" onClick={() => this.handleNext()}>
                <NavigateNextIcon fontSize="medium" />
              </IconButton>
            </div>
          </div>
        </div>
        <div className="chartDetail">
          <Chart
            key={this.state.selected}
            data={this.state.chartData}
            dashboardChart={false}
            dateX="userName"
            valueY="value"
          />
        </div>
      </div>
    );
  }

  render() {
    return (
      <div className="global-dashboard-wrapper">
        <div className="top">
          <div className="left box">
            {this.state.isLoaded ? this.renderPipeline() : this.renderLoader()}
          </div>
          <div className="right box">
            {this.state.isChartLoaded
              ? this.renderChart()
              : this.renderLoader()}
          </div>
        </div>
        <div className="bottom stock-table-row">
          {this.renderStockTable()}
        </div>
        <div className="trend-row">
          <div className="trend-col">
            {this.renderIndentTrend()}
          </div>
          <div className="trend-col">
            {this.renderPOTrend()}
          </div>
        </div>
        <div className="stale-row">
          <div className="trend-col">
            {this.renderIndentStaleChart()}
          </div>
          <div className="trend-col">
            {this.renderPOStaleChart()}
          </div>
        </div>
        <div className="heatmap-section">
          {this.renderOverduePOLines()}
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  allTenant: state.allTennant.tennants,
});

export default connect(mapStateToProps)(GlobalDashboard);
