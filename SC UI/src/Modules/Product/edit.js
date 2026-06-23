//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogContentText from "@material-ui/core/DialogContentText";
import DialogActions from "@material-ui/core/DialogActions";
import Button from "@material-ui/core/Button";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";

const BATCH_OPTIONS = [
  {
    value: "NONE",
    label: "No Tracking",
    desc: "Basic products. No batch data captured.",
  },
  {
    value: "BATCH_ONLY",
    label: "Lot / Brand Tracking",
    desc: "Track by supplier lot or brand. FIFO ordering. No expiry date.",
  },
  {
    value: "BATCH_WITH_EXPIRY",
    label: "Lot + Expiry Date",
    desc: "Track lot and expiry date. FEFO (nearest-expiry-first) ordering.",
  },
];

class Edit extends EditForm {
  updateUrl = apiEndpoints.individualProduct;
  title = messages.common.product;

  state = {
    data: {},
    isLoaded: false,
    categoriesLoaded: false,
    batchMode: "NONE",
    originalBatchMode: "NONE",
    batchModeChanged: false,
    batchModeConfirmOpen: false,
    categories: [],
    // Tenant reorder overrides
    tenantConfigs: [],
    tenantInputValues: {},
    tenantOverrideEnabled: {},
    tenantOriginalOverridden: new Set(),
    tenantConfigsLoaded: false,
    tenantSaving: false,
    tenantOverridesExpanded: false,
  };

  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;
    this.search();
    this.fetchCategories();
    this.loadTenantConfigs();
  }

  async fetchCategories() {
    const response = await API.GET(apiEndpoints.getCategoryIdAndNames);
    if (response.success && Array.isArray(response.data)) {
      this.setState({
        categories: response.data.map((cat) => ({
          id: cat.categoryId ?? cat.id,
          name: cat.categoryName ?? cat.name,
        })),
        categoriesLoaded: true,
      });
    }
  }

  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.productName = data.productName;
      this.formData.reorderQuantity = data.reorderQuantity;
      this.formData.productDescription = data.productDescription;
      this.formData.measurementUnit = data.measurementUnit;
      this.formData.categoryId = data.category.categoryId;
      this.formData.showOnDashboard = data.showOnDashboard;
      this.formData.isManagedInventory =
        data.isManagedInventory !== undefined ? data.isManagedInventory : true;
      this.formData.batchMode =
        data.batchMode || (data.isExpirable ? "BATCH_WITH_EXPIRY" : "NONE");
      this.formData.leadTimeDays = data.leadTimeDays != null ? String(data.leadTimeDays) : "";
      this.formData.defaultExpiryDays = data.defaultExpiryDays != null ? String(data.defaultExpiryDays) : "";
      this.setState({
        isLoaded: true,
        batchMode: this.formData.batchMode,
        originalBatchMode: this.formData.batchMode,
      });
    }
  }

  async loadTenantConfigs() {
    const response = await API.GET(
      apiEndpoints.getAllTenantReorderConfigs(this.props.id)
    );
    if (response.success) {
      const configs = response.data;
      const tenantInputValues = {};
      const tenantOverrideEnabled = {};
      const tenantOriginalOverridden = new Set();
      configs.forEach((c) => {
        tenantInputValues[c.tenantSchema] =
          c.overrideReorderLevel != null ? String(c.overrideReorderLevel) : "";
        tenantOverrideEnabled[c.tenantSchema] = c.isOverridden === true;
        if (c.isOverridden) tenantOriginalOverridden.add(c.tenantSchema);
      });
      this.setState({
        tenantConfigs: configs,
        tenantInputValues,
        tenantOverrideEnabled,
        tenantOriginalOverridden,
        tenantConfigsLoaded: true,
      });
    }
  }

  getTenantDisplayName(tenantSchema) {
    const allTennants = this.props.allTennants || [];
    const match = allTennants.find((t) => t.tenantCode === tenantSchema);
    return match ? match.tenantName : tenantSchema;
  }

  /** Validate overrides — returns true if valid, false + snackbar if not. */
  validateOverrides() {
    const { tenantConfigs, tenantInputValues, tenantOverrideEnabled } = this.state;
    for (const c of tenantConfigs) {
      if (tenantOverrideEnabled[c.tenantSchema]) {
        const val = parseFloat(tenantInputValues[c.tenantSchema]);
        if (isNaN(val) || val < 0) {
          this.props.enqueueSnackbar(
            `Invalid reorder value for ${this.getTenantDisplayName(c.tenantSchema)}`,
            { variant: "warning" }
          );
          return false;
        }
      }
    }
    return true;
  }

  /** Build and fire override save promises. showSuccess = false when called from update(). */
  async saveTenantConfigs({ showSuccess = true } = {}) {
    const {
      tenantConfigs,
      tenantInputValues,
      tenantOverrideEnabled,
      tenantOriginalOverridden,
    } = this.state;

    if (!this.validateOverrides()) return false;

    this.setState({ tenantSaving: true });

    const promises = tenantConfigs.map((c) => {
      const schema = c.tenantSchema;
      const isEnabled = tenantOverrideEnabled[schema];
      if (isEnabled) {
        return API.PUT(apiEndpoints.saveTenantReorderConfig(this.props.id), {
          tenantName: schema,
          reorderLevel: parseFloat(tenantInputValues[schema]),
        });
      } else if (tenantOriginalOverridden.has(schema)) {
        return API.DELETE(
          apiEndpoints.deleteTenantReorderConfig(this.props.id, schema)
        );
      }
      return Promise.resolve({ success: true, noop: true });
    });

    const results = await Promise.all(promises);
    this.setState({ tenantSaving: false });

    const anyFailed = results.some((r) => !r.success && !r.noop);
    if (anyFailed) {
      this.props.enqueueSnackbar("Some overrides could not be saved", {
        variant: "error",
      });
    } else if (showSuccess) {
      this.props.enqueueSnackbar("Reorder level overrides saved", {
        variant: "success",
      });
    }
    this.loadTenantConfigs();
    return !anyFailed;
  }

  async update(event) {
    event.preventDefault();

    // Validate overrides before anything else
    if (this.state.tenantConfigsLoaded && this.state.tenantConfigs.length > 0) {
      if (!this.validateOverrides()) return;
    }

    if (this.state.batchModeChanged) {
      this.setState({ batchModeConfirmOpen: true });
      return;
    }

    this.doUpdate();
  }

  async doUpdate() {
    this.setState({ batchModeConfirmOpen: false, isUpdating: true });
    const [response] = await Promise.all([
      API.PUT(this.updateUrl, this.formData),
      this.state.tenantConfigsLoaded && this.state.tenantConfigs.length > 0
        ? this.saveTenantConfigs({ showSuccess: false })
        : Promise.resolve(),
    ]);
    this.showToaster(response);
    this.setState({ isUpdating: false });
  }

  renderBatchModeConfirmDialog() {
    return (
      <Dialog open={this.state.batchModeConfirmOpen} onClose={() => this.setState({ batchModeConfirmOpen: false })}>
        <DialogTitle>Change Batch Tracking Mode?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Changing the batch tracking mode for an existing product may cause inconsistencies with existing inventory.
          </DialogContentText>
          <DialogContentText style={{ marginTop: 8 }}>
            Existing stock without batch data will remain untracked, and FIFO ordering may be affected for in-progress batches.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => this.setState({ batchModeConfirmOpen: false })} color="default">
            Cancel
          </Button>
          <Button onClick={() => this.doUpdate()} color="primary" variant="contained">
            Continue
          </Button>
        </DialogActions>
      </Dialog>
    );
  }

  renderBatchModeCards() {
    const { batchMode, batchModeChanged } = this.state;
    return (
      <div style={{ marginBottom: 8, width: "100%" }}>
        <label
          style={{
            fontSize: 12,
            color: "#666",
            fontWeight: 500,
            display: "block",
            marginBottom: 8,
          }}
        >
          Batch Tracking
        </label>
        <div style={{ display: "flex", gap: 12 }}>
          {BATCH_OPTIONS.map(({ value, label, desc }) => {
            const selected = batchMode === value;
            return (
              <label
                key={value}
                style={{
                  flex: 1,
                  display: "flex",
                  gap: 10,
                  padding: "10px 14px",
                  border: selected ? "2px solid #1976d2" : "1px solid #ccc",
                  borderRadius: 6,
                  background: selected ? "#e3f2fd" : "#fff",
                  cursor: "pointer",
                }}
              >
                <input
                  type="radio"
                  name="batchMode"
                  value={value}
                  checked={selected}
                  onChange={() => {
                    this.formData.batchMode = value;
                    this.setState({
                      batchMode: value,
                      batchModeChanged: value !== this.state.originalBatchMode,
                    });
                  }}
                  style={{ marginTop: 3, flexShrink: 0 }}
                />
                <div>
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{label}</div>
                  <div style={{ fontSize: 11, color: "#666", marginTop: 3 }}>
                    {desc}
                  </div>
                </div>
              </label>
            );
          })}
        </div>
        {batchModeChanged && (
          <span
            style={{
              fontSize: 11,
              color: "#e65100",
              marginTop: 6,
              display: "block",
            }}
          >
            ⚠ Changing this may affect existing inventory records
          </span>
        )}
      </div>
    );
  }

  renderTenantOverridesSection() {
    const {
      tenantConfigs,
      tenantInputValues,
      tenantOverrideEnabled,
      tenantConfigsLoaded,
      tenantSaving,
      tenantOverridesExpanded,
    } = this.state;

    if (!tenantConfigsLoaded || tenantConfigs.length === 0) return null;

    const activeCount = Object.values(tenantOverrideEnabled).filter(
      Boolean
    ).length;

    return (
      <div
        style={{
          marginTop: 16,
          marginBottom: 8,
          border: "1px solid #e0e0e0",
          borderRadius: 6,
          overflow: "hidden",
        }}
      >
        {/* Collapsible header */}
        <div
          onClick={() =>
            this.setState({
              tenantOverridesExpanded: !tenantOverridesExpanded,
            })
          }
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            padding: "10px 16px",
            background: "#f5f5f5",
            cursor: "pointer",
            userSelect: "none",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: "#333" }}>
              {tenantOverridesExpanded ? "▾" : "▸"} Project-level Reorder
              Overrides
            </span>
            {activeCount > 0 && (
              <span
                style={{
                  fontSize: 11,
                  background: "#1976d2",
                  color: "#fff",
                  borderRadius: 10,
                  padding: "1px 8px",
                }}
              >
                {activeCount} active
              </span>
            )}
          </div>
          <span style={{ fontSize: 11, color: "#888" }}>
            {tenantOverridesExpanded ? "Click to collapse" : "Click to expand"}
          </span>
        </div>

        {/* Expandable body */}
        {tenantOverridesExpanded && (
          <div style={{ padding: 16 }}>
            <p style={{ margin: "0 0 12px", fontSize: 12, color: "#666" }}>
              Override the global reorder level for specific projects. Leave
              unchecked to use the global value.
            </p>
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                fontSize: 13,
              }}
            >
              <thead>
                <tr style={{ background: "#fafafa" }}>
                  <th
                    style={{
                      textAlign: "left",
                      padding: "6px 10px",
                      borderBottom: "1px solid #e0e0e0",
                      fontWeight: 600,
                    }}
                  >
                    Project
                  </th>
                  <th
                    style={{
                      textAlign: "left",
                      padding: "6px 10px",
                      borderBottom: "1px solid #e0e0e0",
                      fontWeight: 600,
                    }}
                  >
                    Global Level
                  </th>
                  <th
                    style={{
                      textAlign: "left",
                      padding: "6px 10px",
                      borderBottom: "1px solid #e0e0e0",
                      fontWeight: 600,
                    }}
                  >
                    Override
                  </th>
                  <th
                    style={{
                      textAlign: "left",
                      padding: "6px 10px",
                      borderBottom: "1px solid #e0e0e0",
                      fontWeight: 600,
                    }}
                  >
                    Override Value
                  </th>
                </tr>
              </thead>
              <tbody>
                {tenantConfigs.map((c) => {
                  const isEnabled =
                    tenantOverrideEnabled[c.tenantSchema] || false;
                  const inputVal = tenantInputValues[c.tenantSchema] || "";
                  const displayName = this.getTenantDisplayName(c.tenantSchema);
                  return (
                    <tr
                      key={c.tenantSchema}
                      style={{ borderBottom: "1px solid #f0f0f0" }}
                    >
                      <td style={{ padding: "8px 10px" }}>
                        <div style={{ fontWeight: 500 }}>{displayName}</div>
                        <div style={{ fontSize: 10, color: "#aaa" }}>
                          {c.tenantSchema}
                        </div>
                      </td>
                      <td style={{ padding: "8px 10px", color: "#555" }}>
                        {c.globalReorderLevel != null
                          ? c.globalReorderLevel
                          : "—"}
                      </td>
                      <td style={{ padding: "8px 10px" }}>
                        <label
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: 6,
                            cursor: "pointer",
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={isEnabled}
                            onChange={(e) => {
                              const enabled = e.target.checked;
                              this.setState((prev) => ({
                                tenantOverrideEnabled: {
                                  ...prev.tenantOverrideEnabled,
                                  [c.tenantSchema]: enabled,
                                },
                                tenantInputValues: enabled
                                  ? prev.tenantInputValues
                                  : {
                                      ...prev.tenantInputValues,
                                      [c.tenantSchema]: "",
                                    },
                              }));
                            }}
                          />
                          <span style={{ fontSize: 12 }}>Override</span>
                        </label>
                      </td>
                      <td style={{ padding: "8px 10px" }}>
                        {isEnabled ? (
                          <input
                            type="number"
                            value={inputVal}
                            min="0"
                            step="0.01"
                            placeholder="Enter value"
                            onChange={(e) => {
                              const val = e.target.value;
                              this.setState((prev) => ({
                                tenantInputValues: {
                                  ...prev.tenantInputValues,
                                  [c.tenantSchema]: val,
                                },
                              }));
                            }}
                            style={{
                              padding: "4px 8px",
                              border: "1px solid #ccc",
                              borderRadius: 4,
                              width: 100,
                              fontSize: 13,
                            }}
                          />
                        ) : (
                          <span style={{ color: "#aaa", fontSize: 12 }}>
                            {c.isOverridden ? "Will be removed on save" : "—"}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <p style={{ margin: "10px 0 0", fontSize: 11, color: "#888" }}>
              Changes will be saved when you click <strong>Save</strong> below.
            </p>
          </div>
        )}
      </div>
    );
  }

  render() {
    return (
      <div className="list-section add">
        {this.renderBatchModeConfirmDialog()}
        {this.renderHeading()}
        {this.state.isLoaded && this.state.categoriesLoaded && (
          <form onSubmit={(e) => this.update(e)}>
            <div className="flex">
              {this.renderTextField({
                fieldname: "productName",
                placeholder: messages.common.inventory,
                required: true,
              })}
            </div>
            <div className="flex">
              {this.renderTextField({
                fieldname: "productDescription",
                placeholder: messages.common.description,
              })}
            </div>
            <div className="flex width50">
              {this.renderTextField({
                fieldname: "reorderQuantity",
                placeholder: "Reorder Level",
                required: true,
                type: "number",
                validation: "nonegative",
              })}
              {this.renderTextField({
                fieldname: "measurementUnit",
                placeholder: messages.fields.measurementUnit,
                required: true,
              })}
              {this.renderAutoComplete({
                fieldname: "categoryId",
                placeholder: "Category",
                options: this.state.categories,
                disableClearable: true,
                required: true,
                getOption: (option) => option.name,
              })}
              {this.renderTextField({
                fieldname: "leadTimeDays",
                placeholder: "Lead Time (Days)",
                type: "number",
              })}
            </div>
            <div className="flex">{this.renderBatchModeCards()}</div>
            {this.state.batchMode === "BATCH_WITH_EXPIRY" && (
              <div className="flex width50">
                {this.renderTextField({
                  fieldname: "defaultExpiryDays",
                  placeholder: "Default Expiry (Days)",
                  type: "number",
                  validation: "nonegative",
                })}
              </div>
            )}
            <div className="flex">
              {this.renderToggle("Show in Dashboard", "showOnDashboard")}
              {this.renderToggle("Is Managed Inventory", "isManagedInventory")}
            </div>
            {this.renderTenantOverridesSection()}
            {this.renderFooter()}
          </form>
        )}
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  allTennants: state.allTennant.tennants,
});

export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Edit)
);
