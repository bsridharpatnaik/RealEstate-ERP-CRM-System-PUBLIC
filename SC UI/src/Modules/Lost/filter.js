import React from "react";
import CommonFilter from "./../../Shared/Filter";
class filter extends CommonFilter {
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
            {this.renderAutoComplete(
              "Product",
              this.props.options?.product,
              "productNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Catgory",
              this.props.options?.category,
              "categoryNames ",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "warehouse",
              this.props.options?.warehouse,
              "warehouseNames",
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
