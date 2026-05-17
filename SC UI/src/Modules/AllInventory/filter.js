import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
class filter extends CommonFilter {
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
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            <div className="filter-dates">
              {this.renderFilterDate("Start Date", "startDate")}
              {this.renderFilterDate("End Date", "EndDate")}
            </div>
            {this.renderAutoComplete(
              "Type",
              this.typeoptions,
              "type",
              (option) => {
                return option["name"];
              }
            )}
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
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
