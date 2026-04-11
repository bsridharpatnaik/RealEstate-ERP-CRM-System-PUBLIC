import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
import { messages } from "./../../messages";

class Filter extends CommonFilter {
  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderSwitch("Only with dead stock", "deadStockPresent")}
            {this.renderSwitch("Only low stock", "lowStock")}
            {this.renderAutoComplete(
              "Product Name",
              this.props.options?.productNames || [],
              "productNames",
              (option) => (typeof option === "object" && option?.name != null ? option.name : String(option ?? "")),
              true
            )}
            {this.renderAutoComplete(
              "Product Code",
              this.props.options?.productCodes || [],
              "productCodes",
              (option) =>
                option && (typeof option === "object" ? (option.name ?? `Product Code ${option.id ?? ""}`) : String(option)),
              true
            )}
            {this.renderAutoComplete(
              "Tenant",
              this.props.options?.tenants || [],
              "tenants",
              (option) => (typeof option === "object" && option?.name != null ? option.name : String(option ?? "")),
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
