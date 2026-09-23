import React from "react";
import { withSnackbar } from "notistack";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Button from "@material-ui/core/Button";
import CircularProgress from "@material-ui/core/CircularProgress";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";

const round6 = (x) => Math.round(x * 1e6) / 1e6;
const rs = (x) => "Rs. " + (x || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/**
 * Edits a PO line's billing unit + rate. Billing qty = baseQty / conversionFactor (canonical).
 * netRate/total are recomputed server-side; the preview here just mirrors that formula.
 */
class POLineBillingDialog extends React.Component {
  state = {
    conversions: [],
    loading: true,
    saving: false,
    billingUnit: this.props.line.billingUnit || "", // "" = base unit
    rate: this.props.line.rate != null ? String(this.props.line.rate) : "",
  };

  async componentDidMount() {
    const { productId } = this.props;
    const response = await API.GET(apiEndpoints.getUnitConversions(productId));
    if (response.success) {
      this.setState({ conversions: response.data, loading: false });
    } else {
      this.props.enqueueSnackbar("Failed to load unit conversions", { variant: "error" });
      this.setState({ loading: false });
    }
  }

  selectedFactor() {
    const { billingUnit, conversions } = this.state;
    if (!billingUnit) return null;
    const c = conversions.find((x) => x.unitName === billingUnit);
    return c ? c.conversionFactor : null;
  }

  computed() {
    const { line } = this.props;
    const { billingUnit } = this.state;
    const baseQty = parseFloat(line.quantity || 0);
    const rate = parseFloat(this.state.rate || 0);
    const disc = parseFloat(line.discountPercent || 0);
    const gst = parseFloat(line.gstPercent || 0);
    const factor = this.selectedFactor();
    const billingQty = billingUnit && factor ? round6(baseQty / factor) : null;
    const effQty = billingUnit ? (billingQty || 0) : baseQty;
    const netRate = rate * effQty * (1 - disc / 100);
    const total = netRate * (1 + gst / 100);
    return { baseQty, rate, factor, billingQty, effQty, netRate, total, disc, gst };
  }

  async handleSave() {
    const { poId, line } = this.props;
    const { billingUnit } = this.state;
    const { rate, factor, billingQty } = this.computed();

    if (isNaN(rate) || rate <= 0) {
      this.props.enqueueSnackbar("Rate must be a positive number", { variant: "warning" });
      return;
    }
    if (billingUnit && (!factor || !billingQty)) {
      this.props.enqueueSnackbar("Selected billing unit has no valid conversion", { variant: "warning" });
      return;
    }

    this.setState({ saving: true });
    const response = await API.PUT(apiEndpoints.updatePOLineBilling(poId, line.id), {
      rate,
      billingUnit: billingUnit || null,
      billingQuantity: billingUnit ? billingQty : null,
      billingConversionFactor: billingUnit ? factor : null,
    });
    this.setState({ saving: false });

    if (response.success) {
      this.props.enqueueSnackbar("Billing updated successfully", { variant: "success" });
      this.props.onSaved(response.data);
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Failed to update billing", { variant: "error" });
    }
  }

  render() {
    const { onClose, line } = this.props;
    const { conversions, loading, saving, billingUnit } = this.state;
    const baseUnit = line.product?.measurementUnit || line.unit || "base unit";
    const { baseQty, billingQty, netRate, total } = this.computed();

    return (
      <Dialog open onClose={onClose} maxWidth="xs" fullWidth disableBackdropClick>
        <DialogTitle>Edit Billing — <span style={{ fontWeight: 400 }}>{line.product?.name || line.product?.productName || ""}</span></DialogTitle>
        <DialogContent dividers>
          {loading ? (
            <div style={{ textAlign: "center", padding: "24px 0" }}><CircularProgress size={26} /></div>
          ) : (
            <>
              <div style={{ fontSize: 13, color: "#555", marginBottom: 12 }}>
                Ordered quantity: <strong>{baseQty} {baseUnit}</strong>
              </div>

              <label style={labelStyle}>Bill in</label>
              <select
                style={inputStyle}
                value={billingUnit}
                onChange={(e) => this.setState({ billingUnit: e.target.value })}
                disabled={saving}
              >
                <option value="">{baseUnit} (base unit)</option>
                {conversions.map((c) => (
                  <option key={c.id} value={c.unitName}>{c.unitName}</option>
                ))}
              </select>

              {billingUnit && (
                <div style={{ fontSize: 12, color: "#777", margin: "6px 0 12px" }}>
                  = <strong>{billingQty != null ? billingQty : "—"} {billingUnit}</strong> (from {baseQty} {baseUnit})
                </div>
              )}

              <label style={{ ...labelStyle, marginTop: 8 }}>
                Rate per {billingUnit || baseUnit}
              </label>
              <input
                style={inputStyle}
                type="number"
                min="0"
                step="any"
                value={this.state.rate}
                onChange={(e) => this.setState({ rate: e.target.value })}
                disabled={saving}
              />

              <div style={previewBox}>
                <div>Net value: <strong>{rs(netRate)}</strong></div>
                <div>Amount incl. tax: <strong>{rs(total)}</strong></div>
                <div style={{ fontSize: 11, color: "#999", marginTop: 4 }}>
                  Discount {line.discountPercent || 0}% · GST {line.gstPercent || 0}% (edit on the PO edit screen)
                </div>
              </div>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={saving}>Cancel</Button>
          <Button onClick={() => this.handleSave()} color="primary" variant="contained" disabled={saving || loading}>
            {saving ? "Saving…" : "Save"}
          </Button>
        </DialogActions>
      </Dialog>
    );
  }
}

const labelStyle = { display: "block", fontSize: 11, color: "#666", marginBottom: 4 };
const inputStyle = {
  width: "100%", padding: "8px 10px", border: "1px solid #ccc",
  borderRadius: 4, fontSize: 14, boxSizing: "border-box",
};
const previewBox = {
  marginTop: 16, padding: "10px 12px", background: "#f5f7fa",
  borderRadius: 4, fontSize: 13, lineHeight: 1.7,
};

export default withSnackbar(POLineBillingDialog);
