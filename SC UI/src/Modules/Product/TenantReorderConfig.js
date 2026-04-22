import React from "react";
import { connect } from "react-redux";
import { withSnackbar } from "notistack";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import CircularProgress from "@material-ui/core/CircularProgress";
import Button from "./../../Shared/Button";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";

class TenantReorderConfig extends React.Component {
  state = {
    configs: [],
    loading: true,
    saving: false,
    inputValues: {},       // { [tenantSchema]: string }
    overrideEnabled: {},   // { [tenantSchema]: boolean }
    originalOverridden: new Set(), // tenantSchemas that had an override when loaded
  };

  componentDidMount() {
    this.loadConfigs();
  }

  async loadConfigs() {
    this.setState({ loading: true });
    const { product } = this.props;
    const response = await API.GET(
      apiEndpoints.getAllTenantReorderConfigs(product.productId)
    );
    if (response.success) {
      const configs = response.data;
      const inputValues = {};
      const overrideEnabled = {};
      const originalOverridden = new Set();
      configs.forEach((c) => {
        inputValues[c.tenantSchema] = c.overrideReorderLevel != null
          ? String(c.overrideReorderLevel)
          : "";
        overrideEnabled[c.tenantSchema] = c.isOverridden === true;
        if (c.isOverridden) originalOverridden.add(c.tenantSchema);
      });
      this.setState({ configs, inputValues, overrideEnabled, originalOverridden, loading: false });
    } else {
      this.props.enqueueSnackbar("Failed to load tenant configurations", { variant: "error" });
      this.setState({ loading: false });
    }
  }

  getTenantDisplayName(tenantSchema) {
    const allTennants = this.props.allTennants || [];
    const match = allTennants.find((t) => t.tenantCode === tenantSchema);
    return match ? match.tenantName : tenantSchema;
  }

  handleToggleOverride(tenantSchema, enabled) {
    this.setState((prev) => ({
      overrideEnabled: { ...prev.overrideEnabled, [tenantSchema]: enabled },
      inputValues: enabled
        ? prev.inputValues
        : { ...prev.inputValues, [tenantSchema]: "" },
    }));
  }

  handleInputChange(tenantSchema, value) {
    this.setState((prev) => ({
      inputValues: { ...prev.inputValues, [tenantSchema]: value },
    }));
  }

  async handleSaveAll() {
    const { product } = this.props;
    const { configs, inputValues, overrideEnabled, originalOverridden } = this.state;

    // Validate all enabled rows before firing any request
    for (const c of configs) {
      if (overrideEnabled[c.tenantSchema]) {
        const val = parseFloat(inputValues[c.tenantSchema]);
        if (isNaN(val) || val < 0) {
          this.props.enqueueSnackbar(
            `Invalid reorder value for ${this.getTenantDisplayName(c.tenantSchema)}`,
            { variant: "warning" }
          );
          return;
        }
      }
    }

    this.setState({ saving: true });

    // Build one promise per tenant that needs a change
    const promises = configs.map((c) => {
      const schema = c.tenantSchema;
      const isEnabled = overrideEnabled[schema];

      if (isEnabled) {
        // Save / update override
        const reorderLevel = parseFloat(inputValues[schema]);
        return API.PUT(
          apiEndpoints.saveTenantReorderConfig(product.productId),
          { tenantName: schema, reorderLevel }
        );
      } else if (originalOverridden.has(schema)) {
        // Was overridden before — user unchecked it → remove
        return API.DELETE(
          apiEndpoints.deleteTenantReorderConfig(product.productId, schema)
        );
      }
      // No override before, still no override — nothing to do
      return Promise.resolve({ success: true, noop: true });
    });

    const results = await Promise.all(promises);
    this.setState({ saving: false });

    const anyFailed = results.some((r) => !r.success && !r.noop);
    if (anyFailed) {
      this.props.enqueueSnackbar("Some changes could not be saved", { variant: "error" });
    } else {
      this.props.enqueueSnackbar("Reorder level overrides saved", { variant: "success" });
    }

    // Reload to reflect persisted state
    this.loadConfigs();
  }

  render() {
    const { product, onClose } = this.props;
    const { configs, loading, saving, inputValues, overrideEnabled } = this.state;

    return (
      <Dialog
        open
        onClose={onClose}
        maxWidth="md"
        fullWidth
        disableBackdropClick
        disableEscapeKeyDown
      >
        <DialogTitle className="trc-dialog-title">
          Tenant Reorder Level Overrides —{" "}
          <span style={{ fontWeight: 400 }}>{product.productName}</span>
          <div style={{ fontSize: 12, fontWeight: 400, color: "#777", marginTop: 2 }}>
            Global reorder level:{" "}
            <strong>
              {product.reorderQuantity != null ? product.reorderQuantity : "—"}
            </strong>
          </div>
        </DialogTitle>

        <DialogContent dividers>
          {loading ? (
            <div style={{ textAlign: "center", padding: "32px 0" }}>
              <CircularProgress size={28} />
            </div>
          ) : configs.length === 0 ? (
            <div style={{ color: "#777", textAlign: "center", padding: "24px 0" }}>
              No tenant schemas found.
            </div>
          ) : (
            <table className="trc-table">
              <thead>
                <tr>
                  <th>Tenant</th>
                  <th>Global Level</th>
                  <th>Override Active</th>
                  <th>Override Value</th>
                </tr>
              </thead>
              <tbody>
                {configs.map((c) => {
                  const isEnabled = overrideEnabled[c.tenantSchema];
                  const inputVal = inputValues[c.tenantSchema] || "";
                  const displayName = this.getTenantDisplayName(c.tenantSchema);

                  return (
                    <tr key={c.tenantSchema}>
                      <td>
                        <div className="trc-tenant-name">{displayName}</div>
                        <div className="trc-tenant-schema">{c.tenantSchema}</div>
                      </td>
                      <td>{c.globalReorderLevel != null ? c.globalReorderLevel : "—"}</td>
                      <td>
                        <label className="trc-toggle-label">
                          <input
                            type="checkbox"
                            className="trc-checkbox"
                            checked={isEnabled || false}
                            onChange={(e) =>
                              this.handleToggleOverride(c.tenantSchema, e.target.checked)
                            }
                          />
                          Override
                        </label>
                      </td>
                      <td>
                        {isEnabled ? (
                          <input
                            type="number"
                            className="trc-input"
                            value={inputVal}
                            min="0"
                            step="0.01"
                            placeholder="Enter value"
                            onChange={(e) =>
                              this.handleInputChange(c.tenantSchema, e.target.value)
                            }
                          />
                        ) : (
                          <span style={{ color: "#aaa" }}>
                            {c.isOverridden ? "Will be removed on save" : "—"}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </DialogContent>

        <DialogActions>
          <Button onClick={onClose} buttonClass="grey" label="Close" disabled={saving} />
          <Button
            onClick={() => this.handleSaveAll()}
            buttonClass="blue"
            label={saving ? "Saving…" : "Save"}
            disabled={saving || loading}
          />
        </DialogActions>
      </Dialog>
    );
  }
}

const mapStateToProps = (state) => ({
  allTennants: state.allTennant.tennants,
});

export default connect(mapStateToProps)(withSnackbar(TenantReorderConfig));
