import React from "react";
import CommonFilter from "./../../Shared/Filter";
import "./style.scss";
import { messages } from "./../../messages";

class Filter extends CommonFilter {
  // selected category NAMES
  getSelectedCategoryNames() {
    const raw = this.filterData.categoryNames;
    const arr = raw == null ? [] : Array.isArray(raw) ? raw : [raw];
    return arr.map((c) => (typeof c === "object" && c?.name != null ? c.name : c));
  }

  // products filtered by selected categories; all products when no category selected
  getProductOptions() {
    const all = this.props.options?.productNames || [];
    const cats = this.getSelectedCategoryNames();
    if (cats.length === 0) return all;
    const withCat = this.props.options?.productWithCategory || [];
    return withCat
      .filter((p) => cats.includes(p.categoryName))
      .map((p) => ({ id: p.id, name: p.name }));
  }

  // on category change: drop selected products no longer valid, then re-render
  handleCategoryChange = () => {
    const allowed = this.getProductOptions().map((o) => o.name);
    const raw = this.filterData.productNames;
    const sel = raw == null ? [] : Array.isArray(raw) ? raw : [raw];
    if (this.getSelectedCategoryNames().length > 0) {
      this.filterData.productNames = sel.filter((s) =>
        allowed.includes(typeof s === "object" && s?.name != null ? s.name : s)
      );
    }
    // remount filter body so the Product dropdown picks up new options + pruned chips
    this.setState({ reset: false }, () => this.setState({ reset: true }));
  };

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
              this.getProductOptions(),
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
              "Category",
              this.props.options?.categoryNames || [],
              "categoryNames",
              (option) => (typeof option === "object" && option?.name != null ? option.name : String(option ?? "")),
              true,
              false,
              this.handleCategoryChange
            )}
            {this.renderAutoComplete(
              "Project",
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
