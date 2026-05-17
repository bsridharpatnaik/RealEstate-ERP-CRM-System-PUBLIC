import React from "react";
import { messages } from "./../../messages";
import CommonFilter from "./../../Shared/Filter";
import { apiEndpoints } from "./../../endpoints";
class filter extends CommonFilter {
  selectOptions = [
    { label: "All", value: "All" },
    { label: "Supplier", value: "SUPPLIER" },
    { label: "Contractor", value: "CONTRACTOR" },
  ];
  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderAutoCompleteWithSearch(
              messages.fields.contactName,
              apiEndpoints.contactNameSearch,
              "name"
            )}
            <div className="filter-mobile-contact">
              {this.renderTextField(messages.fields.mobileNo, "mobilenumber")}
              {this.renderAutoComplete(
                "Contact Type",
                this.selectOptions,
                "contacttype",
                (option) => {
                  return option["label"];
                }
              )}
            </div>
            {this.renderTextField(messages.fields.address, "address")}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
