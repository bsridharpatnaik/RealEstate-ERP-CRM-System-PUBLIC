import React, { Component } from "react";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Button from "@material-ui/core/Button";
import Chip from "@material-ui/core/Chip";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";
import EditIcon from "@material-ui/icons/Edit";
import DeleteIcon from "@material-ui/icons/Delete";
import AddIcon from "@material-ui/icons/Add";
import AttachFileIcon from "@material-ui/icons/AttachFile";
import CircularProgress from "@material-ui/core/CircularProgress";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogContentText from "@material-ui/core/DialogContentText";
import DialogActions from "@material-ui/core/DialogActions";
import TextField from "@material-ui/core/TextField";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";
import SupplierQuoteForm from "./supplierQuoteForm";
import ComparisonMatrix from "./comparisonMatrix";
import { getRole } from "../../helper";
import { getFinalizedPoGroups as getFinalizedPoGroupsHelper, buildQuotePrefill } from "../../Shared/quoteToPo";
import AttachmentThumbnail, { isImageFile } from "../../Shared/AttachmentThumbnail";

const STATUS_COLORS = {
  DRAFT:               { bg: "#f5f5f5", color: "#666" },
  OPEN:                { bg: "#e3f2fd", color: "#1565c0" },
  PARTIALLY_FINALIZED: { bg: "#fff8e1", color: "#f57f17" },
  FINALIZED:           { bg: "#e8f5e9", color: "#2e7d32" },
  PARTIALLY_ORDERED:   { bg: "#e1f5fe", color: "#0277bd" },
  PO_COMPLETED:        { bg: "#e0f2f1", color: "#00695c" },
  CLOSED:              { bg: "#ede7f6", color: "#4527a0" },
  CANCELLED:           { bg: "#ffebee", color: "#b71c1c" },
};

const LINE_STATUS_COLORS = {
  OPEN:      "#1565c0",
  FINALIZED: "#2e7d32",
  PO_LINKED: "#4527a0",
  CLOSED:    "#666",
};

class QuoteComparisonDetails extends Component {
  state = {
    tab: 0,
    detail: null,
    isLoading: true,
    addingQuote: false,
    editingQuote: null,
    finalizeDialog: null, // { lineId, supplierQuoteLineId, lowestRate, line }
    finalizeRemarks: "",
    closingDialog: false,
    cancellingDialog: false,
    reopenDialog: null, // { lineId }
    reopenRemarks: "",
    saving: false,
  };

  componentDidMount() {
    this.loadDetail();
  }

  loadDetail = async () => {
    this.setState({ isLoading: true });
    const res = await API.GET(apiEndpoints.quoteComparisonDetail(this.props.qcId));
    if (res?.success && res.data) {
      this.setState({ detail: res.data });
    } else if (!res?.success) {
      this.props.enqueueSnackbar(res?.errorMessage || "Error loading detail", { variant: "error" });
    }
    this.setState({ isLoading: false });
  };

  get header() { return this.state.detail?.header || {}; }
  get lines() { return this.state.detail?.lines || []; }
  get supplierQuotes() { return this.state.detail?.supplierQuotes || []; }
  get matrix() { return this.state.detail?.matrix || []; }
  get criteria() { return this.header?.criteria || []; }
  get supplierSummary() { return this.state.detail?.supplierSummary || []; }

  canManage = () => {
    const r = getRole()?.toLowerCase();
    return r === "admin" || r === "purchase-manager";
  };

  handleDeleteQuote = async (sqId) => {
    const res = await API.DELETE(apiEndpoints.quoteComparisonDeleteSupplierQuote(this.props.qcId, sqId));
    if (res?.success) {
      this.props.enqueueSnackbar("Supplier quote removed", { variant: "success" });
      this.loadDetail();
    } else {
      this.props.enqueueSnackbar(res?.errorMessage || "Error removing quote", { variant: "error" });
    }
  };

  handleSelectWinner = (lineId, supplierQuoteLineId, quotedRate, line) => {
    const allRates = this.matrix
      .find(r => r.lineId === lineId)
      ?.supplierResponses.filter(sr => sr.hasResponse && sr.quotedRate != null)
      .map(sr => sr.quotedRate) || [];
    const lowestRate = Math.min(...allRates);
    this.setState({
      finalizeDialog: { lineId, supplierQuoteLineId, quotedRate, lowestRate, isNonLowest: quotedRate > lowestRate },
      finalizeRemarks: "",
    });
  };

  handleFinalizeLine = async () => {
    const { finalizeDialog, finalizeRemarks } = this.state;
    if (finalizeDialog.isNonLowest && !finalizeRemarks.trim()) {
      this.props.enqueueSnackbar("Justification is required for non-lowest selection", { variant: "error" });
      return;
    }
    this.setState({ saving: true });
    const res = await API.POST(apiEndpoints.quoteComparisonFinalizeLine(this.props.qcId), {
      qcLineId: finalizeDialog.lineId,
      supplierQuoteLineId: finalizeDialog.supplierQuoteLineId,
      remarks: finalizeRemarks,
    });
    if (res?.success) {
      this.props.enqueueSnackbar("Line finalized", { variant: "success" });
      this.setState({ finalizeDialog: null });
      this.loadDetail();
    } else {
      this.props.enqueueSnackbar(res?.errorMessage || "Error finalizing", { variant: "error" });
    }
    this.setState({ saving: false });
  };

  handleReopenLine = async () => {
    this.setState({ saving: true });
    const res = await API.POST(
      apiEndpoints.quoteComparisonReopenLine(this.props.qcId, this.state.reopenDialog.lineId),
      null,
      { params: { remarks: this.state.reopenRemarks } }
    );
    if (res?.success) {
      this.props.enqueueSnackbar("Line reopened", { variant: "success" });
      this.setState({ reopenDialog: null });
      this.loadDetail();
    } else {
      this.props.enqueueSnackbar(res?.errorMessage || "Error reopening line", { variant: "error" });
    }
    this.setState({ saving: false });
  };

  handleClose = async () => {
    this.setState({ saving: true });
    const res = await API.POST(apiEndpoints.quoteComparisonClose(this.props.qcId));
    if (res?.success) {
      this.props.enqueueSnackbar("Comparison closed", { variant: "success" });
      this.setState({ closingDialog: false });
      this.loadDetail();
    } else {
      this.props.enqueueSnackbar(res?.errorMessage || "Error closing comparison", { variant: "error" });
    }
    this.setState({ saving: false });
  };

  handleReopenComparison = async () => {
    this.setState({ saving: true });
    const res = await API.POST(apiEndpoints.quoteComparisonReopen(this.props.qcId));
    if (res?.success) {
      this.props.enqueueSnackbar("Comparison reopened", { variant: "success" });
      this.loadDetail();
    } else {
      this.props.enqueueSnackbar(res?.errorMessage || "Error reopening comparison", { variant: "error" });
    }
    this.setState({ saving: false });
  };

  handleDownloadAttachment = (file) => async () => {
    try {
      const response = await API.GET(apiEndpoints.masterFileDownload + file.fileUUId, { responseType: "blob" });
      if (response.status === 200) {
        const url = window.URL.createObjectURL(response.data);
        const a = document.createElement("a");
        a.href = url;
        a.download = file.fileName;
        a.click();
      }
    } catch (e) {
      this.props.enqueueSnackbar("Failed to download file", { variant: "error" });
    }
  };

  handleExportRfqPdf = async () => {
    try {
      const res = await API.GETBlob(apiEndpoints.quoteComparisonRfqPdf(this.props.qcId));
      if (res?.success) {
        const blobUrl = window.URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
        window.open(blobUrl, "_blank");
      } else {
        this.props.enqueueSnackbar("Failed to generate RFQ PDF", { variant: "error" });
      }
    } catch (e) {
      this.props.enqueueSnackbar("Failed to generate RFQ PDF", { variant: "error" });
    }
  };

  // Groups finalized-but-not-yet-PO'd lines by their winning supplier quote, so one PO can be
  // created per vendor round in a single click (a PO has exactly one supplier).
  getFinalizedPoGroups = () => getFinalizedPoGroupsHelper(this.matrix);

  handleCreatePo = (group) => () => {
    const quotePrefill = buildQuotePrefill(group, this.lines, this.props.qcId, this.header.project);

    if (this.props.history) {
      // Stashed in sessionStorage (not router state) because the PO route remounts its
      // component on every parent re-render — router state would get wiped before it's read.
      sessionStorage.setItem("qcPoPrefill", JSON.stringify(quotePrefill));
      this.props.history.push("/purchaseOrder");
    } else {
      this.props.enqueueSnackbar("Cannot open Purchase Order — navigation unavailable", { variant: "error" });
    }
  };

  handleCancel = async () => {
    this.setState({ saving: true });
    const res = await API.POST(apiEndpoints.quoteComparisonCancel(this.props.qcId));
    if (res?.success) {
      this.props.enqueueSnackbar("Comparison cancelled", { variant: "success" });
      this.setState({ cancellingDialog: false });
      this.loadDetail();
    } else {
      this.props.enqueueSnackbar(res?.errorMessage || "Error cancelling", { variant: "error" });
    }
    this.setState({ saving: false });
  };

  renderHeader() {
    const h = this.header;
    const s = STATUS_COLORS[h.status] || STATUS_COLORS.DRAFT;
    return (
      <div style={{ background: "#f9fafc", border: "1px solid #e0e0e0", borderRadius: 8, padding: 16, marginBottom: 16 }}>
        <div className="qc-header-row" style={{ display: "flex", alignItems: "flex-start", gap: 12 }}>
          <div style={{ flex: 1 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 8 }}>
              <span style={{ fontWeight: 700, fontSize: 18, color: "#1565c0" }}>{h.qcId}</span>
              <Chip label={h.status?.replace(/_/g, " ")} size="small"
                style={{ background: s.bg, color: s.color, fontWeight: 600 }} />
            </div>
            <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>{h.title}</div>
            <div className="qc-info-grid" style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 8, fontSize: 13 }}>
              <div><span style={{ color: "#888" }}>Project: </span>{h.project || "—"}</div>
              <div><span style={{ color: "#888" }}>Date: </span>{h.comparisonDate ? new Date(h.comparisonDate).toLocaleDateString("en-IN") : "—"}</div>
              <div><span style={{ color: "#888" }}>Created by: </span>{h.createdByUser || "—"}</div>
              <div><span style={{ color: "#888" }}>Indents: </span>{(h.indentIds || []).join(", ") || "—"}</div>
              <div><span style={{ color: "#888" }}>Lines: </span>{this.lines.length}</div>
              <div><span style={{ color: "#888" }}>Suppliers: </span>{this.supplierQuotes.length}</div>
            </div>
            {h.notes && <div style={{ marginTop: 8, fontSize: 13, color: "#555" }}>Notes: {h.notes}</div>}
          </div>

          {/* Actions */}
          <div style={{ display: "flex", gap: 8, flexShrink: 0 }}>
            <Button size="small" variant="outlined" onClick={this.handleExportRfqPdf}>
              Export RFQ PDF
            </Button>
            {this.canManage() && h.status === "CLOSED" && (
              <Button size="small" variant="outlined" color="primary" disabled={this.state.saving}
                onClick={this.handleReopenComparison}>Reopen</Button>
            )}
            {this.canManage() && h.status !== "CLOSED" && h.status !== "CANCELLED" && (
              <>
                <Button size="small" variant="outlined" color="secondary"
                  onClick={() => this.setState({ closingDialog: true })}>Close</Button>
                <Button size="small" variant="outlined" style={{ borderColor: "#d32f2f", color: "#d32f2f" }}
                  onClick={() => this.setState({ cancellingDialog: true })}>Cancel</Button>
              </>
            )}
          </div>
        </div>
      </div>
    );
  }

  renderOverviewTab() {
    const poGroups = this.canManage() ? this.getFinalizedPoGroups() : [];
    return (
      <div>
        {poGroups.length > 0 && (
          <div style={{ marginBottom: 20 }}>
            <h4>Ready for Purchase Order</h4>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
              {poGroups.map(g => (
                <div key={g.supplierQuoteId} style={{
                  display: "flex", alignItems: "center", gap: 10, padding: "8px 12px",
                  border: "1px solid #c8e6c9", background: "#f1f8e9", borderRadius: 8,
                }}>
                  <span style={{ fontSize: 13 }}>
                    <strong>{g.supplierName}</strong>
                    {g.revisionLabel && <span style={{ color: "#888" }}> ({g.revisionLabel})</span>}
                    {" — "}{g.lines.length} item{g.lines.length === 1 ? "" : "s"} finalized
                  </span>
                  <Button size="small" variant="contained" color="primary" onClick={this.handleCreatePo(g)}>
                    Create PO
                  </Button>
                </div>
              ))}
            </div>
          </div>
        )}

        <h4>Demand Lines</h4>
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead>
            <tr style={{ background: "#f5f6fa" }}>
              {["Product", "Required Qty", "Unit", "Indent", "Need By", "Status", "Finalized By", ...(this.canManage() ? ["Action"] : [])].map(h => (
                <th key={h} style={{ padding: "8px 12px", textAlign: "left", borderBottom: "2px solid #e0e0e0", fontWeight: 600 }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {this.lines.map(l => (
              <tr key={l.id} style={{ borderBottom: "1px solid #f0f0f0" }}>
                <td style={{ padding: "8px 12px", fontWeight: 600 }}>{l.productName}</td>
                <td style={{ padding: "8px 12px" }}>{l.requiredQty}</td>
                <td style={{ padding: "8px 12px" }}>{l.unit}</td>
                <td style={{ padding: "8px 12px", fontSize: 11 }}>{l.indentId || "—"}</td>
                <td style={{ padding: "8px 12px" }}>{l.needByDate ? new Date(l.needByDate).toLocaleDateString("en-IN") : "—"}</td>
                <td style={{ padding: "8px 12px" }}>
                  <span style={{ color: LINE_STATUS_COLORS[l.lineStatus] || "#333", fontWeight: 600, fontSize: 12 }}>
                    {l.lineStatus?.replace(/_/g, " ")}
                  </span>
                  {l.isNonLowestSelection && (
                    <Chip label="Non-lowest" size="small"
                      style={{ marginLeft: 4, background: "#fff3e0", color: "#e65100", fontSize: 10 }} />
                  )}
                </td>
                <td style={{ padding: "8px 12px", fontSize: 12, color: "#555" }}>{l.finalizedBy || "—"}</td>
                {this.canManage() && (
                  <td style={{ padding: "8px 12px" }}>
                    {l.lineStatus === "FINALIZED" && (
                      <Button size="small" variant="outlined"
                        onClick={() => this.setState({ reopenDialog: { lineId: l.id }, reopenRemarks: "" })}>
                        Reopen
                      </Button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>

        {(this.header.criteria || []).length > 0 && (
          <div style={{ marginTop: 20 }}>
            <h4>Comparison Criteria</h4>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
              {(this.header.criteria || []).map(c => (
                <Chip key={c.id} label={c.criteriaName} size="small" variant="outlined" />
              ))}
            </div>
          </div>
        )}
      </div>
    );
  }

  renderSupplierQuotesTab() {
    const canEdit = this.canManage() &&
      !["CLOSED", "CANCELLED"].includes(this.header.status);

    return (
      <div>
        {canEdit && (
          <Button variant="contained" color="primary" startIcon={<AddIcon />}
            onClick={() => this.setState({ addingQuote: true, editingQuote: null })}
            style={{ marginBottom: 16 }}>
            Add Supplier Quote
          </Button>
        )}

        {(this.state.addingQuote || this.state.editingQuote) && (
          <div style={{ border: "1px solid #e0e0e0", borderRadius: 8, marginBottom: 16 }}>
            <SupplierQuoteForm
              qcId={this.props.qcId}
              criteria={this.header.criteria || []}
              lines={this.lines}
              existingQuote={this.state.editingQuote}
              onSave={() => { this.setState({ addingQuote: false, editingQuote: null }); this.loadDetail(); }}
              onCancel={() => this.setState({ addingQuote: false, editingQuote: null })}
              enqueueSnackbar={this.props.enqueueSnackbar}
            />
          </div>
        )}

        {this.supplierQuotes.length === 0 ? (
          <div style={{ textAlign: "center", padding: 32, color: "#aaa" }}>
            No supplier quotes yet. Add the first one.
          </div>
        ) : this.supplierQuotes.map(sq => (
          <div key={sq.id} style={{
            border: "1px solid #e0e0e0", borderRadius: 8, padding: 16, marginBottom: 12
          }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 12 }}>
              <span style={{ fontWeight: 700, fontSize: 15 }}>{sq.supplierName}</span>
              {sq.revisionLabel && <Chip label={sq.revisionLabel} size="small"
                style={{ background: "#e3f2fd", color: "#1565c0", fontWeight: 600 }} />}
              {sq.quotationRefNo && <Chip label={sq.quotationRefNo} size="small" variant="outlined" />}
              <span style={{ fontSize: 12, color: "#888", marginLeft: "auto" }}>
                {sq.quotationDate && `Quoted: ${new Date(sq.quotationDate).toLocaleDateString("en-IN")}`}
                {sq.validityDate && ` | Valid till: ${new Date(sq.validityDate).toLocaleDateString("en-IN")}`}
              </span>
              {canEdit && (
                <>
                  <IconButton size="small" onClick={() => this.setState({ editingQuote: sq, addingQuote: false })}>
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={() => this.handleDeleteQuote(sq.id)}>
                    <DeleteIcon fontSize="small" style={{ color: "#d32f2f" }} />
                  </IconButton>
                </>
              )}
            </div>
            {sq.fileInformations && sq.fileInformations.length > 0 && (
              <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 12 }}>
                {sq.fileInformations.map((file, i) => (
                  <Chip key={i}
                    icon={isImageFile(file.fileName) ? undefined : <AttachFileIcon style={{ fontSize: 14 }} />}
                    avatar={isImageFile(file.fileName) ? <AttachmentThumbnail file={file} size={24} downloadUrl={apiEndpoints.masterFileDownload} /> : undefined}
                    label={file.fileName} size="small"
                    variant="outlined" onClick={this.handleDownloadAttachment(file)}
                    style={{ cursor: "pointer", fontSize: 11 }} />
                ))}
              </div>
            )}
            <div className="qc-info-grid" style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 8, fontSize: 12, color: "#555", marginBottom: 12 }}>
              {sq.paymentTerms && <span>Payment: {sq.paymentTerms}</span>}
              {sq.freightTerms && <span>Freight: {sq.freightTerms}</span>}
              {sq.deliveryLeadDays && <span>Lead: {sq.deliveryLeadDays}d</span>}
            </div>
            {/* Line table */}
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
              <thead>
                <tr style={{ background: "#f9f9f9" }}>
                  {["Product", "Qty", "Rate", "Discount%", "GST%", "Freight", "Landed", "Delivery"].map(h => (
                    <th key={h} style={{ padding: "6px 8px", textAlign: "left", borderBottom: "1px solid #e0e0e0" }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {(sq.lines || []).map(l => {
                  const qcLine = this.lines.find(ql => ql.id === l.qcLineId);
                  return (
                    <tr key={l.id}>
                      <td style={{ padding: "6px 8px" }}>{qcLine?.productName || l.qcLineId}</td>
                      <td style={{ padding: "6px 8px" }}>{l.quotedQty}</td>
                      <td style={{ padding: "6px 8px", fontWeight: 600 }}>₹{l.quotedRate}</td>
                      <td style={{ padding: "6px 8px" }}>{l.discountPercent}%</td>
                      <td style={{ padding: "6px 8px" }}>{l.gstPercent}%</td>
                      <td style={{ padding: "6px 8px" }}>₹{l.freightAmount}</td>
                      <td style={{ padding: "6px 8px", color: "#1565c0" }}>₹{l.landedCost?.toFixed(0)}</td>
                      <td style={{ padding: "6px 8px" }}>
                        {l.expectedDeliveryDate ? new Date(l.expectedDeliveryDate).toLocaleDateString("en-IN") : "—"}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    );
  }

  renderMatrixTab() {
    const canFinalize = this.canManage() && !["CLOSED", "CANCELLED", "FINALIZED", "PARTIALLY_ORDERED", "PO_COMPLETED"].includes(this.header.status);
    return (
      <ComparisonMatrix
        matrix={this.matrix}
        criteria={this.header.criteria || []}
        supplierSummary={this.supplierSummary}
        onSelectWinner={canFinalize ? this.handleSelectWinner : null}
        canFinalize={canFinalize}
      />
    );
  }

  renderAuditTab() {
    // Shown from activity log — placeholder, backend filters by entityId=qcId
    return (
      <div style={{ padding: 24, textAlign: "center", color: "#aaa" }}>
        Audit history is visible in the Activity Log module filtered by entity type QUOTE_COMPARISON and ID {this.props.qcId}.
      </div>
    );
  }

  render() {
    const { tab, isLoading, detail, finalizeDialog, finalizeRemarks, closingDialog, cancellingDialog, reopenDialog, saving } = this.state;

    return (
      <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
        {/* Top bar */}
        <div style={{
          display: "flex", alignItems: "center", padding: "12px 24px",
          borderBottom: "1px solid #e0e0e0", background: "#fff", flexShrink: 0
        }}>
          <span style={{ fontWeight: 700, fontSize: 16, flex: 1 }}>Quote Comparison — {this.props.qcId}</span>
          <IconButton onClick={this.props.onClose}><CloseIcon /></IconButton>
        </div>

        {isLoading ? (
          <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
            <CircularProgress />
          </div>
        ) : (
          <div style={{ flex: 1, overflowY: "auto", padding: 24 }}>
            {this.renderHeader()}

            <Tabs value={tab} onChange={(_, v) => this.setState({ tab: v })}
              indicatorColor="primary" textColor="primary" style={{ marginBottom: 16, borderBottom: "1px solid #e0e0e0" }}>
              <Tab label="Overview" />
              <Tab label="Supplier Quotes" />
              <Tab label="Comparison Matrix" />
              <Tab label="Audit" />
            </Tabs>

            {tab === 0 && this.renderOverviewTab()}
            {tab === 1 && this.renderSupplierQuotesTab()}
            {tab === 2 && this.renderMatrixTab()}
            {tab === 3 && this.renderAuditTab()}
          </div>
        )}

        {/* Finalize dialog */}
        <Dialog open={!!finalizeDialog} onClose={() => this.setState({ finalizeDialog: null })}>
          <DialogTitle>Finalize Line</DialogTitle>
          <DialogContent>
            {finalizeDialog?.isNonLowest && (
              <div style={{ background: "#fff3e0", border: "1px solid #ffb74d", borderRadius: 6,
                padding: 10, marginBottom: 12, fontSize: 13 }}>
                ⚠️ This is not the lowest quoted rate. Justification is mandatory.
              </div>
            )}
            <DialogContentText>
              Finalize this line with the selected supplier quote?
              {finalizeDialog && (
                <span style={{ display: "block", marginTop: 8, fontWeight: 600 }}>
                  Rate: ₹{finalizeDialog.quotedRate} (Lowest: ₹{finalizeDialog.lowestRate})
                </span>
              )}
            </DialogContentText>
            <TextField
              label={finalizeDialog?.isNonLowest ? "Justification *" : "Remarks (optional)"}
              value={finalizeRemarks}
              onChange={e => this.setState({ finalizeRemarks: e.target.value })}
              fullWidth multiline rows={3} variant="outlined" size="small"
              style={{ marginTop: 8 }}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ finalizeDialog: null })}>Cancel</Button>
            <Button onClick={this.handleFinalizeLine} color="primary" variant="contained" disabled={saving}>
              {saving ? <CircularProgress size={18} /> : "Finalize"}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Reopen dialog */}
        <Dialog open={!!reopenDialog} onClose={() => this.setState({ reopenDialog: null })}>
          <DialogTitle>Reopen Line</DialogTitle>
          <DialogContent>
            <DialogContentText>Reopen this finalized line for re-evaluation?</DialogContentText>
            <TextField label="Reason" value={this.state.reopenRemarks}
              onChange={e => this.setState({ reopenRemarks: e.target.value })}
              fullWidth multiline rows={2} variant="outlined" size="small" style={{ marginTop: 8 }} />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ reopenDialog: null })}>Cancel</Button>
            <Button onClick={this.handleReopenLine} color="primary" variant="contained" disabled={saving}>
              {saving ? <CircularProgress size={18} /> : "Reopen"}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Close dialog */}
        <Dialog open={closingDialog} onClose={() => this.setState({ closingDialog: false })}>
          <DialogTitle>Close Comparison</DialogTitle>
          <DialogContent>
            <DialogContentText>Mark this comparison as closed? No further edits will be allowed.</DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ closingDialog: false })}>Cancel</Button>
            <Button onClick={this.handleClose} color="primary" variant="contained" disabled={saving}>
              {saving ? <CircularProgress size={18} /> : "Close"}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Cancel dialog */}
        <Dialog open={cancellingDialog} onClose={() => this.setState({ cancellingDialog: false })}>
          <DialogTitle>Cancel Comparison</DialogTitle>
          <DialogContent>
            <DialogContentText>Cancel this comparison? This cannot be undone if lines are PO-linked.</DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ cancellingDialog: false })}>Back</Button>
            <Button onClick={this.handleCancel} variant="contained"
              style={{ background: "#d32f2f", color: "#fff" }} disabled={saving}>
              {saving ? <CircularProgress size={18} /> : "Cancel Comparison"}
            </Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}

export default QuoteComparisonDetails;
