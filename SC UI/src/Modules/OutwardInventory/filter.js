import React from "react";
import CommonFilter from "./../../Shared/Filter";
import Switch from "@material-ui/core/Switch";
import "./style.scss";
import { messages } from "./../../messages";

class filter extends CommonFilter {
  // selected structure-type NAMES (building_type names)
  getSelectedTypeNames() {
    const raw = this.filterData.structureTypes;
    const arr = raw == null ? [] : Array.isArray(raw) ? raw : [raw];
    return arr.map((t) => (typeof t === "object" ? t.name : t));
  }

  // structures filtered by selected types; all structures when no type selected
  getStructureOptions() {
    const all = Array.isArray(this.props.options?.usagelocation)
      ? this.props.options.usagelocation
      : [];
    const types = this.getSelectedTypeNames();
    if (types.length === 0) return all;
    const withType = Array.isArray(this.props.options?.usagelocationWithType)
      ? this.props.options.usagelocationWithType
      : [];
    return withType
      .filter((l) => types.includes(l.typeName))
      .map((l) => ({ id: l.id, name: l.name }));
  }

  // on structure-type change: drop selected structures no longer valid, then re-render
  handleStructureTypeChange = () => {
    const allowed = this.getStructureOptions().map((o) => o.name);
    const raw = this.filterData.usageLocation;
    const sel = raw == null ? [] : Array.isArray(raw) ? raw : [raw];
    if (this.getSelectedTypeNames().length > 0) {
      this.filterData.usageLocation = sel.filter((s) =>
        allowed.includes(typeof s === "object" ? s.name : s)
      );
    }
    // remount filter body so Structure dropdown picks up new options + pruned chips
    this.setState({ reset: false }, () => this.setState({ reset: true }));
  };

  renderToggle(label, fieldname) {
    const checked = !!this.filterData[fieldname];
    return (
      <div className="outward-filter-toggle-item">
        <Switch
          size="small"
          checked={checked}
          onChange={(e) => {
            this.filterData[fieldname] = e.target.checked ? "true" : undefined;
            this.setState({});
          }}
          color="primary"
        />
        <span className="outward-filter-toggle-label">{label}</span>
      </div>
    );
  }

  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            <div className="filter-dates">
              {this.renderFilterDate("Start Date", "startDate")}
              {this.renderFilterDate("End Date", "endDate")}
            </div>
            <div className="outward-filter-grid">
              {this.renderAutoComplete(
                messages.common.category,
                this.props.options?.category,
                "categoryNames",
                (option) => option["name"]
              )}
              {this.renderAutoComplete(
                messages.common.inventory,
                this.props.options?.product,
                "productNames",
                (option) => option["name"]
              )}
              {this.renderAutoComplete(
                "Contractor",
                this.props.options?.contractor,
                "contractorNames",
                (option) => option["name"]
              )}
              {this.renderAutoComplete(
                "Warehouse",
                this.props.options?.warehouse,
                "warehouseNames",
                (option) => option["name"]
              )}
              {this.renderAutoComplete(
                messages.common.location,
                this.getStructureOptions(),
                "usageLocation",
                (option) => option["name"]
              )}
              {this.renderAutoComplete(
                messages.common.finalLocation,
                this.props.options?.usageArea,
                "usageArea",
                (option) => option["name"]
              )}
              {this.renderAutoComplete(
                "Structure Type",
                this.props.options?.buildingtype,
                "structureTypes",
                (option) => option["name"],
                true,
                false,
                this.handleStructureTypeChange
              )}
              {this.renderAutoComplete(
                "Requested By",
                this.props.options?.requestedByOptions,
                "requestedByNames",
                (option) => option["name"]
              )}
              {this.renderAutoComplete(
                "Issued By",
                this.props.options?.issuedByOptions,
                "issuedByNames",
                (option) => option["name"]
              )}
            </div>
            <div className="outward-filter-toggles">
              {this.renderToggle("Rejected Only", "showOnlyRejected")}
              {this.renderToggle("Returned Only", "showOnlyReturned")}
              {this.renderToggle("BOQ Bypassed", "boqBypassed")}
              {this.renderToggle("FIFO Override", "fifoOverride")}
            </div>
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
