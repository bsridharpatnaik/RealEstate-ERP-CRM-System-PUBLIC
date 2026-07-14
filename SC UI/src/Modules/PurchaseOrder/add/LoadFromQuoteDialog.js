import React, { Component } from "react";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Button from "@material-ui/core/Button";
import IconButton from "@material-ui/core/IconButton";
import ArrowBackIcon from "@material-ui/icons/ArrowBack";
import Chip from "@material-ui/core/Chip";
import CircularProgress from "@material-ui/core/CircularProgress";
import { API } from "../../../axios";
import { apiEndpoints } from "../../../endpoints";
import { getFinalizedPoGroups, buildQuotePrefill } from "../../../Shared/quoteToPo";

// Quote comparisons in these statuses can have finalized lines that haven't been turned into
// a PO yet. DRAFT/OPEN have no finalized lines; PO_COMPLETED has none left to link.
// PARTIALLY_ORDERED still has some — only PO_COMPLETED is fully done.
const ELIGIBLE_STATUSES = ["FINALIZED", "PARTIALLY_FINALIZED", "PARTIALLY_ORDERED"];

class LoadFromQuoteDialog extends Component {
  state = {
    step: "list", // "list" | "groups"
    loading: false,
    quotes: [],
    selectedQc: null,
    detail: null,
    groups: [],
  };

  componentDidUpdate(prevProps) {
    if (this.props.open && !prevProps.open) {
      this.setState({ step: "list", selectedQc: null, detail: null, groups: [] });
      this.fetchQuotes();
    }
  }

  fetchQuotes = async () => {
    this.setState({ loading: true });
    const res = await API.POST(apiEndpoints.quoteComparisonList, {});
    if (res?.success && res.data) {
      const quotes = (res.data.content || []).filter(q => ELIGIBLE_STATUSES.includes(q.status));
      this.setState({ quotes, loading: false });
    } else {
      this.setState({ quotes: [], loading: false });
    }
  };

  handleSelectQuote = async (qc) => {
    this.setState({ loading: true, selectedQc: qc });
    const res = await API.GET(apiEndpoints.quoteComparisonDetail(qc.qcId));
    if (res?.success && res.data) {
      const groups = getFinalizedPoGroups(res.data.matrix);
      this.setState({ detail: res.data, groups, step: "groups", loading: false });
    } else {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar("Failed to load quote comparison", { variant: "error" });
      this.setState({ loading: false });
    }
  };

  handleSelectGroup = (group) => {
    const { detail, selectedQc } = this.state;
    const quotePrefill = buildQuotePrefill(group, detail.lines, selectedQc.qcId, detail.header?.project);
    this.props.onSelect(quotePrefill);
  };

  renderQuoteList() {
    const { loading, quotes } = this.state;
    if (loading) return <div style={{ textAlign: "center", padding: 32 }}><CircularProgress /></div>;
    if (quotes.length === 0) {
      return (
        <div style={{ textAlign: "center", padding: 32, color: "#aaa" }}>
          No quote comparisons with finalized, un-ordered lines were found.
        </div>
      );
    }
    return (
      <div>
        {quotes.map(qc => (
          <div key={qc.qcId} onClick={() => this.handleSelectQuote(qc)} style={{
            display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12,
            padding: "12px 14px", borderBottom: "1px solid #f0f0f0", cursor: "pointer",
          }}>
            <div>
              <div style={{ fontWeight: 600, fontSize: 13 }}>{qc.qcId} — {qc.title}</div>
              <div style={{ fontSize: 12, color: "#888" }}>
                {qc.project ? `${qc.project} · ` : ""}{(qc.lines || []).length} line{(qc.lines || []).length === 1 ? "" : "s"}
              </div>
            </div>
            <Chip label={qc.status?.replace(/_/g, " ")} size="small" />
          </div>
        ))}
      </div>
    );
  }

  renderGroups() {
    const { loading, groups, selectedQc } = this.state;
    if (loading) return <div style={{ textAlign: "center", padding: 32 }}><CircularProgress /></div>;
    if (groups.length === 0) {
      return (
        <div style={{ textAlign: "center", padding: 32, color: "#aaa" }}>
          All finalized lines in {selectedQc?.qcId} are already linked to a Purchase Order.
        </div>
      );
    }
    return (
      <div>
        {groups.map(g => (
          <div key={g.supplierQuoteId} style={{
            display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12,
            padding: "12px 14px", border: "1px solid #e0e0e0", borderRadius: 8, marginBottom: 8,
          }}>
            <div>
              <div style={{ fontWeight: 600, fontSize: 13 }}>
                {g.supplierName}{g.revisionLabel && <span style={{ color: "#888" }}> ({g.revisionLabel})</span>}
              </div>
              <div style={{ fontSize: 12, color: "#888" }}>{g.lines.length} item{g.lines.length === 1 ? "" : "s"} finalized</div>
            </div>
            <Button size="small" variant="contained" color="primary" onClick={() => this.handleSelectGroup(g)}>
              Use This Quote
            </Button>
          </div>
        ))}
      </div>
    );
  }

  render() {
    const { open, onClose } = this.props;
    const { step, selectedQc } = this.state;
    return (
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {step === "groups" && (
            <IconButton size="small" onClick={() => this.setState({ step: "list" })} style={{ marginRight: 8 }}>
              <ArrowBackIcon fontSize="small" />
            </IconButton>
          )}
          {step === "list" ? "Load from Quote Comparison" : `Select Vendor — ${selectedQc?.qcId}`}
        </DialogTitle>
        <DialogContent dividers style={{ padding: 0, minHeight: 200 }}>
          {step === "list" ? this.renderQuoteList() : this.renderGroups()}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
        </DialogActions>
      </Dialog>
    );
  }
}

export default LoadFromQuoteDialog;
