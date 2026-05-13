//react
import React from "react";
//third party
import TextField from "@material-ui/core/TextField";
import Autocomplete from "@material-ui/lab/Autocomplete";
import CircularProgress from "@material-ui/core/CircularProgress";
import Button from "@material-ui/core/Button";
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

class HistoricalPricing extends Common {
  title = messages.common.historicalPricing;

  state = {
    // Dropdown data
    productList: [],
    indentList: [],
    productsLoading: true,
    indentsLoading: true,
    // Selections
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
    this.fetchIndents();
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

  onProductsChange = (value) => {
    const prevEffective = new Set(this.getEffectiveProducts().map((p) => p.id));
    this.setState({ directProducts: value || [] }, () => {
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
        "Rate",
        "Discount %",
        "GST %",
        "Net Rate",
      ];
      const rows = rates.map((r) => [
        r.purchaseOrderId || "-",
        r.poDate || "-",
        r.supplierName || "-",
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
                  <th>Rate</th>
                  <th>Discount %</th>
                  <th>GST %</th>
                  <th>Net Rate</th>
                </tr>
              </thead>
              <tbody>
                {rates.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="hp-no-data">
                      No records found
                    </td>
                  </tr>
                ) : (
                  rates.map((r, idx) => (
                    <tr key={idx}>
                      <td>{r.purchaseOrderId || "-"}</td>
                      <td>{r.poDate || "-"}</td>
                      <td>{r.supplierName || "-"}</td>
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
      indentList,
      productsLoading,
      indentsLoading,
      directProducts,
      selectedIndents,
      indentFetchingMap,
    } = this.state;

    const effective = this.getEffectiveProducts();
    const anyIndentFetching = Object.values(indentFetchingMap).some(Boolean);

    return (
      <div className="page historical-pricing-page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.indentPO)}
          </div>
          {effective.length > 0 && (
            <div className="historical-pricing-download-btn">
              <Button
                variant="contained"
                color="primary"
                size="small"
                onClick={this.downloadExcel}
              >
                Download Excel
              </Button>
            </div>
          )}
        </div>

        <div className="historical-pricing-filters">
          {/* Indent selector */}
          {indentsLoading ? (
            <div className="historical-pricing-loading">
              <CircularProgress size={24} />
              <span>Loading indents…</span>
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
                  label="Select Indents"
                  InputLabelProps={{ shrink: true }}
                  placeholder={selectedIndents.length === 0 ? "Search by indent no…" : ""}
                />
              )}
            />
          )}

          {/* Product selector */}
          {productsLoading ? (
            <div className="historical-pricing-loading">
              <CircularProgress size={24} />
              <span>Loading products…</span>
            </div>
          ) : (
            <Autocomplete
              multiple
              id="historical-pricing-inventory-name"
              className="historical-pricing-inventory-name-field"
              options={productList}
              value={directProducts}
              onChange={(e, value) => this.onProductsChange(value)}
              getOptionLabel={(option) =>
                option.productCode
                  ? `${option.name} (${option.productCode})`
                  : option.name || ""
              }
              getOptionSelected={(option, value) => option.id === value.id}
              disableCloseOnSelect
              filterSelectedOptions
              renderInput={(params) => (
                <TextField
                  {...params}
                  variant="outlined"
                  margin="normal"
                  label="Select Products"
                  InputLabelProps={{ shrink: true }}
                  placeholder={directProducts.length === 0 ? "Search by name or code…" : ""}
                />
              )}
            />
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
