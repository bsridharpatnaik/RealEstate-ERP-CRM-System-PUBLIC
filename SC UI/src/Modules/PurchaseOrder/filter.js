import React from "react";
import { messages } from "./../../messages";
import CommonFilter from "./../../Shared/Filter";
import { KeyboardDatePicker } from "@material-ui/pickers";
import moment from "moment";
import { constants } from "./../../messages";
import Checkbox from "@material-ui/core/Checkbox";
import FormControlLabel from "@material-ui/core/FormControlLabel";

class Filter extends CommonFilter {
  format = constants.dateFormat;
  labelsOutside = true;

  renderFilter() {
    return (
      <div className="filter-container po-filter">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {/* Date Range Filters */}
            <div className="filter-item">
              {this.renderFilterDate("Start Date", "startDate", false)}
            </div>
            <div className="filter-item">
              {this.renderFilterDate("End Date", "endDate", false)}
            </div>

            {/* Multi-select filters using dropdown options */}
            {this.renderAutoComplete(
              "Product Names",
              this.props.options?.product || [],
              "productNames",
              (option) => option.name,
              true
            )}

            {this.renderAutoComplete(
              "Product Codes",
              this.props.options?.productCodes || [],
              "productCodes",
              (option) => option.name,
              true
            )}

            {this.renderAutoComplete(
              "Status",
              (this.props.options?.purchaseOrderStatus || []).map(status => ({ name: status, id: status })),
              "status",
              (option) => option.name,
              true
            )}

            {this.renderAutoComplete(
              "Category Names",
              this.props.options?.category || [],
              "categoryNames",
              (option) => option.name,
              true
            )}

            {this.renderAutoComplete(
              "Suppliers",
              this.props.options?.supplier || [],
              "suppliers",
              (option) => option.name,
              true
            )}

            {this.renderAutoComplete(
              "Last Status Updated",
              this.props.options?.stalebuckets || [],
              "staleBuckets",
              (option) => option?.name ?? "",
              false
            )}

            {/* Status Changed filters (dashboard hyperlinking) - same row */}
            <div className="filter-status-changed-row">
              {this.renderAutoComplete(
                "Status Changed To",
                (this.props.options?.purchaseOrderStatus || []).map(status => ({ name: status, id: status })),
                "statusChangedTo",
                (option) => (option ? option.name : ""),
                false
              )}
              {this.renderFilterDate("Status Changed After", "statusChangedAfterDate", false)}
              {this.renderFilterDate("Status Changed Before", "statusChangedBeforeDate", false)}
            </div>

            {/* Priority Filter */}
            {this.renderAutoComplete(
              "Priority",
              ["CRITICAL", "HIGH", "MEDIUM", "NORMAL"].map(p => ({ name: p, id: p })),
              "priority",
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

            {/* SPL PO Filter */}
            <div className="filter-item filter-item-full">
              <span className="filter-field-label">SPL PO</span>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={!!this.filterData.isSpecialPo}
                    onChange={(e) => {
                      this.filterData.isSpecialPo = e.target.checked ? "true" : null;
                      this.setState({});  // force re-render to reflect checkbox state
                    }}
                    color="primary"
                  />
                }
                label="Show SPL POs only"
              />
            </div>
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default Filter;