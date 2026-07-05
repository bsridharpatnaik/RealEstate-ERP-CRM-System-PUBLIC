//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";
import TextField from "@material-ui/core/TextField";
import InputAdornment from "@material-ui/core/InputAdornment";
import SearchIcon from "@material-ui/icons/Search";
import AddIcon from "@material-ui/icons/Add";
import Popper from "@material-ui/core/Popper";
import moment from "moment";

//component
import Filter from "./filter";
import Details from "./details";
//misc
import { messages } from "./../../messages";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import DetailsPopup from "./../../Shared/DetailsPopup";
import Button from "@material-ui/core/Button";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { canEditInventoryModules } from "./../../helper";

const STATUS_COLORS = {
  NEW: { background: "#e3f2fd", color: "#1565c0" },
  PARTIALLY_COMPLETED: { background: "#fff8e1", color: "#f57f17" },
  COMPLETED: { background: "#e8f5e9", color: "#2e7d32" },
  CANCELLED: { background: "#ffebee", color: "#c62828" },
};

class List extends ListCommon {
  title = messages.common.serviceOrder || "Service Order";
  filterData = {};
  searchValue = "";
  searchTimeout = null;
  state = {
    pageno: 0,
    data: [],
    filterOptions: {},
    filterOpen: false,
    filterAnchorEl: null,
    showDetails: false,
    selectedData: null,
    tiles: {},
    activeTile: null,
  };

  componentDidMount() {
    this.filterRef = React.createRef();
    this.fetchDropdownOptions();
    this.fetchTiles();
    this.search();
  }

  fetchTiles = async () => {
    const response = await API.GET(apiEndpoints.getServiceOrderTiles);
    if (response.success) {
      this.setState({ tiles: response.data || {} });
    }
  };

  TILES = [
    { key: "overdue", label: "Overdue", color: "#c62828", bg: "#fdedec", countKey: "overdueCount" },
    { key: "next7", label: "Due in 7 Days", color: "#e67e22", bg: "#fdf2e9", countKey: "next7DaysCount" },
    { key: "next30", label: "Due in 30 Days", color: "#f39c12", bg: "#fef9e7", countKey: "next30DaysCount" },
    { key: "next90", label: "Due in 90 Days", color: "#27ae60", bg: "#eafaf1", countKey: "next90DaysCount" },
  ];

  fmtDate = (d) => {
    const dd = String(d.getDate()).padStart(2, "0");
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    return `${dd}-${mm}-${d.getFullYear()}`;
  };

  handleTileClick = (tile) => {
    const isActive = this.state.activeTile === tile.key;
    delete this.filterData.nextServiceFrom;
    delete this.filterData.nextServiceTo;
    delete this.filterData.nextServiceOverdue;

    if (isActive) {
      this.setState({ activeTile: null }, () => this.search(0));
      return;
    }

    if (tile.key === "overdue") {
      this.filterData.nextServiceOverdue = "true";
    } else {
      const days = { next7: 7, next30: 30, next90: 90 }[tile.key];
      const from = new Date();
      const to = new Date();
      to.setDate(to.getDate() + days);
      this.filterData.nextServiceFrom = this.fmtDate(from);
      this.filterData.nextServiceTo = this.fmtDate(to);
    }
    this.setState({ activeTile: tile.key }, () => this.search(0));
  };

  renderTiles() {
    const { tiles, activeTile } = this.state;
    return (
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", padding: "12px 0" }}>
        {this.TILES.map((t) => {
          const count = tiles[t.countKey] || 0;
          const isActive = activeTile === t.key;
          return (
            <div
              key={t.key}
              onClick={() => this.handleTileClick(t)}
              style={{
                cursor: "pointer",
                minWidth: 140,
                padding: "10px 16px",
                borderRadius: 8,
                background: t.bg,
                border: isActive ? `2px solid ${t.color}` : "2px solid transparent",
                boxShadow: isActive ? "0 1px 4px rgba(0,0,0,0.15)" : "none",
              }}
            >
              <div style={{ fontSize: 22, fontWeight: 700, color: t.color }}>{count}</div>
              <div style={{ fontSize: 12, color: "#555", marginTop: 2 }}>{t.label}</div>
            </div>
          );
        })}
      </div>
    );
  }

  componentWillUnmount() {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
  }

  async fetchDropdownOptions() {
    try {
      const [vendorsRes, tenantsRes, descriptionsRes] = await Promise.all([
        API.GET(apiEndpoints.getSupplierNames),
        API.GET(apiEndpoints.getTenants),
        API.GET(apiEndpoints.getServiceOrderLineDescriptions),
      ]);
      const filterOptions = {};
      if (vendorsRes.success && Array.isArray(vendorsRes.data)) {
        filterOptions.vendors = vendorsRes.data.map((v) => ({ id: v.id, name: (v.name || "").trim() }));
      }
      if (tenantsRes.success && Array.isArray(tenantsRes.data)) {
        filterOptions.projects = tenantsRes.data
          .filter((t) => t.inventory === true)
          .map((t) => { const name = t.name || t.tenantName || ""; return { name, id: name }; })
          .filter((p) => p.name);
      }
      if (descriptionsRes.success && Array.isArray(descriptionsRes.data)) {
        filterOptions.descriptions = descriptionsRes.data;
      }
      this.setState({ filterOptions });
    } catch (e) {
      // non-critical
    }
  }

  handleSearchChange = (value) => {
    this.searchValue = value;
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => this.search(0), 500);
  };

  prepareRequestBody() {
    const params = { filterData: [] };
    if (this.searchValue && this.searchValue.trim().length) {
      params.filterData.push({ attrName: "globalSearch", attrValue: [this.searchValue.trim()] });
    }
    if (this.filterData.startDate) {
      params.filterData.push({ attrName: "startDate", attrValue: [this.filterData.startDate] });
    }
    if (this.filterData.endDate) {
      params.filterData.push({ attrName: "endDate", attrValue: [this.filterData.endDate] });
    }
    const statusArr = Array.isArray(this.filterData.status) ? this.filterData.status : [];
    if (statusArr.length > 0) {
      params.filterData.push({ attrName: "status", attrValue: statusArr.map((s) => s.id || s.name) });
    }
    const vendorsArr = Array.isArray(this.filterData.vendors) ? this.filterData.vendors : [];
    if (vendorsArr.length > 0) {
      params.filterData.push({ attrName: "vendors", attrValue: vendorsArr.map((v) => v.name) });
    }
    const projectsArr = Array.isArray(this.filterData.projectNames) ? this.filterData.projectNames : [];
    if (projectsArr.length > 0) {
      params.filterData.push({ attrName: "projectNames", attrValue: projectsArr.map((p) => p.id || p.name) });
    }
    const descriptionsArr = Array.isArray(this.filterData.descriptions) ? this.filterData.descriptions : [];
    if (descriptionsArr.length > 0) {
      params.filterData.push({ attrName: "descriptions", attrValue: descriptionsArr });
    }
    if (this.filterData.nextServiceFrom) {
      params.filterData.push({ attrName: "nextServiceFrom", attrValue: [this.filterData.nextServiceFrom] });
    }
    if (this.filterData.nextServiceTo) {
      params.filterData.push({ attrName: "nextServiceTo", attrValue: [this.filterData.nextServiceTo] });
    }
    if (this.filterData.nextServiceOverdue) {
      params.filterData.push({ attrName: "nextServiceOverdue", attrValue: [this.filterData.nextServiceOverdue] });
    }
    return params;
  }

  async search(page = 0, sortkey = null, sortby = null) {
    this.page = page;
    if (sortkey !== null) this.sortkey = sortkey;
    if (sortby !== null) this.sortby = sortby;
    if (this.inputRef && this.inputRef.current) this.inputRef.current.value = page + 1;

    const params = this.prepareRequestBody();
    let sortParam = "";
    if (this.sortkey) {
      sortParam = "&sort=" + this.sortkey;
      if (this.sortby) sortParam += "," + this.sortby;
    }

    this.setState({ isLoading: true });
    const response = await API.POST(
      apiEndpoints.getServiceOrder + "?size=" + this.pageSize + "&page=" + page + sortParam,
      params
    );
    this.setState({ isLoading: false, showDetails: false });
    this.showToaster(response);

    if (response.success) {
      const content = response.data?.serviceOrders?.content || [];
      this.setState({
        data: content,
        pages: response.data?.serviceOrders?.totalPages || 0,
        totalRecords: response.data?.serviceOrders?.totalElements || 0,
      });
    }
  }

  showDetail = async (row) => {
    this.setState({ showDetails: true, selectedData: row });
    try {
      const response = await API.GET(apiEndpoints.getServiceOrderDetail(row.serviceOrderId));
      if (response.success && response.data) {
        this.setState((prev) =>
          prev.selectedData && prev.selectedData.serviceOrderId === row.serviceOrderId
            ? { selectedData: { ...prev.selectedData, ...response.data } }
            : null
        );
      }
    } catch (e) {
      // non-fatal
    }
  };

  handleCloseDetails = () => this.setState({ showDetails: false, selectedData: null });

  render() {
    const { data } = this.state;
    return (
      <div className="service-order-list-wrapper">
        <div className="list-section">
          {this.renderTiles()}
          <div className="filter-section">
            <TextField
              variant="outlined"
              placeholder={messages.common.searchByName}
              defaultValue={this.searchValue}
              onChange={(e) => this.handleSearchChange(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon style={{ color: "rgba(108,108,108,0.6)" }} />
                  </InputAdornment>
                ),
              }}
              style={{ marginBottom: 0 }}
            />
            <div className="top-button-wrapper">
              <IconButtons
                onClick={(e) => this.setState({ filterOpen: true, filterAnchorEl: e.currentTarget })}
                buttonClass="filterIcon"
                innerRef={this.filterRef}
                label={messages.common.filter}
                icon={"MenuSVG"}
              />
              {canEditInventoryModules() && (
                <Button
                  onClick={this.props.onAdd}
                  color="primary"
                  variant="contained"
                  startIcon={<AddIcon />}
                  classes={{ root: "add-button", label: "add-label" }}
                >
                  Add Service Order
                </Button>
              )}
            </div>
            <Popper open={this.state.filterOpen} anchorEl={this.state.filterAnchorEl} placement="bottom-end">
              <Filter
                filterData={this.filterData}
                options={this.state.filterOptions}
                search={(d) => { this.filterData = d; this.search(); }}
                close={() => this.setState({ filterOpen: false, filterAnchorEl: null })}
              />
            </Popper>
          </div>

          {this.state.isLoading ? (
            this.renderLoader()
          ) : (
            <table className="generic-table" style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ borderBottom: "2px solid #e0e0e0", textAlign: "left" }}>
                  <th style={{ padding: "10px 8px" }}>SO Number</th>
                  <th style={{ padding: "10px 8px" }}>Service Date</th>
                  <th style={{ padding: "10px 8px" }}>Vendor</th>
                  <th style={{ padding: "10px 8px" }}>Project</th>
                  <th style={{ padding: "10px 8px" }}>Subject</th>
                  <th style={{ padding: "10px 8px" }}>Service Type(s)</th>
                  <th style={{ padding: "10px 8px" }}>Lines</th>
                  <th style={{ padding: "10px 8px", textAlign: "right" }}>Grand Total</th>
                  <th style={{ padding: "10px 8px" }}>Next Service</th>
                  <th style={{ padding: "10px 8px" }}>Status</th>
                  <th style={{ padding: "10px 8px" }}>Created By</th>
                </tr>
              </thead>
              <tbody>
                {data.length === 0 ? (
                  <tr><td colSpan={11} style={{ padding: 24, textAlign: "center", color: "#888" }}>No service orders found</td></tr>
                ) : (
                  data.map((row) => {
                    const statusStyle = STATUS_COLORS[row.status] || {};
                    const serviceTypes = [...new Set((row.lines || []).map((l) => l.serviceType).filter(Boolean))];
                    const isOverdue = row.nextServiceDate &&
                      row.status !== "CANCELLED" &&
                      moment(row.nextServiceDate, "DD-MM-YYYY").isBefore(moment(), "day");
                    return (
                      <tr
                        key={row.serviceOrderId}
                        onClick={() => this.showDetail(row)}
                        style={{ borderBottom: "1px solid #f0f0f0", cursor: "pointer" }}
                      >
                        <td style={{ padding: "10px 8px", fontWeight: 600 }}>{row.serviceOrderId}</td>
                        <td style={{ padding: "10px 8px" }}>{row.serviceDate}</td>
                        <td style={{ padding: "10px 8px" }}>{row.vendor?.name || "-"}</td>
                        <td style={{ padding: "10px 8px" }}>{row.projectName || "-"}</td>
                        <td style={{ padding: "10px 8px" }}>{row.subject || "-"}</td>
                        <td style={{ padding: "10px 8px" }}>
                          {serviceTypes.length > 0 ? serviceTypes.join(", ") : <span style={{ color: "#bbb" }}>-</span>}
                        </td>
                        <td style={{ padding: "10px 8px" }}>{row.lines ? row.lines.length : 0}</td>
                        <td style={{ padding: "10px 8px", textAlign: "right" }}>
                          {row.grandTotal != null ? `₹ ${Number(row.grandTotal).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : "-"}
                        </td>
                        <td style={{ padding: "10px 8px" }}>
                          {row.nextServiceDate ? (
                            <span style={isOverdue ? { color: "#c62828", fontWeight: 600 } : {}}>
                              {row.nextServiceDate}{isOverdue ? " (overdue)" : ""}
                            </span>
                          ) : (
                            <span style={{ color: "#bbb" }}>-</span>
                          )}
                        </td>
                        <td style={{ padding: "10px 8px" }}>
                          <span style={{ ...statusStyle, padding: "3px 10px", borderRadius: 12, fontSize: 12, fontWeight: 600 }}>
                            {(row.status || "").replace(/_/g, " ")}
                          </span>
                        </td>
                        <td style={{ padding: "10px 8px" }}>{row.createdBy || "-"}</td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          )}
          {this.renderPagination()}
        </div>
        <DetailsPopup open={this.state.showDetails} onClose={this.handleCloseDetails}>
          {this.state.selectedData && (
            <Details
              data={this.state.selectedData}
              close={this.handleCloseDetails}
              onRefresh={() => this.search()}
              edit={this.props.edit}
            />
          )}
        </DetailsPopup>
      </div>
    );
  }
}

export default withSnackbar(List);
