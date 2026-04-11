import React from "react";
import ListCommon from "../../Shared/List";
import { withSnackbar } from "notistack";
import Table from "./table";
import { apiEndpoints, exportURL } from "../../endpoints";
import { messages } from "../../messages";
import Popper from "@material-ui/core/Popper";
import Filter from "../Stock/filter";
import IconButtons from "../../Shared/Button/IconButtons.js";

class List extends ListCommon {
  filterData = {};
  title = messages.common.stockAgingReport;
  state = { data: [], options: [] };
  tableData = {
    headers: [
      messages.common.id,
      messages.common.inventory,
      messages.common.inventoryCode,
      messages.common.category,
      messages.common.warehouse,
      messages.common.quantity,
      messages.common.currentStock,
      "Inward Date",
      "Age",
    ],
    keys: [
      "productId",
      "productName",
      "productCode",
      "categoryName",
      "warehouseName",
      "quantity",
      "currentStock",
      "inwardDate",
      "age",
    ],
  };
  url = apiEndpoints.getStockAgingReport;
  exportUrl = exportURL.getStockAgingReport;
  exportFile = messages.exportFiles.stockAgingReport;

  componentDidMount() {
    this.search();
    this.filterRef = React.createRef();
  }

  getExportData(response) {
    return response.data.stockAgingInformation.content;
  }

  prepareRequestBody() {
    let params = {};
    params.filterData = [];

    if (this.searchValue.length) {
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: this.searchValue.map((v) => v.name),
      });
    }

    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];

        if (value && value.length) {
          if (["products", "categories", "warehouses"].includes(field)) {
            value = value.map((v) => v.name);
          } else if (field === "productCodes") {
            value = value.map((v) => (v.productCode != null ? v.productCode : v.name));
          } else if (["closingDate"].includes(field)) {
            value = [value];
          }
          params.filterData.push({
            attrName: field,
            attrValue: value,
          });
        }
      }
    }
    return params;
  }

  async search(page = 0) {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);

    if (response.success) {
      const options = [
        ...(response.data.ldDropdown?.product || []),
        ...(response.data.ldDropdown?.warehouse || []),
        ...(response.data.ldDropdown?.productCodes || []),
      ];

      this.dropdowns = response.data.ldDropdown;
      this.setState({
        data: response.data.stockAgingInformation.content,
        pages: response.data.stockAgingInformation.totalPages,
        totalRecords: response.data.stockAgingInformation.totalElements,
        options,
      });
    }
  }

  render() {
    return (
      <div className="list-section">
        <div className="filter-section">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              this.search(0);
            }}
          >
            {this.renderAutoComplete(
              this.state.options,
              messages.common.searchByName,
              (option) => option.name
            )}
          </form>
          <div className="top-button-wrapper">
            {this.renderExport()}
            <IconButtons
              onClick={() => {
                this.setState({ filterOpen: true });
              }}
              buttonClass="filterIcon"
              innerRef={this.filterRef}
              label={messages.common.filter}
              icon={"FilterSVG"}
            />
          </div>
          <Popper
            open={this.state.filterOpen}
            anchorEl={this.filterRef && this.filterRef.current}
            placement="bottom-end"
          >
            <Filter
              filterData={this.filterData}
              options={this.dropdowns}
              search={(data) => {
                this.filterData = data;
                this.search();
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
            hidedelete={true}
            hideedit={true}
            sortby={this.sortby}
            sortkey={this.sortkey}
            search={(sortkey, sortby) => {
              this.sortby = sortby;
              this.sortkey = sortkey;
              this.search();
            }}
          />
        )}
        {this.renderPagination()}
      </div>
    );
  }
}

export default withSnackbar(List);
