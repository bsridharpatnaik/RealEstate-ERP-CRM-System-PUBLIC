import React from "react";
import CommonFilter from "./../../Shared/Filter";

const BATCH_MODE_OPTIONS = ["NONE", "BATCH_ONLY", "BATCH_WITH_EXPIRY"];
const MANAGED_INVENTORY_OPTIONS = ["true", "false"];

class ProductFilter extends CommonFilter {
  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderAutoComplete(
              "Category",
              this.props.options?.category || [],
              "categoryNames",
              (option) => option["name"]
            )}
            {this.renderAutoComplete(
              "Product Code",
              this.props.options?.productCodes || [],
              "productCodes",
              (option) => option
            )}
            {this.renderAutoComplete(
              "Managed Inventory",
              MANAGED_INVENTORY_OPTIONS,
              "isManagedInventory"
            )}
            {this.renderAutoComplete(
              "Batch Tracking",
              BATCH_MODE_OPTIONS,
              "batchModes"
            )}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default ProductFilter;
