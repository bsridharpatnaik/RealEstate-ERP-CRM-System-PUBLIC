import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
class filter extends CommonFilter {
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
              "Machinery",
              this.props.options?.machinery,
              "machineryNames",
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
              "Location",
              this.props.options?.usagelocation,
              "usageLocationNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderTextField("Vehicle No", "vehicleNos")}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
