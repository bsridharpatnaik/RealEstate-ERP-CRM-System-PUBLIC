import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
import { messages } from "./../../messages";
class filter extends CommonFilter {
  renderFilter() {
    console.log(this.props)
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderAutoComplete(
              messages.common.category,
              this.props.options?.category,
              "categoryNames",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              messages.common.product,
              this.props.options?.product,
              "productNames",
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
