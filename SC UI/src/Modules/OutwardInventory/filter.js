import React from "react";
import CommonFilter from "./../../Shared/Filter";
import Switch from "@material-ui/core/Switch";
import "./style.scss";
import { messages } from "./../../messages";

class filter extends CommonFilter {
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
                this.props.options?.usagelocation,
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
