import React from "react";
import CommonFilter from "../../Shared/Filter";

const STATUS_OPTIONS = [
  "DRAFT", "OPEN", "PARTIALLY_FINALIZED", "FINALIZED",
  "PARTIALLY_ORDERED", "PO_COMPLETED", "CLOSED", "CANCELLED",
];

class QuoteComparisonFilter extends CommonFilter {
  labelsOutside = true;

  renderFilter() {
    const { options = {} } = this.props;
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content" style={{ padding: "16px 20px" }}>

            {this.renderTextField("Search (QC No. or Indent ID)", "search", 60)}

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
              <div>{this.renderFilterDate("Date From", "dateFrom")}</div>
              <div>{this.renderFilterDate("Date To", "dateTo")}</div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 8 }}>
              <div>
                {this.renderAutoComplete("Status", STATUS_OPTIONS, "status", (o) => o.replace(/_/g, " "), true)}
              </div>
              <div>
                {this.renderAutoComplete("Supplier Name", options.suppliers || [], "supplierName", (o) => o, true)}
              </div>
              {this.renderTextField("Created By", "createdBy", 60)}
            </div>

          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default QuoteComparisonFilter;
