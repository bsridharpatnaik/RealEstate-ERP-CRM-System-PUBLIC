import React from "react";
import { withSnackbar } from "notistack";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import CircularProgress from "@material-ui/core/CircularProgress";
import IconButton from "@material-ui/core/IconButton";
import DeleteIcon from "@material-ui/icons/Delete";
import LockIcon from "@material-ui/icons/Lock";
import Tooltip from "@material-ui/core/Tooltip";
import Button from "./../../Shared/Button";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";

class UnitConversionConfig extends React.Component {
  state = {
    conversions: [],
    loading: true,
    saving: false,
    newUnitName: "",
    newFactor: "",
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

  async handleAdd() {
    const { newUnitName, newFactor } = this.state;
    const { product } = this.props;

    if (!newUnitName.trim()) {
      this.props.enqueueSnackbar("Unit name is required", { variant: "warning" });
      return;
    }
    const factor = parseFloat(newFactor);
    if (isNaN(factor) || factor <= 0) {
      this.props.enqueueSnackbar("Conversion factor must be a positive number", { variant: "warning" });
      return;
    }

    this.setState({ saving: true });
    const response = await API.POST(apiEndpoints.addUnitConversion(product.productId), {
      unitName: newUnitName.trim(),
      conversionFactor: factor,
    });
    this.setState({ saving: false });

    if (response.success) {
      this.props.enqueueSnackbar("Unit conversion added", { variant: "success" });
      this.setState({ newUnitName: "", newFactor: "" });
      this.loadConversions();
    } else {
      this.props.enqueueSnackbar("Failed to add unit conversion", { variant: "error" });
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
    const { conversions, loading, saving, newUnitName, newFactor } = this.state;
    const baseUnit = product.measurementUnit || "base unit";

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
                      <th style={thStyle}>1 unit = ? {baseUnit}</th>
                      <th style={thStyle}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {conversions.map((c) => (
                      <tr key={c.id}>
                        <td style={tdStyle}>{c.unitName}</td>
                        <td style={tdStyle}>{c.conversionFactor} {baseUnit}</td>
                        <td style={{ ...tdStyle, textAlign: "center" }}>
                          {c.usedInPo ? (
                            <Tooltip title="Used in a purchase order — cannot delete">
                              <span>
                                <LockIcon fontSize="small" style={{ color: "#aaa", verticalAlign: "middle" }} />
                              </span>
                            </Tooltip>
                          ) : (
                            <IconButton
                              size="small"
                              onClick={() => this.handleDelete(c)}
                              disabled={saving}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <div style={{ display: "flex", gap: 8, alignItems: "flex-end", marginTop: 8 }}>
                <div style={{ flex: 1 }}>
                  <label style={labelStyle}>Billing Unit Name</label>
                  <input
                    style={inputStyle}
                    placeholder="e.g. Box, Bundle, Sheet"
                    value={newUnitName}
                    onChange={(e) => this.setState({ newUnitName: e.target.value })}
                    disabled={saving}
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={labelStyle}>1 unit = ? {baseUnit}</label>
                  <input
                    style={inputStyle}
                    type="number"
                    placeholder="e.g. 12"
                    min="0.001"
                    step="any"
                    value={newFactor}
                    onChange={(e) => this.setState({ newFactor: e.target.value })}
                    disabled={saving}
                  />
                </div>
                <button
                  style={addBtnStyle}
                  onClick={() => this.handleAdd()}
                  disabled={saving}
                >
                  + Add
                </button>
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

export default withSnackbar(UnitConversionConfig);
