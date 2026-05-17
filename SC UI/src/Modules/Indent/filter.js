import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
import { messages } from "./../../messages";

class filter extends CommonFilter {
  labelsOutside = true;

  renderFilter() {
    return (
      <div className="filter-container indent-filter">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            <div className="filter-dates">
              {this.renderFilterDate("Start Date", "startDate")}
              {this.renderFilterDate("End Date", "endDate")}
            </div>
            {this.renderAutoComplete(
              "Category",
              this.props.options?.category,
              "categoryNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Product",
              this.props.options?.product,
              "productNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Product Code",
              this.props.options?.productCodes || [],
              "productCodes",
              (option) => {
                return option && (option["name"] || `Product Code ${option["id"]}`);
              }
            )}
            {this.renderAutoComplete(
              "Indent Status",
              this.props.options?.indentStatus,
              "indentStatus",
              (option) => {
                return option;
              }
            )}
            {this.renderAutoComplete(
              "Line Item Status",
              this.props.options?.indentLineItemStatus,
              "lineItemStatus",
              (option) => {
                return option;
              }
            )}
            {this.renderAutoComplete(
              "Last Status Updated",
              this.props.options?.stalebuckets || [],
              "staleBuckets",
              (option) => option?.name ?? "",
              false
            )}
            <div className="filter-status-changed-row">
              {this.renderAutoComplete(
                "Status Changed To",
                this.props.options?.indentStatus,
                "statusChangedTo",
                (option) => (option ?? ""),
                false
              )}
              {this.renderFilterDate("Status Changed After", "statusChangedAfterDate", false)}
              {this.renderFilterDate("Status Changed Before", "statusChangedBeforeDate", false)}
            </div>
            {this.props.isGlobal && this.renderAutoComplete(
              "Project",
              this.props.options?.projects || [],
              "tenants",
              (option) => option.name,
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




