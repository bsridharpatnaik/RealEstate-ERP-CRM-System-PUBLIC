import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
import { messages } from "./../../messages";

class filter extends CommonFilter {
  labelsOutside = true;

  renderFilter() {
    return (
      <div className="filter-container it-filter">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderTextField("Global Search", "globalSearch")}
            <div className="filter-dates">
              {this.renderFilterDate("Start Date", "startDate", false)}
              {this.renderFilterDate("End Date", "endDate", false)}
            </div>
            {this.renderAutoComplete(
              "Category Names",
              this.props.options?.category || [],
              "categoryNames",
              (option) => option?.name,
              true
            )}
            {this.renderAutoComplete(
              "Product Names",
              this.props.options?.product || [],
              "productNames",
              (option) => option?.name,
              true
            )}
            {this.renderAutoComplete(
              "Product Codes",
              this.props.options?.productCodes || [],
              "productCodes",
              (option) => option && (option.name || `Product Code ${option.id}`),
              true
            )}
            {this.renderAutoComplete(
              "Source Tenant",
              this.props.options?.tenants || [],
              "sourceTenant",
              // Options are {tenantCode, name} objects — show the human-readable name
              (option) => (typeof option === "object" ? option.name : String(option ?? "")),
              true
            )}
            {this.renderAutoComplete(
              "Target Tenant",
              this.props.options?.tenants || [],
              "targetTenant",
              (option) => (typeof option === "object" ? option.name : String(option ?? "")),
              true
            )}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;