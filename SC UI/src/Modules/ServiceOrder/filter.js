import React from "react";
import CommonFilter from "./../../Shared/Filter";
import { constants } from "./../../messages";

const STATUS_OPTIONS = [
  { name: "NEW", id: "NEW" },
  { name: "PARTIALLY COMPLETED", id: "PARTIALLY_COMPLETED" },
  { name: "COMPLETED", id: "COMPLETED" },
  { name: "CANCELLED", id: "CANCELLED" },
];

class Filter extends CommonFilter {
  format = constants.dateFormat;
  labelsOutside = true;

  renderFilter() {
    return (
      <div className="filter-container po-filter">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            <div className="filter-item">
              {this.renderFilterDate("Start Date", "startDate", false)}
            </div>
            <div className="filter-item">
              {this.renderFilterDate("End Date", "endDate", false)}
            </div>

            {this.renderAutoComplete(
              "Status",
              STATUS_OPTIONS,
              "status",
              (option) => option.name,
              true
            )}
            {this.renderAutoComplete(
              "Vendor",
              this.props.options?.vendors || [],
              "vendors",
              (option) => option.name,
              true
            )}
            {this.renderAutoComplete(
              "Project",
              this.props.options?.projects || [],
              "projectNames",
              (option) => option.name,
              true
            )}
            {this.renderAutoComplete(
              "Description",
              this.props.options?.descriptions || [],
              "descriptions",
              (option) => option,
              true
            )}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default Filter;
