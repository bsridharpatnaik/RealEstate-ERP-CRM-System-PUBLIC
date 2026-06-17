import React, { Component } from "react";
import TextField from "@material-ui/core/TextField";
import Button from "@material-ui/core/Button";
import IconButton from "@material-ui/core/IconButton";
import DeleteIcon from "@material-ui/icons/Delete";
import CircularProgress from "@material-ui/core/CircularProgress";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";

const PRESET_CRITERIA = [
  "Unit Rate", "Quoted Quantity", "Brand", "Grade/Make", "Delivery Lead Time (days)",
  "Expected Delivery Date", "Payment Terms", "Freight Charges", "GST %",
  "Discount %", "Warranty", "Compliance Status", "Remarks"
];

class SupplierQuoteForm extends Component {
  // props: qcId, criteria (ComparisonCriteria[]), lines (QuoteComparisonLine[]),
  //        existingQuote (for edit), onSave, onCancel

  constructor(props) {
    super(props);
    const eq = props.existingQuote;
    this.state = {
      supplierId: eq?.supplierId || "",
      supplierName: eq?.supplierName || "",
      quotationRefNo: eq?.quotationRefNo || "",
      quotationDate: eq?.quotationDate ? eq.quotationDate.substring(0, 10) : "",
      validityDate: eq?.validityDate ? eq.validityDate.substring(0, 10) : "",
      paymentTerms: eq?.paymentTerms || "",
      freightTerms: eq?.freightTerms || "",
      deliveryLeadDays: eq?.deliveryLeadDays || "",
      headerNotes: eq?.headerNotes || "",
      saving: false,
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
          criteriaValues: (props.criteria || []).map(c => {
            const existingVal = existing?.criteriaValues?.find(cv => cv.criteriaId === c.id);
            return { criteriaId: c.id, criteriaName: c.criteriaName, value: existingVal?.value || "" };
          }),
        };
      }),
    };
  }

  handleHeaderChange = (key) => (e) => this.setState({ [key]: e.target.value });

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
        quotationRefNo: this.state.quotationRefNo,
        quotationDate: this.state.quotationDate || null,
        validityDate: this.state.validityDate || null,
        paymentTerms: this.state.paymentTerms,
        freightTerms: this.state.freightTerms,
        deliveryLeadDays: this.state.deliveryLeadDays || null,
        headerNotes: this.state.headerNotes,
        fileInformations: [],
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
    const { lines, saving } = this.state;
    const tf = { size: "small", variant: "outlined", style: { marginBottom: 8 } };

    return (
      <div style={{ padding: 24, maxWidth: 900 }}>
        <h3 style={{ marginTop: 0 }}>{this.props.existingQuote ? "Edit Supplier Quote" : "Add Supplier Quote"}</h3>

        {/* Supplier header */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 16 }}>
          <TextField label="Supplier Name *" value={this.state.supplierName}
            onChange={this.handleHeaderChange("supplierName")} fullWidth {...tf} />
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
                <div style={{ fontSize: 12, fontWeight: 600, color: "#666", marginBottom: 8 }}>Comparison Criteria</div>
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
