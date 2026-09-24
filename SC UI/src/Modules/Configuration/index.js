import React, { Component } from "react";
import { withSnackbar } from "notistack";
import Paper from "@material-ui/core/Paper";
import Switch from "@material-ui/core/Switch";
import TextField from "@material-ui/core/TextField";
import Button from "@material-ui/core/Button";
import Tooltip from "@material-ui/core/Tooltip";
import CircularProgress from "@material-ui/core/CircularProgress";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";
import { getRole } from "../../helper";

// Metadata for known configuration keys. Anything the backend returns that is
// not listed here is ignored (safe default for future keys until UI is added).
const SECTIONS = [
  {
    title: "BOQ Enforcement (Outward)",
    description:
      "Controls whether outward entries are blocked or only warned when they conflict with the project BOQ.",
    items: [
      {
        key: "BOQ_BLOCK_ON_EXCEED",
        label: "Block when quantity exceeds BOQ",
        help: "ON: outward save is rejected when requested qty exceeds remaining BOQ (incl. wastage). OFF: warning only, save proceeds.",
        type: "boolean",
      },
      {
        key: "BOQ_BLOCK_WHEN_MISSING",
        label: "Block when BOQ is not defined",
        help: "ON: outward save is rejected when the product has no BOQ record for the selected location. OFF: no check.",
        type: "boolean",
      },
    ],
  },
  {
    title: "Edit / Delete Window (days)",
    description:
      "How many days back a user can edit or delete inward/outward records, by role.",
    items: [
      { key: "INVENTORY_ALLOWED_DAYS_ADMIN", label: "Admin", type: "number" },
      { key: "INVENTORY_ALLOWED_DAYS_MANAGER", label: "Purchase Manager", type: "number" },
      { key: "INVENTORY_ALLOWED_DAYS_EXECUTIVE", label: "Executive / Others", type: "number" },
    ],
  },
  {
    title: "Reject / Return Window (days)",
    description:
      "How many days back a user can add a reject or return against a record, by role.",
    items: [
      { key: "INVENTORY_REJECT_RETURN_DAYS_ADMIN", label: "Admin", type: "number" },
      { key: "INVENTORY_REJECT_RETURN_DAYS_MANAGER", label: "Purchase Manager", type: "number" },
      { key: "INVENTORY_REJECT_RETURN_DAYS_EXECUTIVE", label: "Executive / Others", type: "number" },
    ],
  },
  {
    title: "Stock Expiry",
    description:
      "Window used for the near-expiry stock tile, expiry notifications and the daily stock email.",
    items: [
      {
        key: "NEAR_EXPIRY_DAYS",
        label: "Near-expiry window (days)",
        help: "Batches expiring within this many days are counted as near-expiry. Max 60 (the second expiry band ends at 60 days).",
        type: "number",
        min: 1,
        max: 60,
      },
    ],
  },
];

const ALL_KEYS = SECTIONS.flatMap((s) => s.items.map((i) => i.key));

class Configuration extends Component {
  // Menu entry is admin-only, but the route is reachable by URL; backend rejects
  // non-admin saves with 403 — render read-only instead of letting them fail.
  isAdmin = (getRole() || "").toLowerCase() === "admin";

  state = {
    loading: true,
    // [{ name: display name, id: tenantCode }]
    tenants: [],
    activeTenant: null,
    // tenantCode → { key → { id, value } } as loaded from backend
    constantsByTenant: {},
    // tenantCode → { key → edited value }
    editedByTenant: {},
    saving: false,
  };

  componentDidMount() {
    this.loadAll();
  }

  tenantHeader(tenantCode) {
    return { headers: { "tenant-id": tenantCode } };
  }

  loadAll = async () => {
    this.setState({ loading: true });
    const res = await API.GET(apiEndpoints.getTenants);
    if (!res.success || !Array.isArray(res.data)) {
      this.props.enqueueSnackbar(res.errorMessage || "Failed to load projects", {
        variant: "error",
      });
      this.setState({ loading: false });
      return;
    }
    const tenants = res.data
      .filter((t) => t.inventory === true)
      .map((t) => ({ name: t.tenantName || t.name || "", id: t.tenantCode }))
      .filter((t) => t.name && t.id);

    // Load every project's constants up front — needed for the "differs" dots
    // on tabs, and makes switching tabs instant.
    const results = await Promise.all(
      tenants.map((t) =>
        API.GET(apiEndpoints.inventoryProjectConstants, this.tenantHeader(t.id))
      )
    );
    const constantsByTenant = {};
    const failedTenants = [];
    tenants.forEach((t, i) => {
      const map = {};
      if (results[i].success && Array.isArray(results[i].data)) {
        results[i].data.forEach((row) => {
          map[row.key] = { id: row.id, value: row.value };
        });
      } else {
        failedTenants.push(t.name);
      }
      constantsByTenant[t.id] = map;
    });
    if (failedTenants.length > 0) {
      this.props.enqueueSnackbar(
        `Could not load settings for: ${failedTenants.join(", ")}`,
        { variant: "warning" }
      );
    }

    this.setState({
      tenants,
      constantsByTenant,
      editedByTenant: {},
      activeTenant: this.state.activeTenant || (tenants[0] && tenants[0].id) || null,
      loading: false,
    });
  };

  reloadTenant = async (tenantCode) => {
    const res = await API.GET(
      apiEndpoints.inventoryProjectConstants,
      this.tenantHeader(tenantCode)
    );
    if (res.success && Array.isArray(res.data)) {
      const map = {};
      res.data.forEach((row) => {
        map[row.key] = { id: row.id, value: row.value };
      });
      this.setState({
        constantsByTenant: { ...this.state.constantsByTenant, [tenantCode]: map },
        editedByTenant: { ...this.state.editedByTenant, [tenantCode]: {} },
      });
    }
  };

  currentValue(tenantCode, key) {
    const edited = this.state.editedByTenant[tenantCode] || {};
    if (edited[key] !== undefined) return edited[key];
    const row = (this.state.constantsByTenant[tenantCode] || {})[key];
    return row ? row.value : undefined;
  }

  setValue(key, value) {
    const tenantCode = this.state.activeTenant;
    const edited = { ...(this.state.editedByTenant[tenantCode] || {}), [key]: value };
    this.setState({
      editedByTenant: { ...this.state.editedByTenant, [tenantCode]: edited },
    });
  }

  dirtyKeys(tenantCode) {
    const edited = this.state.editedByTenant[tenantCode] || {};
    return Object.keys(edited).filter((key) => {
      const row = (this.state.constantsByTenant[tenantCode] || {})[key];
      if (!row) return false;
      return Number(edited[key]) !== Number(row.value);
    });
  }

  validate(tenantCode, dirty) {
    const edited = this.state.editedByTenant[tenantCode] || {};
    for (const key of dirty) {
      const meta = SECTIONS.flatMap((s) => s.items).find((i) => i.key === key);
      const raw = edited[key];
      // Number("") === 0, so an emptied field would silently save as 0 — reject it
      if (raw === "" || raw === null || (typeof raw === "string" && raw.trim() === "")) {
        return `${meta ? meta.label : key}: value cannot be empty`;
      }
      const value = Number(raw);
      if (!Number.isFinite(value) || value < 0) {
        return `${meta ? meta.label : key}: enter a valid number`;
      }
      if (meta && meta.type === "number" && meta.min !== undefined && value < meta.min) {
        return `${meta.label}: minimum is ${meta.min}`;
      }
      if (meta && meta.type === "number" && meta.max !== undefined && value > meta.max) {
        return `${meta.label}: maximum is ${meta.max}`;
      }
    }
    return null;
  }

  save = async () => {
    const tenantCode = this.state.activeTenant;
    const dirty = this.dirtyKeys(tenantCode);
    if (dirty.length === 0) return;
    const error = this.validate(tenantCode, dirty);
    if (error) {
      this.props.enqueueSnackbar(error, { variant: "error" });
      return;
    }
    this.setState({ saving: true });
    const edited = this.state.editedByTenant[tenantCode] || {};
    let failed = 0;
    for (const key of dirty) {
      const row = this.state.constantsByTenant[tenantCode][key];
      const response = await API.PUT(
        apiEndpoints.updateInventoryProjectConstant(row.id),
        { value: Number(edited[key]) },
        this.tenantHeader(tenantCode)
      );
      if (!response.success) {
        failed++;
        this.props.enqueueSnackbar(
          `${key}: ${response.errorMessage || "update failed"}`,
          { variant: "error" }
        );
      }
    }
    this.setState({ saving: false });
    if (failed === 0) {
      this.props.enqueueSnackbar("Configuration saved", { variant: "success" });
    }
    this.reloadTenant(tenantCode);
  };

  /**
   * True when this tenant's saved value for any known key differs from another
   * tenant's saved value (uses DB values, not unsaved edits).
   */
  tenantDiffers(tenantCode) {
    const { constantsByTenant, tenants } = this.state;
    const mine = constantsByTenant[tenantCode] || {};
    for (const key of ALL_KEYS) {
      if (!mine[key]) continue;
      for (const t of tenants) {
        if (t.id === tenantCode) continue;
        const theirs = (constantsByTenant[t.id] || {})[key];
        if (theirs && Number(theirs.value) !== Number(mine[key].value)) {
          return true;
        }
      }
    }
    return false;
  }

  renderItem(item) {
    const tenantCode = this.state.activeTenant;
    const value = this.currentValue(tenantCode, item.key);
    if (value === undefined) return null; // key not present in this project's DB
    return (
      <div
        key={item.key}
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "10px 0",
          borderBottom: "1px solid #f0f2f5",
          gap: 16,
        }}
      >
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, fontWeight: 500, color: "#333" }}>
            {item.label}
          </div>
          {item.help && (
            <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>
              {item.help}
            </div>
          )}
        </div>
        {item.type === "boolean" ? (
          <Switch
            color="primary"
            disabled={!this.isAdmin}
            checked={Number(value) === 1}
            onChange={(e) => this.setValue(item.key, e.target.checked ? 1 : 0)}
          />
        ) : (
          <TextField
            type="number"
            variant="outlined"
            size="small"
            disabled={!this.isAdmin}
            style={{ width: 110 }}
            value={value}
            inputProps={{
              min: item.min !== undefined ? item.min : 0,
              ...(item.max !== undefined ? { max: item.max } : {}),
            }}
            onChange={(e) => this.setValue(item.key, e.target.value)}
          />
        )}
      </div>
    );
  }

  renderTab(tenant) {
    const active = this.state.activeTenant === tenant.id;
    const dirty = this.dirtyKeys(tenant.id).length > 0;
    const differs = this.tenantDiffers(tenant.id);
    return (
      <button
        key={tenant.id}
        onClick={() => this.setState({ activeTenant: tenant.id })}
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: 6,
          padding: "6px 14px",
          borderRadius: 16,
          border: active ? "1px solid #1976d2" : "1px solid #cfd8dc",
          background: active ? "#1976d2" : "#fff",
          color: active ? "#fff" : "#455a64",
          fontSize: 13,
          fontWeight: active ? 600 : 400,
          cursor: "pointer",
          outline: "none",
        }}
      >
        {tenant.name}
        {dirty && (
          <span
            style={{ color: active ? "#ffe082" : "#e65100", fontWeight: 700 }}
            title="Unsaved changes"
          >
            *
          </span>
        )}
        {differs && (
          <Tooltip title="Some settings differ from other projects">
            <span
              style={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                background: active ? "#ffe082" : "#fb8c00",
                display: "inline-block",
              }}
            />
          </Tooltip>
        )}
      </button>
    );
  }

  render() {
    const { loading, saving, tenants, activeTenant } = this.state;
    const dirtyCount = activeTenant ? this.dirtyKeys(activeTenant).length : 0;
    const activeName =
      (tenants.find((t) => t.id === activeTenant) || {}).name || "";
    return (
      <div style={{ padding: 16 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 4,
          }}
        >
          <h2 style={{ margin: 0, fontSize: 20 }}>Configuration</h2>
          {this.isAdmin && (
            <Button
              variant="contained"
              color="primary"
              disabled={loading || saving || dirtyCount === 0}
              onClick={this.save}
            >
              {saving ? "Saving…" : dirtyCount > 0 ? `Save ${activeName} (${dirtyCount})` : "Save"}
            </Button>
          )}
        </div>
        {!this.isAdmin && (
          <div
            style={{
              fontSize: 13,
              color: "#8a6d3b",
              background: "#fcf8e3",
              border: "1px solid #faebcc",
              borderRadius: 4,
              padding: "6px 12px",
              marginBottom: 8,
            }}
          >
            Read-only view — only administrators can change configuration.
          </div>
        )}
        <div style={{ fontSize: 12, color: "#888", marginBottom: 12 }}>
          Per-project settings. An orange dot on a project means its saved values
          differ from other projects; * means unsaved changes.
        </div>
        {loading ? (
          <div style={{ textAlign: "center", padding: 40 }}>
            <CircularProgress />
          </div>
        ) : (
          <>
            {/* All projects visible — wraps to multiple rows, no scroll arrows */}
            <div
              style={{
                display: "flex",
                flexWrap: "wrap",
                gap: 8,
                marginBottom: 16,
              }}
            >
              {tenants.map((t) => this.renderTab(t))}
            </div>
            {activeTenant && (
              <div
                className="config-sections-grid"
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(440px, 1fr))",
                  gap: 16,
                  alignItems: "start",
                }}
              >
                {SECTIONS.map((section) => (
                  <Paper key={section.title} style={{ overflow: "hidden" }}>
                    <div
                      style={{
                        padding: "10px 18px",
                        background: "#eef2f7",
                        borderBottom: "1px solid #d8e0ea",
                        borderLeft: "4px solid #1976d2",
                      }}
                    >
                      <div
                        style={{
                          fontSize: 13,
                          fontWeight: 700,
                          letterSpacing: 0.8,
                          textTransform: "uppercase",
                          color: "#37474f",
                        }}
                      >
                        {section.title}
                      </div>
                      <div style={{ fontSize: 12, color: "#78909c", marginTop: 2 }}>
                        {section.description}
                      </div>
                    </div>
                    <div style={{ padding: "4px 18px 10px" }}>
                      {section.items.map((item) => this.renderItem(item))}
                    </div>
                  </Paper>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    );
  }
}

export default withSnackbar(Configuration);
