//react
import React from "react";
import { connect } from "react-redux";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";
import SettingsIcon from "@material-ui/icons/Settings";

//component
import Table from "./../../Shared/Table";
import TenantReorderConfig from "./TenantReorderConfig";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import * as XLSX from "xlsx";
import { getCategories } from "./../../actions/categories";
import { messages } from "./../../messages";
import { API } from "./../../axios";
import { canEditInventoryModules } from "./../../helper";

class List extends ListCommon {
  deleteKey = "productId";
  deleteUrl = apiEndpoints.individualProduct;
  title = messages.common.product;

  state = { data: [], options: [], tenantConfigProduct: null };
  tableData = {
    headers: [
      messages.common.inventory,
      "Product Code",
      messages.common.description,
      "Reorder Level",
      messages.fields.measurementUnit,
      messages.fields.categoryName,
    ],
    keys: [
      "productName",
      "productCode",
      "productDescription",
      "reorderQuantity",
      "measurementUnit",
      "category",
    ],
  };
  // For product listing, don't send isManagedInventory parameter to get both true and false results
  url = apiEndpoints.getProductWithManagedInventory();
  exportUrl = exportURL.getProductWithManagedInventory();
  exportFile = messages.exportFiles.product;
  componentDidMount() {
    this.search();
    this.getCategory();
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    params.filterData.push({
      attrName: "name",
      attrValue: this.searchValue,
    });
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
    }));
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
  async getCategory() {
    const response = await API.GET(apiEndpoints.getCategoryNames);
    if (response.success) {
      this.props.getCategories(response.data);
    }
  }
  async search(page = 0) {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);

    if (response.success) {
      let data = response.data.content;
      data = data.map((item) => {
        let category = item.category.categoryName;
        return { ...item, category: category };
      });
      this.setState({
        data: data,
        pages: response.data.totalPages,
        options: response.data.productAndCategoryNames || [],
      });
    }
  }

  render() {
    const { tenantConfigProduct } = this.state;

    const customActions = canEditInventoryModules()
      ? [
          {
            key: "tenant-reorder",
            title: "Tenant Reorder Overrides",
            icon: <SettingsIcon style={{ fontSize: 18 }} />,
            onClick: (row) => this.setState({ tenantConfigProduct: row }),
          },
        ]
      : [];

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
          <div className="top-button-wrapper">{this.renderExport()}</div>
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
      </div>
    );
  }
}
export default connect(null, { getCategories }, null, { forwardRef: true })(
  withSnackbar(List)
);
