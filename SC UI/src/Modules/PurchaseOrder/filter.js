import React from "react";
import { messages } from "./../../messages";
import CommonFilter from "./../../Shared/Filter";
import { constants } from "./../../messages";
import Switch from "@material-ui/core/Switch";
import FormControlLabel from "@material-ui/core/FormControlLabel";

class Filter extends CommonFilter {
  format = constants.dateFormat;
  labelsOutside = true;

  state = {
    ...this.state,
    showAdvanced: false,
  };

  renderFilter() {
    const { showAdvanced } = this.state;
    return (
      <div className="filter-container po-filter">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">

            {/* Row 1: Date Range */}
            <div className="filter-item">
              {this.renderFilterDate("Start Date", "startDate", false)}
            </div>
            <div className="filter-item">
              {this.renderFilterDate("End Date", "endDate", false)}
            </div>

            {/* Row 2: Product & Category */}
            {this.renderAutoComplete(
              "Product Names",
              this.props.options?.product || [],
              "productNames",
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

            {/* Row 3: Supplier & Status */}
            {this.renderAutoComplete(
              "Suppliers",
              this.props.options?.supplier || [],
              "suppliers",
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

            {/* Row 4: Project & Updated Within */}
            {this.renderAutoComplete(
              "Project",
              this.props.options?.projects || [],
              "projectNames",
              (option) => option.name,
              true
            )}
            {this.renderAutoComplete(
              "Updated Within",
              this.props.options?.stalebuckets || [],
              "staleBuckets",
              (option) => option?.name ?? "",
              false
            )}

            {/* Toggle switches row */}
            <div className="filter-item filter-item-full po-filter-toggles">
              <FormControlLabel
                control={
                  <Switch
                    checked={!!this.filterData.isSpecialPo}
                    onChange={(e) => {
                      this.filterData.isSpecialPo = e.target.checked ? "true" : null;
                      this.setState({});
                    }}
                    color="primary"
                  />
                }
                label="SPL POs only"
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={!!this.filterData.hasOverdueOnly}
                    onChange={(e) => {
                      this.filterData.hasOverdueOnly = e.target.checked ? "true" : null;
                      this.setState({});
                    }}
                    color="primary"
                  />
                }
                label="Overdue POs only"
              />
            </div>

            {/* Advanced section (collapsed by default) */}
            <div className="filter-item filter-item-full po-filter-advanced-toggle">
              <button
                type="button"
                className="po-filter-advanced-btn"
                onClick={() => this.setState({ showAdvanced: !showAdvanced })}
              >
                <span>Advanced</span>
                <span className="po-filter-advanced-arrow">{showAdvanced ? "▲" : "▼"}</span>
              </button>
            </div>

            {showAdvanced && (
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
            )}

          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default Filter;
