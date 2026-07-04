import React from "react";
import { withSnackbar } from "notistack";
import Button from "@material-ui/core/Button";
import Chip from "@material-ui/core/Chip";
import Popper from "@material-ui/core/Popper";
import AddIcon from "@material-ui/icons/Add";
import ListCommon from "../../Shared/List";
import IconButtons from "../../Shared/Button/IconButtons";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";
import { messages } from "../../messages";
import Filter from "./filter";
import QuoteComparisonDetails from "./details";

const STATUS_COLORS = {
  DRAFT:                { bg: "#f5f5f5", color: "#666" },
  OPEN:                 { bg: "#e3f2fd", color: "#1565c0" },
  PARTIALLY_FINALIZED:  { bg: "#fff8e1", color: "#f57f17" },
  FINALIZED:            { bg: "#e8f5e9", color: "#2e7d32" },
  PARTIALLY_ORDERED:    { bg: "#e1f5fe", color: "#0277bd" },
  PO_COMPLETED:         { bg: "#e0f2f1", color: "#00695c" },
  CLOSED:               { bg: "#ede7f6", color: "#4527a0" },
  CANCELLED:            { bg: "#ffebee", color: "#b71c1c" },
};

class QuoteComparisonList extends ListCommon {
  filterData = {};
  url = apiEndpoints.quoteComparisonList;
  pageSize = 25;
  page = 0;

  state = {
    data: [],
    pages: 0,
    totalRecords: 0,
    isLoading: false,
    filterOpen: false,
    supplierOptions: [],
    tenantNameByCode: {},
    selectedQcId: null,
    detailsOpen: false,
  };

  componentDidMount() {
    this.filterRef = React.createRef();
    this.search();
    this.fetchSupplierNames();
    this.fetchTenants();
  }

  async fetchSupplierNames() {
    const res = await API.GET(apiEndpoints.quoteComparisonSupplierNames);
    if (res?.success && Array.isArray(res.data)) {
      this.setState({ supplierOptions: res.data });
    }
  }

  async fetchTenants() {
    const res = await API.GET(apiEndpoints.getTenants);
    if (res?.success && Array.isArray(res.data)) {
      const tenantNameByCode = {};
      res.data.forEach((t) => {
        if (t.tenantCode) tenantNameByCode[t.tenantCode] = t.tenantName || t.name || t.tenantCode;
      });
      this.setState({ tenantNameByCode });
    }
  }

  // row.project is a free-text string (comma-separated codes/names) set at creation time.
  // Resolve any segment that matches a known tenant code to its display name.
  resolveProjectDisplay = (project) => {
    if (!project) return project;
    const { tenantNameByCode } = this.state;
    return project
      .split(",")
      .map((p) => p.trim())
      .map((p) => tenantNameByCode[p] || p)
      .join(", ");
  };

  prepareRequestBody() {
    const body = {};
    for (const field in this.filterData) {
      const value = this.filterData[field];
      if (value === undefined || value === null || value === "") continue;
      if (Array.isArray(value) && value.length === 0) continue;
      body[field] = value;
    }
    return body;
  }

  search = async (page = 0) => {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);
    if (response.success) {
      const paged = response.data;
      this.setState({
        data: paged.content || [],
        pages: paged.totalPages || 0,
        totalRecords: paged.totalElements || 0,
      });
    }
  };

  openDetails = (qcId) => this.setState({ selectedQcId: qcId, detailsOpen: true });
  closeDetails = () => this.setState({ selectedQcId: null, detailsOpen: false }, () => this.search(this.page));

  navigateToCreate = () => {
    if (this.props.history) this.props.history.push("/quoteComparison/create");
  };

  renderStatusChip(status) {
    const s = STATUS_COLORS[status] || { bg: "#f5f5f5", color: "#333" };
    return (
      <Chip
        label={status?.replace(/_/g, " ")}
        size="small"
        style={{ background: s.bg, color: s.color, fontWeight: 600 }}
      />
    );
  }

  render() {
    const { data, isLoading, filterOpen, detailsOpen, selectedQcId, totalRecords, supplierOptions } = this.state;

    return (
      <div className="page">
        <div className="header-info">
          <div>
            <h2 className="page-title">Quote Comparison</h2>
          </div>
        </div>

        <div className="list-section">
          <div className="filter-section">
            <div>
              {totalRecords > 0 && (
                <span style={{ fontSize: 13, color: "#555" }}>
                  {totalRecords} record{totalRecords !== 1 ? "s" : ""}
                </span>
              )}
            </div>
            <div className="top-button-wrapper" style={{ display: "flex", gap: "8px" }}>
              <IconButtons
                onClick={() => this.setState({ filterOpen: true })}
                buttonClass="filterIcon"
                label={messages.common.filter}
                icon="FilterSVG"
                innerRef={this.filterRef}
              />
              <Button
                variant="contained"
                color="primary"
                startIcon={<AddIcon />}
                onClick={this.navigateToCreate}
              >
                New Comparison
              </Button>
            </div>
          </div>

          <Popper
            open={filterOpen}
            anchorEl={this.filterRef && this.filterRef.current}
            placement="bottom-end"
            style={{ zIndex: 1300 }}
          >
            <Filter
              filterData={this.filterData}
              options={{ suppliers: supplierOptions }}
              search={(data) => {
                this.filterData = data;
                this.setState({ filterOpen: false });
                this.search(0);
              }}
              close={() => this.setState({ filterOpen: false })}
            />
          </Popper>

          {isLoading ? (
            this.renderLoader()
          ) : (
            <div style={{ overflowX: "auto" }}>
              <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
                <thead>
                  <tr style={{ background: "#f5f6fa" }}>
                    {["QC No", "Title", "Project", "Indent(s)", "Date", "Status", "Suppliers", "Lines", "Actions"].map(h => (
                      <th key={h} style={{ padding: "10px 12px", textAlign: "left", fontWeight: 600, borderBottom: "2px solid #e0e0e0" }}>
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {data.length === 0 ? (
                    <tr><td colSpan={9} style={{ textAlign: "center", padding: 32, color: "#aaa" }}>No records found</td></tr>
                  ) : data.map(row => (
                    <tr key={row.qcId} style={{ borderBottom: "1px solid #f0f0f0" }}>
                      <td style={{ padding: "10px 12px", fontWeight: 600, color: "#1565c0" }}>{row.qcId}</td>
                      <td style={{ padding: "10px 12px" }}>{row.title}</td>
                      <td style={{ padding: "10px 12px" }}>{this.resolveProjectDisplay(row.project)}</td>
                      <td style={{ padding: "10px 12px", fontSize: 12, color: "#555" }}>
                        {(row.indentIds || []).join(", ") || "—"}
                      </td>
                      <td style={{ padding: "10px 12px" }}>{row.comparisonDate ? new Date(row.comparisonDate).toLocaleDateString("en-IN") : "—"}</td>
                      <td style={{ padding: "10px 12px" }}>{this.renderStatusChip(row.status)}</td>
                      <td style={{ padding: "10px 12px", textAlign: "center" }}>{(row.supplierQuotes || []).length}</td>
                      <td style={{ padding: "10px 12px", textAlign: "center" }}>{(row.lines || []).length}</td>
                      <td style={{ padding: "10px 12px" }}>
                        <Button size="small" variant="outlined" onClick={() => this.openDetails(row.qcId)}>
                          View
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {this.renderPagination()}
        </div>

        {/* Details — full-screen popup (uses the whole viewport, no wasted side band) */}
        {detailsOpen && selectedQcId && (
          <div style={{
            position: "fixed", top: 0, left: 0, right: 0, bottom: 0, width: "100%",
            background: "#fff", zIndex: 1300, overflowY: "auto"
          }}>
            <QuoteComparisonDetails
              qcId={selectedQcId}
              onClose={this.closeDetails}
              enqueueSnackbar={this.props.enqueueSnackbar}
              history={this.props.history}
            />
          </div>
        )}
      </div>
    );
  }
}

export default withSnackbar(QuoteComparisonList);
