//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";
import PublishIcon from "@material-ui/icons/Publish";
import SettingsIcon from "@material-ui/icons/Settings";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import IconButtons from "./../../Shared/Button/IconButtons.js";

//component
import Table from "./../../Shared/Table";
import TenantReorderConfig from "./TenantReorderConfig";
import Button from "./../../Shared/Button";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";
import UnitConversionConfig from "./UnitConversionConfig";
import SwapHorizIcon from "@material-ui/icons/SwapHoriz";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import * as XLSX from "xlsx";
import { messages } from "./../../messages";
import { API } from "./../../axios";
import { canEditInventoryModules } from "./../../helper";

class List extends ListCommon {
  deleteKey = "productId";
  deleteUrl = apiEndpoints.individualProduct;
  title = messages.common.product;

  filterData = {};
  filterDropdowns = { category: [], productCodes: [] };

  state = {
    data: [],
    options: [],
    tenantConfigProduct: null,
    unitConversionProduct: null,
    importDialogOpen: false,
    importResult: null,
    importLoading: false,
    filterOpen: false,
  };
  importFileRef = React.createRef();
  tableData = {
    headers: [
      messages.common.inventory,
      "Product Code",
      messages.common.description,
      "Reorder Level",
      messages.fields.measurementUnit,
      messages.fields.categoryName,
      "Managed Inventory",
      "Batch Tracking",
    ],
    keys: [
      "productName",
      "productCode",
      "productDescription",
      "reorderQuantity",
      "measurementUnit",
      "category",
      "isManagedInventory",
      "batchMode",
    ],
  };
  // For product listing, don't send isManagedInventory parameter to get both true and false results
  url = apiEndpoints.getProductWithManagedInventory();
  exportUrl = exportURL.getProductWithManagedInventory();
  exportFile = messages.exportFiles.product;
  componentDidMount() {
    this.filterRef = React.createRef();
    this.search();
    this.loadFilterDropdowns();
  }

  async loadFilterDropdowns() {
    // Load categories
    const catRes = await API.GET(apiEndpoints.getCategoryNames);
    // Load all product codes via product dropdown endpoint
    const prodRes = await API.GET(apiEndpoints.getProductForDropdown);
    const categories = catRes.success ? (catRes.data || []) : [];
    const productCodes = prodRes.success
      ? [...new Set((prodRes.data || []).map((p) => p.productCode).filter(Boolean))].sort()
      : [];
    this.filterDropdowns = { category: categories, productCodes };
  }

  prepareRequestBody() {
    let params = {};
    params.filterData = [];

    if (this.searchValue && this.searchValue.length) {
      params.filterData.push({ attrName: "name", attrValue: this.searchValue });
    }

    if (this.filterData) {
      // categoryNames — extract name from object
      if (this.filterData.categoryNames && this.filterData.categoryNames.length) {
        params.filterData.push({
          attrName: "categoryNames",
          attrValue: this.filterData.categoryNames.map((c) =>
            typeof c === "object" ? c.name : c
          ),
        });
      }
      // productCodes — plain strings
      if (this.filterData.productCodes && this.filterData.productCodes.length) {
        params.filterData.push({
          attrName: "productCodes",
          attrValue: Array.isArray(this.filterData.productCodes)
            ? this.filterData.productCodes
            : [this.filterData.productCodes],
        });
      }
      // isManagedInventory — "true"/"false" strings
      if (this.filterData.isManagedInventory && this.filterData.isManagedInventory.length) {
        params.filterData.push({
          attrName: "isManagedInventory",
          attrValue: Array.isArray(this.filterData.isManagedInventory)
            ? this.filterData.isManagedInventory
            : [this.filterData.isManagedInventory],
        });
      }
      // batchModes — enum strings
      if (this.filterData.batchModes && this.filterData.batchModes.length) {
        params.filterData.push({
          attrName: "batchModes",
          attrValue: Array.isArray(this.filterData.batchModes)
            ? this.filterData.batchModes
            : [this.filterData.batchModes],
        });
      }
    }

    return params;
  }
  getExportData(response) {
    return response.data.content.map((item) => ({
      "Product Name": item.productName,
      "Product Code": item.productCode,
      "Description": item.productDescription,
      "Reorder Level": item.reorderQuantity,
      "Measurement Unit": item.measurementUnit,
      "Category": item.category?.categoryName || "",
      "Managed Inventory": item.isManagedInventory ? "Yes" : "No",
      "Can Expire": item.isExpirable ? "Yes" : "No",
      "Batch Tracking": item.batchMode || "NONE",
      "Lead Time (Days)": item.leadTimeDays != null ? item.leadTimeDays : "",
    }));
  }

  async handleImportFile(e) {
    const file = e.target.files[0];
    if (!file) return;
    // Reset input so same file can be re-selected
    e.target.value = "";
    this.setState({ importLoading: true, importResult: null });
    try {
      const formData = new FormData();
      formData.append("file", file);
      const response = await API.POSTMultipart(apiEndpoints.importProducts, formData);
      if (response.success) {
        this.setState({ importResult: response.data, importLoading: false });
        this.search(0);
      } else {
        this.setState({ importLoading: false });
        this.props.enqueueSnackbar(response.errorMessage || "Import failed", { variant: "error" });
      }
    } catch (err) {
      this.setState({ importLoading: false });
      this.props.enqueueSnackbar("Import failed", { variant: "error" });
    }
  }

  async exportToCSV() {
    const response = await this.getExportAPIData();
    if (!response.success) {
      this.props.enqueueSnackbar(response.errorMessage, { variant: "error" });
      return;
    }
    const data = this.getExportData(response);
    const ws = XLSX.utils.json_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "Products");
    XLSX.writeFile(wb, "product.xlsx");
  }
  async search(page = 0) {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);

    if (response.success) {
      let data = response.data.content;
      data = data.map((item) => ({
        ...item,
        category: item.category?.categoryName || "",
        isManagedInventory: item.isManagedInventory ? "Yes" : "No",
        batchMode: item.batchMode || "NONE",
      }));
      this.setState({
        data: data,
        pages: response.data.totalPages,
        options: response.data.productAndCategoryNames || [],
      });
    }
  }

  render() {
    const { tenantConfigProduct, importDialogOpen, importResult, importLoading } = this.state;

    const customActions = [
      // Reorder Overrides — accessible to all roles
      {
        key: "tenant-reorder",
        title: "Reorder Overrides",
        icon: <SettingsIcon style={{ fontSize: 18 }} />,
        onClick: (row) => this.setState({ tenantConfigProduct: row }),
      },
      ...(canEditInventoryModules()
        ? [
            {
              key: "unit-conversions",
              title: "Billing Unit Conversions",
              icon: <SwapHorizIcon style={{ fontSize: 18 }} />,
              onClick: (row) => this.setState({ unitConversionProduct: row }),
            },
          ]
        : []),
    ];

    return (
      <div className="list-section">
        <div className="filter-section">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              this.search(0);
            }}
          >
            {this.renderAutoSearch(
              apiEndpoints.productGlobalSearch,
              "Search by Inventory Name or Category Name"
            )}
          </form>
          <div className="top-button-wrapper">
            {this.renderExport()}
            <IconButtons
              onClick={() => this.setState({ filterOpen: true })}
              buttonClass="filterIcon"
              innerRef={this.filterRef}
              label="Filter"
              icon={"FilterSVG"}
            />
            {canEditInventoryModules() && (
              <>
                <input
                  type="file"
                  accept=".xlsx"
                  ref={this.importFileRef}
                  style={{ display: "none" }}
                  onChange={(e) => this.handleImportFile(e)}
                />
                <Button
                  buttonClass="blue"
                  label="Import"
                  icon={<PublishIcon style={{ fontSize: 18, marginRight: 4 }} />}
                  onClick={() => {
                    this.setState({ importDialogOpen: true, importResult: null });
                  }}
                />
              </>
            )}
          </div>
          <Popper
            open={this.state.filterOpen}
            anchorEl={this.filterRef && this.filterRef.current}
            placement="bottom-end"
          >
            <Filter
              filterData={this.filterData}
              options={this.filterDropdowns}
              search={(data) => {
                this.filterData = data;
                this.setState({ filterOpen: false });
                this.search(0);
              }}
              close={() => this.setState({ filterOpen: false })}
            />
          </Popper>
        </div>
        {this.state.isLoading ? (
          this.renderLoader()
        ) : (
          <Table
            tableData={this.tableData}
            rows={this.state.data}
            edit={this.props.edit}
            delete={(row) => this.delete(row)}
            sortby={this.sortby}
            sortkey={this.sortkey}
            search={(sortkey, sortby) => {
              this.sortby = sortby;
              this.sortkey = sortkey;
              this.search();
            }}
            hideedit={this.props.hideedit}
            hidedelete={!canEditInventoryModules()}
            customActions={customActions}
          />
        )}
        {this.renderPagination()}

        {tenantConfigProduct && (
          <TenantReorderConfig
            product={tenantConfigProduct}
            onClose={() => this.setState({ tenantConfigProduct: null })}
          />
        )}

        {/* ── Import Dialog ── */}
        <Dialog
          open={importDialogOpen}
          maxWidth="sm"
          fullWidth
          onClose={() => this.setState({ importDialogOpen: false, importResult: null })}
        >
          <DialogTitle>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span>{importResult ? "Import Results" : "Import Products"}</span>
              <IconButton size="small" onClick={() => this.setState({ importDialogOpen: false, importResult: null })}>
                <CloseIcon />
              </IconButton>
            </div>
          </DialogTitle>
          <DialogContent dividers>
            {!importResult ? (
              <div>
                <p style={{ marginTop: 0, marginBottom: 12 }}>
                  Upload the product Excel file (same format as the download).
                  Only these columns are updated:
                </p>
                <ul style={{ marginBottom: 16, paddingLeft: 20 }}>
                  <li><strong>Product Name</strong></li>
                  <li><strong>Reorder Level</strong></li>
                  <li><strong>Managed Inventory</strong> — values: <code>Yes</code> / <code>No</code></li>
                  <li>
                    <strong>Batch Tracking</strong> — values:{" "}
                    <code>NONE</code> / <code>BATCH_ONLY</code> / <code>BATCH_WITH_EXPIRY</code>
                  </li>
                </ul>
                <p style={{ marginBottom: 8, color: "#555", fontSize: 13 }}>
                  Products are matched by <strong>Product Code</strong> first, then <strong>Product Name</strong>.
                  Rows with no changes are skipped. Errors are collected and shown after import.
                </p>
                {importLoading && (
                  <p style={{ color: "#1976d2" }}>Uploading and processing...</p>
                )}
              </div>
            ) : (
              <div>
                <div style={{ display: "flex", gap: 24, marginBottom: 16 }}>
                  <div style={{ background: "#e8f5e9", padding: "12px 20px", borderRadius: 6 }}>
                    <div style={{ fontSize: 24, fontWeight: 700, color: "#2e7d32" }}>{importResult.updated}</div>
                    <div style={{ fontSize: 13, color: "#555" }}>Updated</div>
                  </div>
                  <div style={{ background: "#fff3e0", padding: "12px 20px", borderRadius: 6 }}>
                    <div style={{ fontSize: 24, fontWeight: 700, color: "#e65100" }}>{importResult.skipped}</div>
                    <div style={{ fontSize: 13, color: "#555" }}>Skipped</div>
                  </div>
                </div>

                {importResult.updatedItems && importResult.updatedItems.length > 0 && (
                  <div style={{ marginBottom: 16 }}>
                    <div style={{ fontWeight: 600, marginBottom: 6, color: "#2e7d32" }}>
                      Updated products ({importResult.updatedItems.length}):
                    </div>
                    <div style={{ maxHeight: 180, overflowY: "auto", background: "#f1f8e9", border: "1px solid #c5e1a5", borderRadius: 4, padding: "8px 12px" }}>
                      {importResult.updatedItems.map((item, idx) => (
                        <div key={idx} style={{ fontSize: 13, color: "#1b5e20", marginBottom: 4 }}>
                          {item}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {importResult.errors && importResult.errors.length > 0 && (
                  <div>
                    <div style={{ fontWeight: 600, marginBottom: 6, color: "#c62828" }}>
                      Errors ({importResult.errors.length}):
                    </div>
                    <div style={{ maxHeight: 180, overflowY: "auto", background: "#fafafa", border: "1px solid #eee", borderRadius: 4, padding: "8px 12px" }}>
                      {importResult.errors.map((err, idx) => (
                        <div key={idx} style={{ fontSize: 13, color: "#b71c1c", marginBottom: 4 }}>
                          {err}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {(!importResult.errors || importResult.errors.length === 0) &&
                 (!importResult.updatedItems || importResult.updatedItems.length === 0) && (
                  <p style={{ color: "#777", margin: 0 }}>No changes detected — all rows already up to date.</p>
                )}
              </div>
            )}
          </DialogContent>
          <DialogActions>
            {!importResult ? (
              <>
                <Button
                  buttonClass="grey"
                  label="Cancel"
                  onClick={() => this.setState({ importDialogOpen: false })}
                />
                <Button
                  buttonClass="blue"
                  label={importLoading ? "Uploading..." : "Choose File & Import"}
                  disabled={importLoading}
                  onClick={() => this.importFileRef.current && this.importFileRef.current.click()}
                />
              </>
            ) : (
              <Button
                buttonClass="blue"
                label="Close"
                onClick={() => this.setState({ importDialogOpen: false, importResult: null })}
              />
            )}
          </DialogActions>
        </Dialog>

        {this.state.unitConversionProduct && (
          <UnitConversionConfig
            product={this.state.unitConversionProduct}
            onClose={() => this.setState({ unitConversionProduct: null })}
          />
        )}
      </div>
    );
  }
}
export default withSnackbar(List);
