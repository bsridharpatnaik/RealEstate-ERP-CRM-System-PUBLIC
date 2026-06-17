import React, { Component } from "react";
import { withSnackbar } from "notistack";
import Button from "@material-ui/core/Button";
import TextField from "@material-ui/core/TextField";
import Checkbox from "@material-ui/core/Checkbox";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import IconButton from "@material-ui/core/IconButton";
import DeleteIcon from "@material-ui/icons/Delete";
import AddIcon from "@material-ui/icons/Add";
import CircularProgress from "@material-ui/core/CircularProgress";
import Step1SelectIndents from "../PurchaseOrder/add/step1SelectIndents";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";

const PRESET_CRITERIA = [
  "Brand / Make",
  "Grade / Quality",
  "Delivery Lead Time (days)",
  "Expected Delivery Date",
  "Payment Terms",
  "Freight / Transportation Charges",
  "GST %",
  "Discount %",
  "Warranty / Guarantee",
  "Compliance / Certification Status",
  "After-sales Support",
  "Remarks / Additional Notes",
];

const STEPS = ["Select Indent Lines", "Review Lines", "Criteria", "Summary"];

class QuoteComparisonCreate extends Component {
  state = {
    step: 0,
    // Step 1 output
    selectedIndents: [],
    // Step 2 — review/edit selected lines
    reviewedLines: [],
    // Step 3 — criteria
    selectedPresets: new Set(),
    customCriteria: [],
    // Step 4 — header
    title: "",
    notes: "",
    comparisonDate: new Date().toISOString().substring(0, 10),
    saving: false,
  };

  // Step 1 → Step 2
  handleIndentSelectionDone = () => {
    const { selectedIndents } = this.state;
    if (!selectedIndents || selectedIndents.length === 0) {
      this.props.enqueueSnackbar("Select at least one indent line", { variant: "error" });
      return;
    }
    const reviewedLines = selectedIndents.map(ind => ({
      indentId: String(ind.actualIndentId || ind.indentNo || ""),
      indentLineId: ind.lineItemCode || ind.indentId || null,
      productId: ind.productId || null,
      productName: ind.inventoryName || "",
      unit: ind.unit || "",
      requiredQty: parseFloat(ind.quantity) || 0,
      specifications: ind.specification !== "-" ? ind.specification : "",
      needByDate: "",
    }));
    this.setState({ step: 1, reviewedLines });
  };

  handleLineChange = (idx, key) => (e) => {
    const reviewedLines = [...this.state.reviewedLines];
    reviewedLines[idx] = { ...reviewedLines[idx], [key]: e.target.value };
    this.setState({ reviewedLines });
  };

  togglePreset = (name) => {
    const s = new Set(this.state.selectedPresets);
    if (s.has(name)) s.delete(name); else s.add(name);
    this.setState({ selectedPresets: s });
  };

  addCustomCriteria = () => {
    this.setState(prev => ({ customCriteria: [...prev.customCriteria, ""] }));
  };

  handleCustomChange = (idx) => (e) => {
    const customCriteria = [...this.state.customCriteria];
    customCriteria[idx] = e.target.value;
    this.setState({ customCriteria });
  };

  removeCustom = (idx) => () => {
    const customCriteria = this.state.customCriteria.filter((_, i) => i !== idx);
    this.setState({ customCriteria });
  };

  buildCriteriaList = () => {
    const { selectedPresets, customCriteria } = this.state;
    let order = 1;
    const list = [];
    PRESET_CRITERIA.forEach(name => {
      if (selectedPresets.has(name)) {
        list.push({ criteriaName: name, criteriaType: "TEXT", isMandatory: false, displayOrder: order++ });
      }
    });
    customCriteria.forEach(name => {
      if (name.trim()) {
        list.push({ criteriaName: name.trim(), criteriaType: "TEXT", isMandatory: false, displayOrder: order++ });
      }
    });
    return list;
  };

  handleCreate = async () => {
    const { title, reviewedLines, comparisonDate, notes } = this.state;
    if (!title.trim()) {
      this.props.enqueueSnackbar("Title is required", { variant: "error" });
      return;
    }
    this.setState({ saving: true });
    try {
      const criteria = this.buildCriteriaList();
      const payload = {
        title: title.trim(),
        notes,
        comparisonDate: comparisonDate || null,
        lines: reviewedLines.map(l => ({
          indentId: l.indentId,
          indentLineId: l.indentLineId,
          productId: l.productId,
          productName: l.productName,
          unit: l.unit,
          requiredQty: parseFloat(l.requiredQty) || 0,
          specifications: l.specifications,
          needByDate: l.needByDate || null,
        })),
        criteria,
      };
      const res = await API.POST(apiEndpoints.quoteComparisonCreate, payload);
      if (!res || !res.success) {
        this.props.enqueueSnackbar(res?.errorMessage || "Error creating quote comparison", { variant: "error" });
        this.setState({ saving: false });
        return;
      }
      this.props.enqueueSnackbar("Quote comparison created", { variant: "success" });
      if (this.props.history) this.props.history.push("/quoteComparison");
    } catch (e) {
      this.props.enqueueSnackbar("Error creating quote comparison", { variant: "error" });
    }
    this.setState({ saving: false });
  };

  renderStepper() {
    const { step } = this.state;
    return (
      <div style={{ display: "flex", alignItems: "center", marginBottom: 32, gap: 0 }}>
        {STEPS.map((label, i) => (
          <React.Fragment key={i}>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", minWidth: 100 }}>
              <div style={{
                width: 32, height: 32, borderRadius: "50%", display: "flex", alignItems: "center",
                justifyContent: "center", fontWeight: 700, fontSize: 14,
                background: i < step ? "#2e7d32" : i === step ? "#1565c0" : "#e0e0e0",
                color: i <= step ? "#fff" : "#999",
              }}>{i < step ? "✓" : i + 1}</div>
              <div style={{ fontSize: 11, marginTop: 4, color: i === step ? "#1565c0" : "#666", fontWeight: i === step ? 600 : 400 }}>
                {label}
              </div>
            </div>
            {i < STEPS.length - 1 && (
              <div style={{ flex: 1, height: 2, background: i < step ? "#2e7d32" : "#e0e0e0", margin: "0 4px", marginBottom: 20 }} />
            )}
          </React.Fragment>
        ))}
      </div>
    );
  }

  renderStep0() {
    return (
      <div>
        <Step1SelectIndents
          onSelectIndents={(indents) => this.setState({ selectedIndents: indents })}
          onIndentItemsChange={(indents) => this.setState({ selectedIndents: indents })}
          enqueueSnackbar={this.props.enqueueSnackbar}
        />
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 12, marginTop: 16 }}>
          <Button variant="outlined" onClick={() => this.props.history.push("/quoteComparison")}>
            Cancel
          </Button>
          <Button variant="contained" color="primary" onClick={this.handleIndentSelectionDone}>
            Next: Review Lines
          </Button>
        </div>
      </div>
    );
  }

  renderStep1() {
    const { reviewedLines } = this.state;
    const tf = { size: "small", variant: "outlined" };
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>Review & Adjust Lines</h3>
        <p style={{ color: "#666", fontSize: 13 }}>
          Verify quantities, specifications, and need-by dates. These will be sent to suppliers for quoting.
        </p>
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
            <thead>
              <tr style={{ background: "#f5f6fa" }}>
                {["Product", "Unit", "Required Qty", "Specifications", "Need By Date", "Indent Ref"].map(h => (
                  <th key={h} style={{ padding: "10px 12px", textAlign: "left", fontWeight: 600, borderBottom: "2px solid #e0e0e0" }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {reviewedLines.map((line, idx) => (
                <tr key={idx} style={{ borderBottom: "1px solid #f0f0f0" }}>
                  <td style={{ padding: "8px 12px", fontWeight: 600 }}>{line.productName}</td>
                  <td style={{ padding: "8px 12px" }}>{line.unit}</td>
                  <td style={{ padding: "8px 12px", width: 100 }}>
                    <TextField {...tf} type="number" value={line.requiredQty}
                      onChange={this.handleLineChange(idx, "requiredQty")} style={{ width: 90 }} />
                  </td>
                  <td style={{ padding: "8px 12px", width: 200 }}>
                    <TextField {...tf} value={line.specifications}
                      onChange={this.handleLineChange(idx, "specifications")} style={{ width: 180 }} />
                  </td>
                  <td style={{ padding: "8px 12px", width: 160 }}>
                    <TextField {...tf} type="date" InputLabelProps={{ shrink: true }}
                      value={line.needByDate} onChange={this.handleLineChange(idx, "needByDate")} style={{ width: 148 }} />
                  </td>
                  <td style={{ padding: "8px 12px", fontSize: 11, color: "#888" }}>{line.indentId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div style={{ display: "flex", justifyContent: "space-between", marginTop: 24 }}>
          <Button variant="outlined" onClick={() => this.setState({ step: 0 })}>Back</Button>
          <Button variant="contained" color="primary" onClick={() => this.setState({ step: 2 })}>
            Next: Define Criteria
          </Button>
        </div>
      </div>
    );
  }

  renderStep2() {
    const { selectedPresets, customCriteria } = this.state;
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>Comparison Criteria</h3>
        <p style={{ color: "#666", fontSize: 13 }}>
          Select parameters you want to compare across supplier quotes. These become columns in the comparison matrix.
        </p>
        <div style={{ marginBottom: 24 }}>
          <div style={{ fontWeight: 600, marginBottom: 12 }}>Standard Criteria</div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 4 }}>
            {PRESET_CRITERIA.map(name => (
              <FormControlLabel
                key={name}
                control={
                  <Checkbox
                    checked={selectedPresets.has(name)}
                    onChange={() => this.togglePreset(name)}
                    color="primary"
                    size="small"
                  />
                }
                label={<span style={{ fontSize: 13 }}>{name}</span>}
              />
            ))}
          </div>
        </div>

        <div>
          <div style={{ display: "flex", alignItems: "center", marginBottom: 8, gap: 8 }}>
            <span style={{ fontWeight: 600 }}>Custom Criteria</span>
            <IconButton size="small" onClick={this.addCustomCriteria}>
              <AddIcon fontSize="small" />
            </IconButton>
          </div>
          {customCriteria.map((val, idx) => (
            <div key={idx} style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
              <TextField
                size="small" variant="outlined" placeholder="Criteria name"
                value={val} onChange={this.handleCustomChange(idx)} style={{ width: 280 }}
              />
              <IconButton size="small" onClick={this.removeCustom(idx)}>
                <DeleteIcon fontSize="small" />
              </IconButton>
            </div>
          ))}
          {customCriteria.length === 0 && (
            <div style={{ fontSize: 12, color: "#aaa" }}>No custom criteria. Click + to add.</div>
          )}
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", marginTop: 24 }}>
          <Button variant="outlined" onClick={() => this.setState({ step: 1 })}>Back</Button>
          <Button variant="contained" color="primary" onClick={() => this.setState({ step: 3 })}>
            Next: Summary
          </Button>
        </div>
      </div>
    );
  }

  renderStep3() {
    const { title, notes, comparisonDate, reviewedLines, saving } = this.state;
    const criteria = this.buildCriteriaList();
    const tf = { size: "small", variant: "outlined", style: { marginBottom: 12 } };
    return (
      <div>
        <h3 style={{ marginTop: 0 }}>Summary & Create</h3>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 24 }}>
          <TextField label="Title *" value={title} onChange={e => this.setState({ title: e.target.value })}
            fullWidth {...tf} />
          <TextField label="Comparison Date" type="date" InputLabelProps={{ shrink: true }}
            value={comparisonDate} onChange={e => this.setState({ comparisonDate: e.target.value })}
            fullWidth {...tf} />
          <TextField label="Notes" value={notes} onChange={e => this.setState({ notes: e.target.value })}
            fullWidth multiline rows={2} {...tf} style={{ ...tf.style, gridColumn: "1 / -1" }} />
        </div>

        <div style={{ display: "flex", gap: 24, marginBottom: 24 }}>
          <div style={{ flex: 1, background: "#f9f9f9", borderRadius: 8, padding: 16 }}>
            <div style={{ fontWeight: 600, marginBottom: 8 }}>Lines ({reviewedLines.length})</div>
            {reviewedLines.map((l, i) => (
              <div key={i} style={{ fontSize: 12, marginBottom: 4, color: "#555" }}>
                • {l.productName} — {l.requiredQty} {l.unit}
              </div>
            ))}
          </div>
          <div style={{ flex: 1, background: "#f9f9f9", borderRadius: 8, padding: 16 }}>
            <div style={{ fontWeight: 600, marginBottom: 8 }}>Criteria ({criteria.length})</div>
            {criteria.length === 0
              ? <div style={{ fontSize: 12, color: "#aaa" }}>None selected (basic comparison only)</div>
              : criteria.map((c, i) => (
                  <div key={i} style={{ fontSize: 12, marginBottom: 4, color: "#555" }}>• {c.criteriaName}</div>
                ))
            }
          </div>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between" }}>
          <Button variant="outlined" onClick={() => this.setState({ step: 2 })}>Back</Button>
          <Button variant="contained" color="primary" onClick={this.handleCreate} disabled={saving}>
            {saving ? <CircularProgress size={20} /> : "Create Quote Comparison"}
          </Button>
        </div>
      </div>
    );
  }

  render() {
    const { step } = this.state;
    return (
      <div style={{ padding: 24, maxWidth: step === 0 ? "100%" : 960, margin: "0 auto" }}>
        <h2 style={{ marginTop: 0, marginBottom: 24 }}>New Quote Comparison</h2>
        {this.renderStepper()}
        {step === 0 && this.renderStep0()}
        {step === 1 && this.renderStep1()}
        {step === 2 && this.renderStep2()}
        {step === 3 && this.renderStep3()}
      </div>
    );
  }
}

export default withSnackbar(QuoteComparisonCreate);
