//react
import React from "react";
//third party
import TextField from "@material-ui/core/TextField";
import Autocomplete from "@material-ui/lab/Autocomplete";
import CircularProgress from "@material-ui/core/CircularProgress";
import Button from "@material-ui/core/Button";
import Checkbox from "@material-ui/core/Checkbox";
import CheckBoxOutlineBlankIcon from "@material-ui/icons/CheckBoxOutlineBlank";
import CheckBoxIcon from "@material-ui/icons/CheckBox";
import IndeterminateCheckBoxIcon from "@material-ui/icons/IndeterminateCheckBox";
import * as XLSX from "xlsx";
//components
import Common from "./../../Shared/CommonIndex";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import { withSnackbar } from "notistack";
//style
import "./style.scss";

function renderOnTimeRate(rate) {
  if (rate === null || rate === undefined) return <span style={{ color: "#bbb", fontSize: 12 }}>N/A</span>;
  const color = rate >= 90 ? "#2e7d32" : rate >= 70 ? "#e65100" : "#b71c1c";
  const bg    = rate >= 90 ? "#e8f5e9"  : rate >= 70 ? "#fff3e0"  : "#ffebee";
  return (
    <span style={{ fontWeight: 700, color, background: bg, padding: "2px 7px", borderRadius: 4, fontSize: 12 }}>
      {rate.toFixed(1)}%
    </span>
  );
}

class HistoricalPricing extends Common {
  title = messages.common.historicalPricing;

  state = {
    // Dropdown data
    productList: [],          // all products (full list)
    categoryList: [],         // derived from productList on load
    indentList: [],
    productsLoading: true,
    indentsLoading: true,
    // Selections
    selectedCategory: null,   // { id, name } or null
    directProducts: [],       // products chosen directly in product dropdown
    selectedIndents: [],      // indents chosen in indent dropdown
    // Cache: indentId → [{ id, name, productCode }]
    indentProductsCache: {},
    indentFetchingMap: {},    // indentId → boolean (loading)
    // Pricing data
    productRatesMap: {},      // productId → rates[]
    ratesLoadingMap: {},      // productId → boolean
  };

  async componentDidMount() {
    this.fetchProducts();
    this.fetchCategories();
    this.fetchIndents();
  }

  async fetchCategories() {
    const response = await API.GET(apiEndpoints.getCategoryIdAndNames);
    if (response.success && Array.isArray(response.data)) {
      const categoryList = response.data
        .map((cat) => ({ id: cat.categoryId ?? cat.id, name: cat.categoryName ?? cat.name }))
        .sort((a, b) => a.name.localeCompare(b.name));
      this.setState({ categoryList });
    }
  }

  async fetchProducts() {
    this.setState({ productsLoading: true });
    const response = await API.GET(apiEndpoints.getProductForDropdown);
    if (response.success && Array.isArray(response.data)) {
      const productList = response.data.map((p) => ({
        id: p.productId,
        name: p.productName,
        productCode: p.productCode || "",
      }));
      this.setState({ productList, productsLoading: false });
    } else {
      this.setState({ productList: [], productsLoading: false });
    }
  }

  async fetchIndents() {
    this.setState({ indentsLoading: true });
    const response = await API.GET(apiEndpoints.getIndentsForDropdown);
    if (response.success && Array.isArray(response.data)) {
      const indentList = response.data.map((i) => ({
        id: i.indentId,
        label: `${i.indentId} — ${i.indentDate || ""} (${i.indentStatus || ""})`,
        indentDate: i.indentDate,
        indentStatus: i.indentStatus,
        tenant: i.tenant,
      }));
      this.setState({ indentList, indentsLoading: false });
    } else {
      this.setState({ indentList: [], indentsLoading: false });
    }
  }

  // Returns the deduplicated union of directly-selected products and
  // products derived from all currently-selected indents.
  getEffectiveProducts() {
    const { directProducts, selectedIndents, indentProductsCache } = this.state;
    const seen = new Set();
    const result = [];
    directProducts.forEach((p) => {
      if (!seen.has(p.id)) { seen.add(p.id); result.push(p); }
    });
    selectedIndents.forEach((indent) => {
      (indentProductsCache[indent.id] || []).forEach((p) => {
        if (!seen.has(p.id)) { seen.add(p.id); result.push(p); }
      });
    });
    return result;
  }

  // Sync rates maps with the current effective products:
  // fetch for newly added, clean up for removed.
  syncRatesForEffectiveProducts(prevEffectiveIds) {
    const effective = this.getEffectiveProducts();
    const newIds = new Set(effective.map((p) => p.id));

    // Clean up removed products
    const updatedRatesMap = { ...this.state.productRatesMap };
    const updatedLoadingMap = { ...this.state.ratesLoadingMap };
    prevEffectiveIds.forEach((id) => {
      if (!newIds.has(id)) {
        delete updatedRatesMap[id];
        delete updatedLoadingMap[id];
      }
    });
    this.setState(
      { productRatesMap: updatedRatesMap, ratesLoadingMap: updatedLoadingMap },
      () => {
        // Fetch for newly added products
        effective.forEach((p) => {
          if (!prevEffectiveIds.has(p.id)) {
            this.fetchPreviousRates(p.id);
          }
        });
      }
    );
  }

  onCategoryChange = async (value) => {
    const prevEffective = new Set(this.getEffectiveProducts().map((p) => p.id));
    const newCat = value || null;
    if (!newCat) {
      // Cleared — reload full product list, clear selection
      this.setState({ selectedCategory: null, directProducts: [], productsLoading: true }, async () => {
        await this.fetchProducts();
        this.syncRatesForEffectiveProducts(prevEffective);
      });
      return;
    }
    // Fetch products for this category, then auto-select all of them
    this.setState({ selectedCategory: newCat, productsLoading: true });
    const res = await API.GET(apiEndpoints.getProductForIndentByCategory(newCat.id));
    const categoryProducts = (res.success && Array.isArray(res.data))
      ? res.data.map((p) => ({
          id: p.productId,
          name: p.productName,
          productCode: p.productCode || "",
        }))
      : [];
    const autoSelected = categoryProducts.slice(0, this.MAX_PRODUCTS);
    if (categoryProducts.length > this.MAX_PRODUCTS) {
      this.props.enqueueSnackbar(
        `${categoryProducts.length} products in this category. Showing first ${this.MAX_PRODUCTS} — use the product dropdown to pick specific ones.`,
        { variant: "info", autoHideDuration: 5000 }
      );
    }
    this.setState({ productList: categoryProducts, directProducts: autoSelected, productsLoading: false }, () => {
      this.syncRatesForEffectiveProducts(prevEffective);
    });
  };

  MAX_PRODUCTS = 20;

  onProductsChange = (value) => {
    const selected = value || [];
    if (selected.length > this.MAX_PRODUCTS) {
      this.props.enqueueSnackbar(
        `Please select at most ${this.MAX_PRODUCTS} products at a time to avoid overloading the page.`,
        { variant: "warning" }
      );
      return;
    }
    const prevEffective = new Set(this.getEffectiveProducts().map((p) => p.id));
    this.setState({ directProducts: selected }, () => {
      this.syncRatesForEffectiveProducts(prevEffective);
    });
  };

  onIndentsChange = (value) => {
    const newSelected = value || [];
    const prevEffective = new Set(this.getEffectiveProducts().map((p) => p.id));
    const { selectedIndents, indentProductsCache, indentFetchingMap } = this.state;

    // Determine which indents were added
    const prevIndentIds = new Set(selectedIndents.map((i) => i.id));
    const addedIndents = newSelected.filter((i) => !prevIndentIds.has(i.id));

    // Mark newly added indents as fetching if not cached
    const newFetchingMap = { ...indentFetchingMap };
    addedIndents.forEach((indent) => {
      if (!indentProductsCache[indent.id]) {
        newFetchingMap[indent.id] = true;
      }
    });

    this.setState({ selectedIndents: newSelected, indentFetchingMap: newFetchingMap }, () => {
      // Fetch products for each newly added indent not yet cached
      addedIndents.forEach((indent) => {
        if (!indentProductsCache[indent.id]) {
          this.fetchProductsForIndent(indent.id, prevEffective);
        }
      });
      // If indents were only removed (no new fetches needed), sync immediately
      if (addedIndents.length === 0) {
        this.syncRatesForEffectiveProducts(prevEffective);
      }
    });
  };

  async fetchProductsForIndent(indentId, prevEffectiveIds) {
    const response = await API.GET(apiEndpoints.getIndentDetail + indentId);
    const products = [];
    if (response.success && response.data && Array.isArray(response.data.inventoryList)) {
      response.data.inventoryList.forEach((item) => {
        if (item.product) {
          products.push({
            id: item.product.productId,
            name: item.product.productName,
            productCode: item.product.productCode || "",
          });
        }
      });
    }

    this.setState(
      (prev) => ({
        indentProductsCache: { ...prev.indentProductsCache, [indentId]: products },
        indentFetchingMap: { ...prev.indentFetchingMap, [indentId]: false },
      }),
      () => this.syncRatesForEffectiveProducts(prevEffectiveIds)
    );
  }

  async fetchPreviousRates(productId) {
    this.setState((prev) => ({
      ratesLoadingMap: { ...prev.ratesLoadingMap, [productId]: true },
    }));
    const response = await API.GET(
      apiEndpoints.getPurchaseOrderPreviousRates(productId)
    );
    if (response.success && Array.isArray(response.data)) {
      this.setState((prev) => ({
        productRatesMap: { ...prev.productRatesMap, [productId]: response.data },
        ratesLoadingMap: { ...prev.ratesLoadingMap, [productId]: false },
      }));
    } else {
      this.setState((prev) => ({
        productRatesMap: { ...prev.productRatesMap, [productId]: [] },
        ratesLoadingMap: { ...prev.ratesLoadingMap, [productId]: false },
      }));
      if (!response.success) {
        this.props.enqueueSnackbar(
          response.errorMessage || "Failed to load previous rates",
          { variant: "error" }
        );
      }
    }
  }

  formatNum = (val) =>
    val != null ? Number(val).toFixed(2) : "-";

  computeNetRate = (r) => {
    if (r.rate == null) return "-";
    let net = r.rate;
    if (r.discountPercent) net = net * (1 - r.discountPercent / 100);
    if (r.gstPercent) net = net * (1 + r.gstPercent / 100);
    return Number(net).toFixed(2);
  };

  downloadExcel = () => {
    const effective = this.getEffectiveProducts();
    const { productRatesMap } = this.state;
    const wb = XLSX.utils.book_new();

    effective.forEach((product) => {
      const rates = productRatesMap[product.id] || [];
      const sheetName = (product.name || `Product_${product.id}`).substring(0, 31);
      const headers = [
        "Purchase Order",
        "PO Date",
        "Supplier",
        "On-time %",
        "Rate",
        "Discount %",
        "GST %",
        "Net Rate",
      ];
      const rows = rates.map((r) => [
        r.purchaseOrderId || "-",
        r.poDate || "-",
        r.supplierName || "-",
        r.onTimeRate != null ? `${r.onTimeRate.toFixed(1)}%` : "N/A",
        r.rate != null ? Number(r.rate).toFixed(2) : "-",
        r.discountPercent != null ? Number(r.discountPercent).toFixed(2) : "-",
        r.gstPercent != null ? Number(r.gstPercent).toFixed(2) : "-",
        this.computeNetRate(r),
      ]);
      const ws = XLSX.utils.aoa_to_sheet([headers, ...rows]);
      XLSX.utils.book_append_sheet(wb, ws, sheetName);
    });

    XLSX.writeFile(wb, "Historical_Pricing.xlsx");
  };

  renderProductTable(product) {
    const { productRatesMap, ratesLoadingMap } = this.state;
    const isLoading = ratesLoadingMap[product.id];
    const rates = productRatesMap[product.id] || [];

    return (
      <div key={product.id} className="historical-pricing-product-section">
        <div className="product-section-header">
          {product.name}
          {product.productCode ? (
            <span className="product-code"> ({product.productCode})</span>
          ) : null}
        </div>

        {isLoading ? (
          <div className="historical-pricing-loading">
            <CircularProgress size={22} />
            <span>Loading rates…</span>
          </div>
        ) : (
          <div className="list-section historical-pricing-table">
            <table className="hp-table">
              <thead>
                <tr>
                  <th>Purchase Order</th>
                  <th>PO Date</th>
                  <th>Supplier</th>
                  <th>On-time %</th>
                  <th>Rate</th>
                  <th>Discount %</th>
                  <th>GST %</th>
                  <th>Net Rate</th>
                </tr>
              </thead>
              <tbody>
                {rates.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="hp-no-data">
                      No records found
                    </td>
                  </tr>
                ) : (
                  rates.map((r, idx) => (
                    <tr key={idx}>
                      <td>{r.purchaseOrderId || "-"}</td>
                      <td>{r.poDate || "-"}</td>
                      <td>{r.supplierName || "-"}</td>
                      <td>{renderOnTimeRate(r.onTimeRate)}</td>
                      <td>{this.formatNum(r.rate)}</td>
                      <td>{this.formatNum(r.discountPercent)}</td>
                      <td>{this.formatNum(r.gstPercent)}</td>
                      <td>{this.computeNetRate(r)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }

  render() {
    const {
      productList,
      categoryList,
      indentList,
      productsLoading,
      indentsLoading,
      selectedCategory,
      directProducts,
      selectedIndents,
      indentFetchingMap,
    } = this.state;

    const effective = this.getEffectiveProducts();
    const anyIndentFetching = Object.values(indentFetchingMap).some(Boolean);
    const hasAnySelection = effective.length > 0 || selectedIndents.length > 0 || selectedCategory;

    // productList is already filtered when category is selected (via onCategoryChange fetch)
    const filteredProductList = productList;

    return (
      <div className="page historical-pricing-page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.indentPO)}
          </div>
          {effective.length > 0 && (
            <div className="historical-pricing-download-btn">
              <Button variant="contained" color="primary" size="small" onClick={this.downloadExcel}>
                Download Excel
              </Button>
            </div>
          )}
        </div>

        {/* Filter bar — Category → Product → Indent */}
        <div className="historical-pricing-filters">
          {/* Category */}
          <Autocomplete
            id="historical-pricing-category"
            className="historical-pricing-category-field"
            options={categoryList}
            value={selectedCategory}
            onChange={(e, value) => this.onCategoryChange(value)}
            getOptionLabel={(option) => option.name || ""}
            getOptionSelected={(option, value) => option.id === value.id}
            renderInput={(params) => (
              <TextField
                {...params}
                variant="outlined"
                margin="normal"
                label="Category"
                InputLabelProps={{ shrink: true }}
                placeholder="All categories"
              />
            )}
          />

          {/* Product — checkbox multi-select with Select All */}
          {productsLoading ? (
            <div className="historical-pricing-loading">
              <CircularProgress size={24} /><span>Loading…</span>
            </div>
          ) : (() => {
            const allSelected = filteredProductList.length > 0 && directProducts.length === filteredProductList.length;
            const someSelected = directProducts.length > 0 && !allSelected;
            const SELECT_ALL_OPT = { id: "__select_all__", name: "__select_all__" };
            const optionsWithSelectAll = filteredProductList.length > 0
              ? [SELECT_ALL_OPT, ...filteredProductList]
              : filteredProductList;
            return (
              <Autocomplete
                multiple
                id="historical-pricing-inventory-name"
                className="historical-pricing-inventory-name-field"
                options={optionsWithSelectAll}
                value={directProducts}
                onChange={(e, newValue) => {
                  if (newValue.some((v) => v.id === "__select_all__")) {
                    // Toggle: if all selected → deselect all, else select all
                    this.onProductsChange(allSelected ? [] : filteredProductList.slice(0, this.MAX_PRODUCTS));
                  } else {
                    this.onProductsChange(newValue);
                  }
                }}
                getOptionLabel={(option) =>
                  option.id === "__select_all__" ? "Select All" :
                  option.productCode ? `${option.name} (${option.productCode})` : option.name || ""
                }
                getOptionSelected={(option, value) => option.id === value.id}
                disableCloseOnSelect
                disableClearable={false}
                renderTags={() => (
                  <span style={{ paddingLeft: 8, color: "#333", fontSize: 14 }}>
                    {directProducts.length === 0
                      ? ""
                      : directProducts.length === filteredProductList.length
                      ? `All ${directProducts.length} products selected`
                      : `${directProducts.length} product${directProducts.length > 1 ? "s" : ""} selected`}
                  </span>
                )}
                renderOption={(option, { selected }) => {
                  if (option.id === "__select_all__") {
                    return (
                      <div style={{ display: "flex", alignItems: "center", fontWeight: 600 }}>
                        <Checkbox
                          icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                          checkedIcon={<CheckBoxIcon fontSize="small" />}
                          indeterminateIcon={<IndeterminateCheckBoxIcon fontSize="small" />}
                          checked={allSelected}
                          indeterminate={someSelected}
                          style={{ marginRight: 8, color: "#1565c0" }}
                        />
                        {allSelected ? "Deselect All" : filteredProductList.length > this.MAX_PRODUCTS ? `Select All (max ${this.MAX_PRODUCTS})` : "Select All"}
                      </div>
                    );
                  }
                  return (
                    <div style={{ display: "flex", alignItems: "center" }}>
                      <Checkbox
                        icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                        checkedIcon={<CheckBoxIcon fontSize="small" />}
                        checked={selected}
                        style={{ marginRight: 8 }}
                      />
                      <span style={{ fontSize: 13 }}>
                        {option.name}
                        {option.productCode && (
                          <span style={{ color: "#999", marginLeft: 6, fontSize: 11 }}>
                            ({option.productCode})
                          </span>
                        )}
                      </span>
                    </div>
                  );
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    variant="outlined"
                    margin="normal"
                    label="Products"
                    InputLabelProps={{ shrink: true }}
                    placeholder={directProducts.length === 0
                      ? selectedCategory ? `Search in ${selectedCategory.name}…` : "Search by name or code…"
                      : ""}
                  />
                )}
              />
            );
          })()}

          {/* Indent — pre-fills products from indent */}
          {indentsLoading ? (
            <div className="historical-pricing-loading">
              <CircularProgress size={24} /><span>Loading indents…</span>
            </div>
          ) : (
            <Autocomplete
              multiple
              id="historical-pricing-indent"
              className="historical-pricing-indent-field"
              options={indentList}
              value={selectedIndents}
              onChange={(e, value) => this.onIndentsChange(value)}
              getOptionLabel={(option) => option.label || option.id || ""}
              getOptionSelected={(option, value) => option.id === value.id}
              disableCloseOnSelect
              filterSelectedOptions
              renderInput={(params) => (
                <TextField
                  {...params}
                  variant="outlined"
                  margin="normal"
                  label="Indents (optional)"
                  InputLabelProps={{ shrink: true }}
                  placeholder={selectedIndents.length === 0 ? "Auto-fill products from indent…" : ""}
                />
              )}
            />
          )}

          {/* Clear all */}
          {hasAnySelection && (
            <div style={{ display: "flex", alignItems: "center", paddingTop: 8 }}>
              <Button
                size="small"
                style={{ color: "#999", textTransform: "none", fontSize: 12 }}
                onClick={() => {
                  const prevEffective = new Set(this.getEffectiveProducts().map((p) => p.id));
                  this.setState(
                    { selectedCategory: null, directProducts: [], selectedIndents: [], indentProductsCache: {}, indentFetchingMap: {} },
                    () => this.syncRatesForEffectiveProducts(prevEffective)
                  );
                }}
              >
                Clear all
              </Button>
            </div>
          )}
        </div>

        {anyIndentFetching && (
          <div className="historical-pricing-loading">
            <CircularProgress size={22} />
            <span>Fetching products from selected indents…</span>
          </div>
        )}

        {effective.map((product) => this.renderProductTable(product))}
      </div>
    );
  }
}

export default withSnackbar(HistoricalPricing);
