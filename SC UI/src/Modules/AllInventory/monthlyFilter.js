import { IconButton } from "@material-ui/core";
import React from "react";
import { messages } from "../../messages";
import { CloseIcon } from "../../Shared/Icons/Index";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
class monthlyFilter extends CommonFilter {
typeoptions = [
    { name: "Inward",       value: "Inward" },
    { name: "Transfer In",  value: "Transfer-In" },
    { name: "Transfer Out", value: "Transfer-Out" },
    { name: "Outward",      value: "Outward" },
    { name: "Lost/Damaged", value: "Lost-Damaged" },
    { name: "Excess Found", value: "Excess-Found" },
    { name: "All",          value: "" },
];
  renderFilter() {
    const { startDate, EndDate } = this.props.filterData;
    const disableDownloadButton = !(startDate && EndDate);
    return (
      <div className="filter-container">
        <div className="filter-heading">
          <span className="filter-title">Monthly Report</span>
          <IconButton
            aria-label="back"
            onClick={this.props.close}
            className="close-icon"
          >
            <CloseIcon />
          </IconButton>
        </div>
        {this.state.reset ? (
          <div className="filter-content">
            <div className="filter-dates">
              {this.renderFilterDate("Start Date *", "startDate")}
              {this.renderFilterDate("End Date *", "EndDate")}
            </div>
            {this.renderAutoComplete(
              "Product",
              this.props?.options?.product,
              "products",
              (option) => {
                return option["name"];
              }
            )}

            {this.renderAutoComplete(
              "Category",
              this.props?.options?.category,
              "categories",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Warehouse",
              this.props.options?.warehouse,
              "warehouses",
              (option) => {
                return option["name"];
              }
            )}
          </div>
        ) : null}
        {this.renderFooter(false, messages.common.export, disableDownloadButton, this.props.onReset)}
      </div>
    );
  }
}

export default monthlyFilter;
