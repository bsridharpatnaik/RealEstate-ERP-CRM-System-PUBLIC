import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
class filter extends CommonFilter {
  statusOptions = ["All", "High", "Low"];
  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            <div className="filter-dates full-width">
              {this.renderFilterDate("Closing Date", "closingDate", false)}
            </div>
            {this.renderAutoComplete(
              "Product",
              this.props.options?.product,
              "products",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Category",
              this.props.options?.category,
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
            {this.renderAutoComplete(
              "Stock Status",
              this.statusOptions,
              "stockStatus"
            )}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
