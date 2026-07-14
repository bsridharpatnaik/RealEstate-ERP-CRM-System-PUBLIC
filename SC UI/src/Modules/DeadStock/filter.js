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

  // product codes filtered by selected categories; all codes when no category selected
  getCodeOptions() {
    const all = this.props.options?.productCodes || [];
    const cats = this.getSelectedCategoryNames();
    if (cats.length === 0) return all;
    const withCat = this.props.options?.productWithCategory || [];
    return withCat
      .filter((p) => cats.includes(p.categoryName) && p.productCode != null)
      .map((p) => ({ id: p.id, name: p.productCode }));
  }

  // on category change: drop selected products/codes no longer valid, then re-render
  handleCategoryChange = () => {
    const hasCats = this.getSelectedCategoryNames().length > 0;
    if (hasCats) {
      const allowedNames = this.getProductOptions().map((o) => o.name);
      const rawN = this.filterData.productNames;
      const selN = rawN == null ? [] : Array.isArray(rawN) ? rawN : [rawN];
      this.filterData.productNames = selN.filter((s) =>
        allowedNames.includes(typeof s === "object" && s?.name != null ? s.name : s)
      );

      const allowedCodes = this.getCodeOptions().map((o) =>
        typeof o === "object" && o?.name != null ? o.name : o
      );
      const rawC = this.filterData.productCodes;
      const selC = rawC == null ? [] : Array.isArray(rawC) ? rawC : [rawC];
      this.filterData.productCodes = selC.filter((s) =>
        allowedCodes.includes(typeof s === "object" && s?.name != null ? s.name : s)
      );
    }
    // remount filter body so the dropdowns pick up new options + pruned chips
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
              "Category",
              this.props.options?.categoryNames || [],
              "categoryNames",
              (option) => (typeof option === "object" && option?.name != null ? option.name : String(option ?? "")),
              true,
              false,
              this.handleCategoryChange
            )}
            {this.renderAutoComplete(
              "Product Name",
              this.getProductOptions(),
              "productNames",
              (option) => (typeof option === "object" && option?.name != null ? option.name : String(option ?? "")),
              true
            )}
            {this.renderAutoComplete(
              "Product Code",
              this.getCodeOptions(),
              "productCodes",
              (option) =>
                option && (typeof option === "object" ? (option.name ?? `Product Code ${option.id ?? ""}`) : String(option)),
              true
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
