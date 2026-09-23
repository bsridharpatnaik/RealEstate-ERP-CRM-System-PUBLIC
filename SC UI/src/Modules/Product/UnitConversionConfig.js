import React from "react";
import { withSnackbar } from "notistack";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import CircularProgress from "@material-ui/core/CircularProgress";
import IconButton from "@material-ui/core/IconButton";
import DeleteIcon from "@material-ui/icons/Delete";
import EditIcon from "@material-ui/icons/Edit";
import LockIcon from "@material-ui/icons/Lock";
import Tooltip from "@material-ui/core/Tooltip";
import Button from "./../../Shared/Button";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";

// conversion_factor is ALWAYS stored canonically as base-units-per-1-billing-unit (PO calc divides by it).
// displayDirection only controls how the number is entered/shown.
const BASE_PER_BILLING = "BASE_PER_BILLING"; // "1 {base} = value {billing}"  e.g. 1 pc = 85 kg
const BILLING_PER_BASE = "BILLING_PER_BASE"; // "1 {billing} = value {base}"  e.g. 1 bundle = 75 kg

const round6 = (x) => Math.round(x * 1e6) / 1e6;

// value the user sees/enters, derived from the stored canonical factor + direction
const shownValue = (factor, direction) =>
  direction === BASE_PER_BILLING ? round6(1 / factor) : round6(factor);

// canonical factor to store (base units per 1 of this unit), from the value typed in the chosen
// direction relative to a reference unit. refFactor = base units per 1 reference unit (1 for base).
const canonicalFactor = (value, direction, refFactor) =>
  direction === BASE_PER_BILLING ? refFactor / value : value * refFactor;

class UnitConversionConfig extends React.Component {
  state = {
    conversions: [],
    loading: true,
    saving: false,
    editingId: null, // null = add mode, otherwise the conversion id being edited
    formUnitName: "",
    formDirection: BASE_PER_BILLING,
    formValue: "",
    formReference: "", // "" = base unit, otherwise another unit's name
  };

  componentDidMount() {
    this.loadConversions();
  }

  async loadConversions() {
    this.setState({ loading: true });
    const { product } = this.props;
    const response = await API.GET(apiEndpoints.getUnitConversions(product.productId));
    if (response.success) {
      this.setState({ conversions: response.data, loading: false });
    } else {
      this.props.enqueueSnackbar("Failed to load unit conversions", { variant: "error" });
      this.setState({ loading: false });
    }
  }

  resetForm() {
    this.setState({
      editingId: null, formUnitName: "", formDirection: BASE_PER_BILLING, formValue: "", formReference: "",
    });
  }

  // base units per 1 of the given reference unit (1 for the base unit itself). null if not found.
  refFactorOf(referenceUnit) {
    if (!referenceUnit) return 1;
    const c = this.state.conversions.find(
      (x) => x.unitName === referenceUnit && x.id !== this.state.editingId
    );
    return c ? c.conversionFactor : null;
  }

  startEdit(c) {
    const direction = c.displayDirection || BILLING_PER_BASE;
    // referenceValue set = row was defined against a reference; else legacy (reconstruct vs base)
    const value = c.referenceValue != null ? c.referenceValue : shownValue(c.conversionFactor, direction);
    this.setState({
      editingId: c.id,
      formUnitName: c.unitName,
      formDirection: direction,
      formValue: String(value),
      formReference: c.referenceUnit || "",
    });
  }

  async handleSave() {
    const { formUnitName, formDirection, formValue, formReference, editingId } = this.state;
    const { product } = this.props;

    if (!formUnitName.trim()) {
      this.props.enqueueSnackbar("Billing unit name is required", { variant: "warning" });
      return;
    }
    const value = parseFloat(formValue);
    if (isNaN(value) || value <= 0) {
      this.props.enqueueSnackbar("Value must be a positive number", { variant: "warning" });
      return;
    }
    const refFactor = this.refFactorOf(formReference);
    if (refFactor == null) {
      this.props.enqueueSnackbar("Reference unit not found — pick another", { variant: "warning" });
      return;
    }

    const body = {
      unitName: formUnitName.trim(),
      conversionFactor: canonicalFactor(value, formDirection, refFactor),
      displayDirection: formDirection,
      referenceUnit: formReference || null,
      referenceValue: value,
    };

    this.setState({ saving: true });
    const response = editingId
      ? await API.PUT(apiEndpoints.updateUnitConversion(product.productId, editingId), body)
      : await API.POST(apiEndpoints.addUnitConversion(product.productId), body);
    this.setState({ saving: false });

    if (response.success) {
      this.props.enqueueSnackbar(editingId ? "Unit conversion updated" : "Unit conversion added", {
        variant: "success",
      });
      this.resetForm();
      this.loadConversions();
    } else {
      this.props.enqueueSnackbar(
        response.errorMessage || (editingId ? "Failed to update unit conversion" : "Failed to add unit conversion"),
        { variant: "error" }
      );
    }
  }

  async handleDelete(conversion) {
    const { product } = this.props;
    this.setState({ saving: true });
    const response = await API.DELETE(
      apiEndpoints.deleteUnitConversion(product.productId, conversion.id)
    );
    this.setState({ saving: false });

    if (response.success) {
      this.props.enqueueSnackbar("Unit conversion removed", { variant: "success" });
      if (this.state.editingId === conversion.id) this.resetForm();
      this.loadConversions();
    } else {
      this.props.enqueueSnackbar(
        response.errorMessage || "Failed to remove unit conversion",
        { variant: "error" }
      );
    }
  }

  render() {
    const { product, onClose } = this.props;
    const { conversions, loading, saving, editingId, formUnitName, formDirection, formValue, formReference } = this.state;
    const baseUnit = product.measurementUnit || "base unit";
    const unitLabel = formUnitName.trim() || "unit";
    const refLabel = formReference.trim() || baseUnit;
    const referenceOptions = conversions.filter((c) => c.id !== editingId);

    return (
      <Dialog open onClose={onClose} maxWidth="sm" fullWidth disableBackdropClick disableEscapeKeyDown>
        <DialogTitle>
          Billing Unit Conversions —{" "}
          <span style={{ fontWeight: 400 }}>{product.productName}</span>
          <div style={{ fontSize: 12, fontWeight: 400, color: "#777", marginTop: 2 }}>
            Base unit: <strong>{baseUnit}</strong>
          </div>
        </DialogTitle>

        <DialogContent dividers>
          {loading ? (
            <div style={{ textAlign: "center", padding: "32px 0" }}>
              <CircularProgress size={28} />
            </div>
          ) : (
            <>
              {conversions.length === 0 ? (
                <div style={{ color: "#999", marginBottom: 16 }}>
                  No alternate billing units configured.
                </div>
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse", marginBottom: 20 }}>
                  <thead>
                    <tr style={{ background: "#f5f5f5" }}>
                      <th style={thStyle}>Billing Unit</th>
                      <th style={thStyle}>Conversion</th>
                      <th style={thStyle}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {conversions.map((c) => {
                      const dir = c.displayDirection || BILLING_PER_BASE;
                      const refUnit = c.referenceValue != null ? (c.referenceUnit || baseUnit) : baseUnit;
                      const val = c.referenceValue != null ? c.referenceValue : shownValue(c.conversionFactor, dir);
                      const text =
                        dir === BASE_PER_BILLING
                          ? `1 ${refUnit} = ${val} ${c.unitName}`
                          : `1 ${c.unitName} = ${val} ${refUnit}`;
                      return (
                        <tr key={c.id} style={editingId === c.id ? { background: "#e3f2fd" } : null}>
                          <td style={tdStyle}>{c.unitName}</td>
                          <td style={tdStyle}>{text}</td>
                          <td style={{ ...tdStyle, textAlign: "center", whiteSpace: "nowrap" }}>
                            <IconButton size="small" onClick={() => this.startEdit(c)} disabled={saving}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                            {c.usedInPo ? (
                              <Tooltip title="Used in a purchase order — cannot delete">
                                <span>
                                  <LockIcon fontSize="small" style={{ color: "#aaa", verticalAlign: "middle" }} />
                                </span>
                              </Tooltip>
                            ) : (
                              <IconButton size="small" onClick={() => this.handleDelete(c)} disabled={saving}>
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}

              <div style={{ fontSize: 12, fontWeight: 600, color: "#555", marginBottom: 6 }}>
                {editingId ? "Edit billing unit" : "Add billing unit"}
              </div>

              <div style={{ marginBottom: 10 }}>
                <label style={labelStyle}>Billing Unit Name</label>
                <input
                  style={inputStyle}
                  placeholder="e.g. kg, Box, Bundle"
                  value={formUnitName}
                  onChange={(e) => this.setState({ formUnitName: e.target.value })}
                  disabled={saving}
                />
              </div>

              <div style={{ marginBottom: 10 }}>
                <label style={labelStyle}>Defined against</label>
                <select
                  style={inputStyle}
                  value={formReference}
                  onChange={(e) => this.setState({ formReference: e.target.value })}
                  disabled={saving}
                >
                  <option value="">{baseUnit} (base unit)</option>
                  {referenceOptions.map((c) => (
                    <option key={c.id} value={c.unitName}>{c.unitName}</option>
                  ))}
                </select>
              </div>

              <label style={labelStyle}>How do they relate?</label>
              <div style={{ marginBottom: 10 }}>
                <label style={radioRow}>
                  <input
                    type="radio"
                    checked={formDirection === BASE_PER_BILLING}
                    onChange={() => this.setState({ formDirection: BASE_PER_BILLING })}
                    disabled={saving}
                  />
                  <span style={{ marginLeft: 6 }}>
                    1 <strong>{refLabel}</strong> = &nbsp;
                    <input
                      style={inlineInput}
                      type="number"
                      min="0.000001"
                      step="any"
                      placeholder="e.g. 85"
                      value={formDirection === BASE_PER_BILLING ? formValue : ""}
                      onChange={(e) => this.setState({ formValue: e.target.value })}
                      onFocus={() => this.setState({ formDirection: BASE_PER_BILLING })}
                      disabled={saving}
                    />
                    &nbsp; <strong>{unitLabel}</strong>
                  </span>
                </label>
                <label style={radioRow}>
                  <input
                    type="radio"
                    checked={formDirection === BILLING_PER_BASE}
                    onChange={() => this.setState({ formDirection: BILLING_PER_BASE })}
                    disabled={saving}
                  />
                  <span style={{ marginLeft: 6 }}>
                    1 <strong>{unitLabel}</strong> = &nbsp;
                    <input
                      style={inlineInput}
                      type="number"
                      min="0.000001"
                      step="any"
                      placeholder="e.g. 12"
                      value={formDirection === BILLING_PER_BASE ? formValue : ""}
                      onChange={(e) => this.setState({ formValue: e.target.value })}
                      onFocus={() => this.setState({ formDirection: BILLING_PER_BASE })}
                      disabled={saving}
                    />
                    &nbsp; <strong>{refLabel}</strong>
                  </span>
                </label>
              </div>

              <div style={{ display: "flex", gap: 8 }}>
                <button style={addBtnStyle} onClick={() => this.handleSave()} disabled={saving}>
                  {editingId ? "Save" : "+ Add"}
                </button>
                {editingId && (
                  <button style={cancelBtnStyle} onClick={() => this.resetForm()} disabled={saving}>
                    Cancel
                  </button>
                )}
              </div>
            </>
          )}
        </DialogContent>

        <DialogActions>
          <Button onClick={onClose} buttonClass="grey" label="Close" disabled={saving} />
        </DialogActions>
      </Dialog>
    );
  }
}

const thStyle = {
  padding: "8px 12px",
  textAlign: "left",
  fontSize: 13,
  fontWeight: 600,
  borderBottom: "1px solid #e0e0e0",
};
const tdStyle = {
  padding: "8px 12px",
  fontSize: 13,
  borderBottom: "1px solid #f0f0f0",
};
const labelStyle = {
  display: "block",
  fontSize: 11,
  color: "#666",
  marginBottom: 4,
};
const inputStyle = {
  width: "100%",
  padding: "7px 10px",
  border: "1px solid #ccc",
  borderRadius: 4,
  fontSize: 13,
  boxSizing: "border-box",
};
const inlineInput = {
  width: 90,
  padding: "5px 8px",
  border: "1px solid #ccc",
  borderRadius: 4,
  fontSize: 13,
};
const radioRow = {
  display: "flex",
  alignItems: "center",
  fontSize: 13,
  marginBottom: 8,
  cursor: "pointer",
};
const addBtnStyle = {
  padding: "7px 16px",
  background: "#1976d2",
  color: "#fff",
  border: "none",
  borderRadius: 4,
  fontSize: 13,
  cursor: "pointer",
  whiteSpace: "nowrap",
};
const cancelBtnStyle = {
  padding: "7px 16px",
  background: "#e0e0e0",
  color: "#333",
  border: "none",
  borderRadius: 4,
  fontSize: 13,
  cursor: "pointer",
};

export default withSnackbar(UnitConversionConfig);
