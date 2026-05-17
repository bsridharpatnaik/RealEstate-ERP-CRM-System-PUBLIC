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
              "Supplier",
              this.props.options?.supplier,
              "supplierNames",
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
              "Invoice Received",
              this.invoiceOptions,
              "invoiceReceived"
            )}
            {this.renderAutoComplete(
              "Show only Rejected",
              this.invoiceOptions,
              "showOnlyRejected",
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
