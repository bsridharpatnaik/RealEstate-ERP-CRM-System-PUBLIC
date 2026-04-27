import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";
import converter from "json-2-csv";

//component
import Table from "./table";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import MonthlyReportFilter from "./monthlyFilter";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import { getJson2csvCallback } from "../../helper";
import { API } from "../../axios";
import moment from "moment";

class List extends ListCommon {
  filterData = {};
  monthlyReportFilterData = {};
  title = messages.common.allInventory;
  state = { data: [], options: [], isLoading: false };
  tableData = {
    headers: [
      messages.fields.date,
      "Type",
      "Warehouse",
      messages.common.inventory,
      "Category",
      "Quantity",
      "Closing Stock",
      "Contact",
    ],
    keys: [
      "date",
      "type",
      "warehouseName",
      "productName",
      "categoryName",
      "quantity",
      "closingStock",
      "name",
    ],
  };
  url = apiEndpoints.getAllInventoryTransactions;
  exportUrl = exportURL.getAllInventoryTransactions;
  exportFile = messages.exportFiles.allInventory;
  refreshClosingStock = apiEndpoints.refreshClosingStock;

typeoptions = [
    { name: "Inward",       value: "Inward" },
    { name: "Transfer In",  value: "Transfer-In" },
    { name: "Transfer Out", value: "Transfer-Out" },
    { name: "Outward",      value: "Outward" },
    { name: "Lost/Damaged", value: "Lost-Damaged" },
    { name: "All",          value: "" },
];

  componentDidMount() {
    if (this.props.filter) {
      this.filterData.type = this.typeoptions.filter(
        (option) => option.value === this.props.filter
      );
    }
    this.setInitialDates();
    this.search();
    this.filterRef = React.createRef();
    this.monthlyReportFilterRef = React.createRef();
  }

  setInitialDates = () => {
    this.monthlyReportFilterData.startDate = moment(1, "DD").format("DD-MM-YYYY");
    this.monthlyReportFilterData.EndDate = moment().format("DD-MM-YYYY");
  }

  async search(page = 0, sortkey = null, sortby = null) {
    if (sortkey !== null) this.sortkey = sortkey;
    if (sortby !== null) this.sortby = sortby;
    this.setState({ isLoading: true });
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);

    if (response.success) {
      const data = response.data;
      this.dropdowns = data.ldDropdown;
      const options = [...this.dropdowns.product, ...this.dropdowns.warehouse];
      this.setState({
        data: response.data.transactions.content,
        pages: response.data.transactions.totalPages,
        totalRecords: response.data.transactions.totalElements,
        options: options,
        isLoading: false,
      });
    }
  }

  getExportData(response) {
    return response.data.transactions.content;
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
          } else if (field === "type") {
            value = value.map((v) => v.value);
          } else if (["startDate", "EndDate"].includes(field)) {
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

  prepareMonthlyRequestBody() {
    let params = {};
    params.filterData = [];
    if (this.filterData) {
      for (const field in this.monthlyReportFilterData) {
        let value = this.monthlyReportFilterData[field];

        if (value && value.length) {
          if (["products", "categories", "warehouses"].includes(field)) {
            value = value.map((v) => v.name);
          } else if (field === "type") {
            value = value.map((v) => v.value);
          } else if (["startDate", "EndDate"].includes(field)) {
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

  // Override base-class CSV export with a proper Excel export that respects filters
  downloadExcel = async () => {
    const params = this.prepareRequestBody();
    const response = await API.POSTBlob(exportURL.allInventoryExportExcel, params);
    if (!response.success) {
      this.props.enqueueSnackbar("Export failed. Please try again.", { variant: "error" });
      return;
    }
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", "inventory-transactions.xlsx");
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  renderExport() {
    return (
      <IconButtons
        onClick={this.downloadExcel}
        buttonClass="download"
        label="Download Excel"
        icon={"DownloadSVG"}
      />
    );
  }

  async exportMonthlyReportToCSV() {
    const response = await API.POST(exportURL.getInventoryReport, this.prepareMonthlyRequestBody());
    if (!response.success) {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
      return;
    }
    var startValue;
    var endValue;
    for (const field in this.monthlyReportFilterData) {
      let value = this.monthlyReportFilterData[field];
      if (["startDate"].includes(field)) {
        startValue = [value];
      }
      if (["EndDate"].includes(field)) {
        endValue = [value];
      }
    }
    let json2csvCallback = getJson2csvCallback("Monthly Report_" + startValue + "_" + endValue + ".csv");
    converter.json2csv(response.data, json2csvCallback);
  }

  refreshClosingStock = async () => {
    const response = await API.GET(apiEndpoints.refreshClosingStock);
    if (response.success) {
      this.props.enqueueSnackbar("Closing stock updated successfully.", {
        variant: "success",
      });
      this.search();
    } else {
      this.props.enqueueSnackbar("Failed to update closing stock.", {
        variant: "error",
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
            {this.renderAutoComplete(this.state.options, "Search", (option) => {
              return option.name;
            })}
          </form>
          <div className="top-button-wrapper">
            <IconButtons
              onClick={() => {
                this.setState({ monthlyReportFilterOpen: true });
              }}
              innerRef={this.monthlyReportFilterRef}
              buttonClass="download"
              label={messages.common.exportMonthlyReport}
              icon={"DownloadSVG"}
            />
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
            <IconButtons
              onClick={this.refreshClosingStock}
              buttonClass="refreshIcon"
              label="Refresh"
              icon={"RefreshSVG"}
            />
          </div>
          <Popper
            open={this.state.monthlyReportFilterOpen}
            anchorEl={this.monthlyReportFilterRef && this.monthlyReportFilterRef.current}
            placement="bottom-end"
          >
            <MonthlyReportFilter
              filterData={this.monthlyReportFilterData}
              options={this.dropdowns || {}}
              search={(data) => {
                this.monthlyReportFilterData = data;
                this.exportMonthlyReportToCSV();
              }}
              onReset={this.setInitialDates}
              close={() => this.setState({ monthlyReportFilterOpen: false })}
            />
          </Popper>
          <Popper
            open={this.state.filterOpen}
            anchorEl={this.filterRef && this.filterRef.current}
            placement="bottom-end"
          >
            <Filter
              filterData={this.filterData}
              options={this.dropdowns || {}}
              search={(data) => {
                this.filterData = data;
                this.search();
              }}
              close={() => this.setState({ filterOpen: false })}
            />
          </Popper>
        </div>
        <div id="note">
          Note - This page is updated every 30 mins. Click on "Refresh" button to update now.
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
