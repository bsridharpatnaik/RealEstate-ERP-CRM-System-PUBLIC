import React, { Component } from "react";
import TextField from "@material-ui/core/TextField";
import Button from "@material-ui/core/Button";
import IconButton from "@material-ui/core/IconButton";
import DeleteIcon from "@material-ui/icons/Delete";
import AttachFileIcon from "@material-ui/icons/AttachFile";
import InsertDriveFileIcon from "@material-ui/icons/InsertDriveFile";
import CircularProgress from "@material-ui/core/CircularProgress";
import Autocomplete from "@material-ui/lab/Autocomplete";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";
import AttachmentThumbnail, { isImageFile } from "../../Shared/AttachmentThumbnail";

const ATTACHMENT_MAX_MB = 10;
const ATTACHMENT_VALID_TYPES = [
  "image/jpeg", "image/jpg", "image/png", "application/pdf",
  "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
];

class SupplierQuoteForm extends Component {
  // props: qcId, criteria (ComparisonCriteria[]), lines (QuoteComparisonLine[]),
  //        existingQuote (for edit), onSave, onCancel

  constructor(props) {
    super(props);
    const eq = props.existingQuote;
    const lineCriteria = (props.criteria || []).filter(c => c.criteriaScope !== "HEADER");
    const headerCriteria = (props.criteria || []).filter(c => c.criteriaScope === "HEADER");
    this.state = {
      supplierId: eq?.supplierId || "",
      supplierName: eq?.supplierName || "",
      revisionLabel: eq?.revisionLabel || "",
      quotationRefNo: eq?.quotationRefNo || "",
      quotationDate: eq?.quotationDate ? eq.quotationDate.substring(0, 10) : "",
      validityDate: eq?.validityDate ? eq.validityDate.substring(0, 10) : "",
      paymentTerms: eq?.paymentTerms || "",
      freightTerms: eq?.freightTerms || "",
      deliveryLeadDays: eq?.deliveryLeadDays || "",
      headerNotes: eq?.headerNotes || "",
      suppliers: [],
      saving: false,
      uploading: false,
      // received quote files (vendor's quotation PDF/scan/etc) — array of { fileUUId, fileName }
      attachments: eq?.fileInformations ? Array.from(eq.fileInformations).map(f => ({ fileUUId: f.fileUUId, fileName: f.fileName })) : [],
      // header-scoped criteria — one value per vendor, shown once
      headerCriteriaValues: headerCriteria.map(c => {
        const existingVal = eq?.headerCriteriaValues?.find(cv => cv.criteriaId === c.id);
        return { criteriaId: c.id, criteriaName: c.criteriaName, value: existingVal?.value || "" };
      }),
      // lines: one entry per QC line
      lines: (props.lines || []).map(l => {
        const existing = eq?.lines?.find(el => el.qcLineId === l.id);
        return {
          qcLineId: l.id,
          productName: l.productName,
          requiredQty: l.requiredQty,
          unit: l.unit,
          quotedQty: existing?.quotedQty || "",
          quotedRate: existing?.quotedRate || "",
          discountPercent: existing?.discountPercent || 0,
          gstPercent: existing?.gstPercent || 0,
          freightAmount: existing?.freightAmount || 0,
          expectedDeliveryDate: existing?.expectedDeliveryDate?.substring(0, 10) || "",
          lineRemarks: existing?.lineRemarks || "",
          criteriaValues: lineCriteria.map(c => {
            const existingVal = existing?.criteriaValues?.find(cv => cv.criteriaId === c.id);
            return { criteriaId: c.id, criteriaName: c.criteriaName, value: existingVal?.value || "" };
          }),
        };
      }),
    };
  }

  async componentDidMount() {
    const res = await API.GET(apiEndpoints.getSupplierNames);
    if (res?.success && Array.isArray(res.data)) {
      this.setState({ suppliers: res.data });
    }
  }

  handleHeaderChange = (key) => (e) => this.setState({ [key]: e.target.value });

  handleHeaderCriteriaChange = (cIdx) => (e) => {
    const headerCriteriaValues = [...this.state.headerCriteriaValues];
    headerCriteriaValues[cIdx] = { ...headerCriteriaValues[cIdx], value: e.target.value };
    this.setState({ headerCriteriaValues });
  };

  handleLineChange = (idx, key) => (e) => {
    const lines = [...this.state.lines];
    lines[idx] = { ...lines[idx], [key]: e.target.value };
    this.setState({ lines });
  };

  handleCriteriaChange = (lineIdx, cIdx) => (e) => {
    const lines = [...this.state.lines];
    const criteriaValues = [...lines[lineIdx].criteriaValues];
    criteriaValues[cIdx] = { ...criteriaValues[cIdx], value: e.target.value };
    lines[lineIdx] = { ...lines[lineIdx], criteriaValues };
    this.setState({ lines });
  };

  handleAttachmentUpload = async (e) => {
    const inputEl = e.target;
    const file = inputEl.files[0];
    if (!file) return;

    if (file.size / 1024 / 1024 > ATTACHMENT_MAX_MB) {
      this.props.enqueueSnackbar(`File upload is restricted to ${ATTACHMENT_MAX_MB}MB`, { variant: "error" });
      inputEl.value = "";
      return;
    }
    if (!ATTACHMENT_VALID_TYPES.includes(file.type)) {
      this.props.enqueueSnackbar("Only PDF, Word, Excel and image files are allowed", { variant: "error" });
      inputEl.value = "";
      return;
    }

    const formData = new FormData();
    formData.append("file", file, file.name);
    this.setState({ uploading: true });
    try {
      const response = await API.POST(apiEndpoints.masterFileUpload, formData);
      if (response.success) {
        this.setState(prev => ({
          attachments: [...prev.attachments, { fileUUId: response.data.fileUUId, fileName: file.name }],
        }));
        this.props.enqueueSnackbar("File attached", { variant: "success" });
      } else {
        this.props.enqueueSnackbar(response.errorMessage || "Upload failed", { variant: "error" });
      }
    } catch (e) {
      this.props.enqueueSnackbar("Upload failed", { variant: "error" });
    }
    inputEl.value = "";
    this.setState({ uploading: false });
  };

  handleRemoveAttachment = (idx) => () => {
    this.setState(prev => ({ attachments: prev.attachments.filter((_, i) => i !== idx) }));
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

  computeLanded = (line) => {
    const rate = parseFloat(line.quotedRate) || 0;
    const qty = parseFloat(line.quotedQty) || 0;
    const disc = parseFloat(line.discountPercent) || 0;
    const gst = parseFloat(line.gstPercent) || 0;
    const freight = parseFloat(line.freightAmount) || 0;
    const base = rate * qty;
    const discAmt = base * disc / 100;
    const gstAmt = (base - discAmt) * gst / 100;
    return (base - discAmt + gstAmt + freight).toFixed(2);
  };

  handleSave = async () => {
    const { qcId, existingQuote } = this.props;
    const { supplierName, saving, lines, quotationDate, validityDate, expectedDeliveryDate, ...rest } = this.state;
    if (!supplierName.trim()) {
      this.props.enqueueSnackbar("Supplier name is required", { variant: "error" });
      return;
    }

    this.setState({ saving: true });
    try {
      const payload = {
        supplierId: this.state.supplierId || null,
        supplierName: this.state.supplierName,
        revisionLabel: this.state.revisionLabel ? this.state.revisionLabel.trim() : null,
        quotationRefNo: this.state.quotationRefNo,
        quotationDate: this.state.quotationDate || null,
        validityDate: this.state.validityDate || null,
        paymentTerms: this.state.paymentTerms,
        freightTerms: this.state.freightTerms,
        deliveryLeadDays: this.state.deliveryLeadDays || null,
        headerNotes: this.state.headerNotes,
        fileInformations: this.state.attachments.map(a => ({ fileUUId: a.fileUUId, fileName: a.fileName })),
        headerCriteriaValues: this.state.headerCriteriaValues.filter(cv => cv.value),
        lines: lines.map(l => ({
          qcLineId: l.qcLineId,
          quotedQty: parseFloat(l.quotedQty) || null,
          quotedRate: parseFloat(l.quotedRate) || null,
          discountPercent: parseFloat(l.discountPercent) || 0,
          gstPercent: parseFloat(l.gstPercent) || 0,
          freightAmount: parseFloat(l.freightAmount) || 0,
          expectedDeliveryDate: l.expectedDeliveryDate || null,
          lineRemarks: l.lineRemarks,
          criteriaValues: l.criteriaValues.filter(cv => cv.value),
        })),
      };

      let res;
      if (existingQuote) {
        res = await API.PUT(apiEndpoints.quoteComparisonUpdateSupplierQuote(qcId, existingQuote.id), payload);
      } else {
        res = await API.POST(apiEndpoints.quoteComparisonAddSupplierQuote(qcId), payload);
      }

      if (!res || !res.success) {
        this.props.enqueueSnackbar(res?.errorMessage || "Error saving supplier quote", { variant: "error" });
        this.setState({ saving: false });
        return;
      }
      this.props.enqueueSnackbar("Supplier quote saved", { variant: "success" });
      this.props.onSave();
    } catch (e) {
      this.props.enqueueSnackbar("Error saving supplier quote", { variant: "error" });
    }
    this.setState({ saving: false });
  };

  render() {
    const { lines, saving, uploading, headerCriteriaValues, attachments } = this.state;
    const tf = { size: "small", variant: "outlined", style: { marginBottom: 8 } };

    return (
      <div style={{ padding: 24, maxWidth: 900 }}>
        <h3 style={{ marginTop: 0 }}>{this.props.existingQuote ? "Edit Supplier Quote" : "Add Supplier Quote"}</h3>

        {/* Supplier header */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 16 }}>
          <Autocomplete
            options={this.state.suppliers}
            getOptionLabel={(o) => o.name || ""}
            getOptionSelected={(o, v) => o.id === v.id}
            value={
              this.state.suppliers.find((s) => Number(s.id) === Number(this.state.supplierId)) ||
              (this.state.supplierName ? { id: this.state.supplierId || null, name: this.state.supplierName } : null)
            }
            onChange={(e, val) =>
              this.setState({ supplierId: val ? val.id : "", supplierName: val ? val.name : "" })
            }
            renderInput={(params) => (
              <TextField {...params} label="Supplier *" fullWidth {...tf} />
            )}
          />
          <TextField label="Revision" value={this.state.revisionLabel}
            onChange={this.handleHeaderChange("revisionLabel")} fullWidth {...tf}
            placeholder="e.g. R-0, R-1 — leave blank to auto-number"
            helperText="Add another quote for the same vendor to capture a negotiation round (R-0, R-1, R-2...)" />
          <TextField label="Quotation Ref No" value={this.state.quotationRefNo}
            onChange={this.handleHeaderChange("quotationRefNo")} fullWidth {...tf} />
          <TextField label="Quotation Date" type="date" InputLabelProps={{ shrink: true }}
            value={this.state.quotationDate} onChange={this.handleHeaderChange("quotationDate")} fullWidth {...tf} />
          <TextField label="Validity Date" type="date" InputLabelProps={{ shrink: true }}
            value={this.state.validityDate} onChange={this.handleHeaderChange("validityDate")} fullWidth {...tf} />
          <TextField label="Payment Terms" value={this.state.paymentTerms}
            onChange={this.handleHeaderChange("paymentTerms")} fullWidth {...tf} />
          <TextField label="Freight Terms" value={this.state.freightTerms}
            onChange={this.handleHeaderChange("freightTerms")} fullWidth {...tf} />
          <TextField label="Delivery Lead Days" type="number" value={this.state.deliveryLeadDays}
            onChange={this.handleHeaderChange("deliveryLeadDays")} fullWidth {...tf} />
          <TextField label="Header Notes" value={this.state.headerNotes}
            onChange={this.handleHeaderChange("headerNotes")} fullWidth {...tf} />
        </div>

        {/* Header-scoped criteria — one value per vendor, applies to the whole quote */}
        {headerCriteriaValues.length > 0 && (
          <div style={{ marginBottom: 16, padding: 16, background: "#f5f8ff", borderRadius: 8, border: "1px solid #dbe6fb" }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: "#1565c0", marginBottom: 8 }}>
              Vendor-Wide Criteria (applies to entire quote)
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 10 }}>
              {headerCriteriaValues.map((cv, cIdx) => (
                <TextField key={cv.criteriaId} label={cv.criteriaName} value={cv.value}
                  onChange={this.handleHeaderCriteriaChange(cIdx)} fullWidth {...tf} />
              ))}
            </div>
          </div>
        )}

        {/* Received quote attachments — the vendor's actual quotation document(s) */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
            <span style={{ fontWeight: 600, fontSize: 13 }}>Received Quote Attachments</span>
            <Button component="label" size="small" variant="outlined" startIcon={<AttachFileIcon fontSize="small" />} disabled={uploading}>
              {uploading ? "Uploading…" : "Attach File"}
              <input type="file" hidden onChange={this.handleAttachmentUpload}
                accept="image/jpeg,image/jpg,image/png,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" />
            </Button>
          </div>
          {attachments.length === 0 ? (
            <div style={{ fontSize: 12, color: "#aaa" }}>No files attached. Attach the vendor's quotation (PDF/scan/email) for reference.</div>
          ) : (
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
              {attachments.map((file, idx) => (
                <div key={idx} style={{
                  display: "flex", alignItems: "center", gap: 4, padding: "4px 8px", borderRadius: 16,
                  background: "#f0f4fa", border: "1px solid #dbe6fb", fontSize: 12,
                }}>
                  {isImageFile(file.fileName) ? (
                    <AttachmentThumbnail file={file} size={20} downloadUrl={apiEndpoints.masterFileDownload} />
                  ) : (
                    <InsertDriveFileIcon fontSize="small" style={{ color: "#1565c0" }} />
                  )}
                  <span style={{ cursor: "pointer", color: "#1565c0" }} onClick={this.handleDownloadAttachment(file)}>
                    {file.fileName}
                  </span>
                  <IconButton size="small" style={{ padding: 2 }} onClick={this.handleRemoveAttachment(idx)}>
                    <DeleteIcon style={{ fontSize: 14 }} />
                  </IconButton>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Line items */}
        <h4>Line Responses</h4>
        {lines.map((line, idx) => (
          <div key={line.qcLineId} style={{
            border: "1px solid #e0e0e0", borderRadius: 8, padding: 16, marginBottom: 16
          }}>
            <div style={{ fontWeight: 600, marginBottom: 12 }}>
              {line.productName} — Required: {line.requiredQty} {line.unit}
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 10 }}>
              <TextField label="Quoted Qty" type="number" value={line.quotedQty}
                onChange={this.handleLineChange(idx, "quotedQty")} fullWidth {...tf} />
              <TextField label="Unit Rate" type="number" value={line.quotedRate}
                onChange={this.handleLineChange(idx, "quotedRate")} fullWidth {...tf} />
              <TextField label="Discount %" type="number" value={line.discountPercent}
                onChange={this.handleLineChange(idx, "discountPercent")} fullWidth {...tf} />
              <TextField label="GST %" type="number" value={line.gstPercent}
                onChange={this.handleLineChange(idx, "gstPercent")} fullWidth {...tf} />
              <TextField label="Freight Amount" type="number" value={line.freightAmount}
                onChange={this.handleLineChange(idx, "freightAmount")} fullWidth {...tf} />
              <TextField label="Expected Delivery" type="date" InputLabelProps={{ shrink: true }}
                value={line.expectedDeliveryDate} onChange={this.handleLineChange(idx, "expectedDeliveryDate")} fullWidth {...tf} />
              <TextField label="Line Remarks" value={line.lineRemarks}
                onChange={this.handleLineChange(idx, "lineRemarks")} fullWidth {...tf} />
              <div style={{ display: "flex", alignItems: "center", fontSize: 12, color: "#555", padding: 8,
                background: "#f9f9f9", borderRadius: 4, border: "1px solid #e0e0e0" }}>
                <span>Landed Cost: <strong>₹{this.computeLanded(line)}</strong></span>
              </div>
            </div>

            {/* Criteria values */}
            {line.criteriaValues.length > 0 && (
              <div style={{ marginTop: 12 }}>
                <div style={{ fontSize: 12, fontWeight: 600, color: "#666", marginBottom: 8 }}>Per-Product Criteria</div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 10 }}>
                  {line.criteriaValues.map((cv, cIdx) => (
                    <TextField key={cv.criteriaId} label={cv.criteriaName} value={cv.value}
                      onChange={this.handleCriteriaChange(idx, cIdx)} fullWidth {...tf} />
                  ))}
                </div>
              </div>
            )}
          </div>
        ))}

        {/* Actions */}
        <div style={{ display: "flex", gap: 12, justifyContent: "flex-end" }}>
          <Button variant="outlined" onClick={this.props.onCancel} disabled={saving}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={this.handleSave} disabled={saving}>
            {saving ? <CircularProgress size={20} /> : "Save Quote"}
          </Button>
        </div>
      </div>
    );
  }
}

export default SupplierQuoteForm;
