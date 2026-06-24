import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
import { messages } from "./../../messages";

class filter extends CommonFilter {
  labelsOutside = true;

  state = {
    ...this.state,
    showAdvanced: false,
  };

  renderFilter() {
    const { showAdvanced } = this.state;
    return (
      <div className="filter-container indent-filter">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">

            {/* Row 1: Date Range */}
            <div className="filter-item">
              {this.renderFilterDate("Start Date", "startDate")}
            </div>
            <div className="filter-item">
              {this.renderFilterDate("End Date", "endDate")}
            </div>

            {/* Row 2: Category & Product */}
            {this.renderAutoComplete(
              "Category",
              this.props.options?.category,
              "categoryNames",
              (option) => option["name"]
            )}
            {this.renderAutoComplete(
              "Product",
              this.props.options?.product,
              "productNames",
              (option) => option["name"]
            )}

            {/* Row 3: Indent Status & Line Item Status */}
            {this.renderAutoComplete(
              "Indent Status",
              this.props.options?.indentStatus,
              "indentStatus",
              (option) => option
            )}
            {this.renderAutoComplete(
              "Line Item Status",
              this.props.options?.indentLineItemStatus,
              "lineItemStatus",
              (option) => option
            )}

            {/* Row 4: Project (global only) & Updated Within */}
            {this.props.isGlobal && this.renderAutoComplete(
              "Project",
              this.props.options?.projects || [],
              "tenants",
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
            {this.renderAutoComplete(
              "Quote Requested",
              ["Yes", "No"],
              "hasQuoteRequested",
              (option) => option,
              false
            )}

            {/* Advanced toggle */}
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
              <>
                {this.renderAutoComplete(
                  "Product Code",
                  this.props.options?.productCodes || [],
                  "productCodes",
                  (option) => option && (option["name"] || `Product Code ${option["id"]}`)
                )}
                {this.renderAutoComplete(
                  "Required By",
                  this.props.options?.requiredByOptions || [],
                  "requiredByNames",
                  (option) => option["name"]
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
              </>
            )}

          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
