import React from "react";
import CommonFilter from "./../../Shared/Filter";
import Switch from "@material-ui/core/Switch";
import "./style.scss";
import { messages } from "./../../messages";

class filter extends CommonFilter {
  renderToggle(label, fieldname) {
    const checked = !!this.filterData[fieldname];
    return (
      <div className="inward-filter-toggle-item">
        <Switch
          size="small"
          checked={checked}
          onChange={(e) => {
            this.filterData[fieldname] = e.target.checked ? "true" : undefined;
            this.setState({});
          }}
          color="primary"
        />
        <span className="inward-filter-toggle-label">{label}</span>
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
            <div className="inward-filter-grid">
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
                "Supplier",
                this.props.options?.supplier,
                "supplierNames",
                (option) => option["name"]
              )}
              {this.renderAutoComplete(
                "Warehouse",
                this.props.options?.warehouse,
                "warehouseNames",
                (option) => option["name"]
              )}
            </div>
            <div className="inward-filter-toggles">
              {this.renderToggle("Invoice Received", "invoiceReceived")}
              {this.renderToggle("Rejected Only", "showOnlyRejected")}
              {this.renderToggle("Missing Challan/Bill", "missingChallanBill")}
            </div>
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
