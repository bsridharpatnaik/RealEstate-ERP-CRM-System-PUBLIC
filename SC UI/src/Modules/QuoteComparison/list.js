import React, { Component } from "react";
import { withSnackbar } from "notistack";
import Button from "@material-ui/core/Button";
import Chip from "@material-ui/core/Chip";
import CircularProgress from "@material-ui/core/CircularProgress";
import Drawer from "@material-ui/core/Drawer";
import IconButton from "@material-ui/core/IconButton";
import FilterListIcon from "@material-ui/icons/FilterList";
import AddIcon from "@material-ui/icons/Add";
import { API } from "../../axios";
import { apiEndpoints, appRoutes } from "../../endpoints";
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

class QuoteComparisonList extends Component {
  state = {
    data: [],
    totalRecords: 0,
    page: 0,
    pageSize: 100,
    isLoading: false,
    filterOpen: false,
    filterData: {},
    selectedQcId: null,
    detailsOpen: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    this.setState({ isLoading: true });
    try {
      const { page, pageSize, filterData } = this.state;
      const res = await API.POST(
        `${apiEndpoints.quoteComparisonList}&page=${page}`,
        filterData
      );
      if (res?.success && res.data) {
        this.setState({
          data: res.data.content || [],
          totalRecords: res.data.totalElements || 0,
        });
      } else if (!res?.success) {
        this.props.enqueueSnackbar(res?.errorMessage || "Error loading quote comparisons", { variant: "error" });
      }
    } catch (e) {
      this.props.enqueueSnackbar("Error loading quote comparisons", { variant: "error" });
    }
    this.setState({ isLoading: false });
  };

  handleFilterApply = (filterData) => {
    this.setState({ filterData, filterOpen: false, page: 0 }, this.fetchData);
  };

  openDetails = (qcId) => this.setState({ selectedQcId: qcId, detailsOpen: true });
  closeDetails = () => this.setState({ selectedQcId: null, detailsOpen: false }, this.fetchData);

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
    const { data, isLoading, filterOpen, detailsOpen, selectedQcId, totalRecords } = this.state;

    return (
      <div style={{ padding: 24 }}>
        {/* Header */}
        <div style={{ display: "flex", alignItems: "center", marginBottom: 16, gap: 12 }}>
          <h2 style={{ flex: 1, margin: 0 }}>Quote Comparison</h2>
          <IconButton onClick={() => this.setState({ filterOpen: true })}>
            <FilterListIcon />
          </IconButton>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={this.navigateToCreate}
          >
            New Comparison
          </Button>
        </div>

        {/* Summary */}
        <div style={{ marginBottom: 12, color: "#666", fontSize: 13 }}>
          {totalRecords} record{totalRecords !== 1 ? "s" : ""}
        </div>

        {/* Table */}
        {isLoading ? (
          <div style={{ textAlign: "center", padding: 40 }}><CircularProgress /></div>
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
                    <td style={{ padding: "10px 12px" }}>{row.project}</td>
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

        {/* Filter Drawer */}
        <Drawer anchor="right" open={filterOpen} onClose={() => this.setState({ filterOpen: false })}>
          <Filter onApply={this.handleFilterApply} />
        </Drawer>

        {/* Details Slide Panel */}
        {detailsOpen && selectedQcId && (
          <div style={{
            position: "fixed", top: 0, right: 0, bottom: 0, width: "80%", maxWidth: 1200,
            background: "#fff", boxShadow: "-4px 0 24px rgba(0,0,0,0.15)", zIndex: 1200, overflowY: "auto"
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
