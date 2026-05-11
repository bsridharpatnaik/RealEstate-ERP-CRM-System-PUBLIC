import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
import { messages } from "./../../messages";
class filter extends CommonFilter {
  invoiceOptions = ["true", "false"];

  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderTextField("Text Search", "textSearch")}
            <div className="filter-dates">
              {this.renderFilterDate("Start Date", "startDate")}
              {this.renderFilterDate("End Date", "endDate")}
            </div>
            {this.renderAutoComplete(
              messages.common.category,
              this.props.options?.category,
              "categoryNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              messages.common.inventory,
              this.props.options?.product,
              "productNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Contractor",
              this.props.options?.contractor,
              "contractorNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Warehouse",
              this.props.options?.warehouse,
              "warehouseNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              messages.common.location,
              this.props.options?.usagelocation,
              "usageLocation",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              messages.common.finalLocation,
              this.props.options?.usageArea,
              "usageArea",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Show only Rejected",
              this.invoiceOptions,
              "showOnlyRejected",
              undefined,
              false
            )}
            {this.renderAutoComplete(
              "Show only Returned",
              this.invoiceOptions,
              "showOnlyReturned",
              undefined,
              false
            )}
            {this.renderAutoComplete(
              "BOQ Bypassed",
              this.invoiceOptions,
              "boqBypassed",
              undefined,
              false
            )}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
