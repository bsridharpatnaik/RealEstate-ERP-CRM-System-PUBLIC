//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";

//component
import Table from "./table";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import { Slide } from "@material-ui/core";
import Details from "./details";
import { API } from "./../../axios";

class List extends ListCommon {
  filterData = {};
  title = messages.common.stock;
  state = { data: [], options: [], showDetails: false, key: 1 };
  tableData = {
    headers: [
      messages.common.id,
      messages.common.inventory,
      messages.common.inventoryCode,
      messages.common.category,
      messages.common.totalStock,
      "Last Inward Date",
      messages.common.reorderQuantity,
      messages.common.stockStatus,
    ],
    keys: [
      "productId",
      "productName",
      "productCode",
      "categoryName",
      "totalQuantityInHand",
      "lastInwardDate",
      "reorderQuantity",
      "stockStatus",
    ],
  };
  url = apiEndpoints.getStock;
  exportUrl = exportURL.getStock;
  exportFile = messages.exportFiles.stock;
  ignoreQueryParamsExport = true;
  componentDidMount() {
    this.search();
    this.filterRef = React.createRef();
    this.getOptions();
  }
  async getOptions() {
    const response = await API.GET(apiEndpoints.stockDropdown);
    if (response.success) {
      this.dropdowns = response.data;
      const options = [...this.dropdowns.product, ...this.dropdowns.warehouse, ...(this.dropdowns.productCodes || [])];
      this.props.setOptions(this.dropdowns);
      this.setState({ options: options });
    }
  }
  getExportData(response) {
    return response.data;
  }

  scrollBottom() {
    window.scrollTo(0, document.body.scrollHeight);
  }

  showDetail = (row) => {
    this.setState({ showDetails: true, selectedData: row });
    this.scrollBottom()
  }

  prepareRequestBody() {
    let params;
    params = {};
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
            value = value.map((v) => v.productCode != null ? v.productCode : v.name);
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
  async search(page = 0, sortkey = null, sortby = null) {
    if (sortkey !== null) this.sortkey = sortkey;
    if (sortby !== null) this.sortby = sortby;
    const params = this.prepareRequestBody();
    this.setState({ isLoading: true });

    const response = await this.getData(page, params);

    if (response.success) {
      this.setState({
        data: response.data.stockInformation.content,
        pages: response.data.stockInformation.totalPages,
        totalRecords: response.data.stockInformation.totalElements,
      });
    }
  }

  render() {
    return (
      <div className={this.state.showDetails ? "split" : ""}>
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
                (option) => {
                  return option.name;
                }
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
              key={this.state.key}
              tableData={this.tableData}
              rows={this.state.data}
              headerName={this.tableData.headers}
              hidedelete={true}
              hideedit={true}
              sortby={this.sortby}
              sortkey={this.sortkey}
              search={(sortkey, sortby) => {
                this.sortby = sortby;
                this.sortkey = sortkey;
                this.search();
              }}
              showDetail={(row) => {
                this.showDetail(row)
              }}
            />
          )}
          {this.renderPagination()}
        </div>
        <Slide
          direction="right"
          in={this.state.showDetails}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <Details
            data={this.state.selectedData}
            edit={this.props.edit}
            delete={(row) => this.delete(row)}
            close={() =>
              this.setState({ showDetails: false, key: this.state.key + 1 })
            }
          />
        </Slide>
      </div>
    );
  }
}

export default withSnackbar(List);
