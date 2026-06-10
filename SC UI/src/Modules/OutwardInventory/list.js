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
import Button from "./../../Shared/Button";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import { Slide } from "@material-ui/core";
import Details from "./details";
import DetailsPopup from "./../../Shared/DetailsPopup";
import Total from "./../../Shared/TotalSidePanel";
import { API } from '../../axios';

class List extends ListCommon {
  deleteKey = "outwardid";
  deleteUrl = apiEndpoints.individualOutwardInventory;
  title = messages.common.outwardInventory;
  filterData = {};
  searchDebounceTimer = null;
  state = {
    data: [],
    globalSearchText: "",
    showDetails: false,
    key: 1,
    showTotal: false,
    pageno: 0,
    totals: []
  };
  url = apiEndpoints.getOutwardInventory;
  exportUrl = exportURL.getOutwardInventory;
  exportFile = messages.exportFiles.outwardInventory;
  ignoreQueryParamsExport = true;

  tableData = {
    headers: [
      messages.common.id,
      messages.fields.date,
      "Contractor",
      messages.common.warehouse,
      messages.common.location,
      messages.common.finalLocation,
      "Inventory Count",
      "Created By",
    ],
    keys: [
      "outwardid",
      "date",
      "contractor",
      "warehouse",
      "usageLocation",
      "usageArea",
      "inventoryCount",
      "createdBy",
    ],
  };

  componentDidMount() {
    this.search();
    this.filterRef = React.createRef();
  }
  replaceSortKey(sortKey) {
    if (sortKey === "warehouse") {
      return "warehouse.warehouseName";
    } else if (sortKey === "contractor") {
      return "contractor.name";
    }
    return sortKey;
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    if (this.state.globalSearchText.trim().length) {
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: [this.state.globalSearchText.trim()],
      });
    }
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];

        if (value && value.length) {
          if (
            [
              "productNames",
              "contractorNames",
              "warehouseNames",
              "usageLocation",
              "usageArea",
              "categoryNames",
            ].includes(field)
          ) {
            value = value.map((v) => v.name);
          } else if (["showOnlyRejected", "showOnlyReturned", "startDate", "endDate", "textSearch", "boqBypassed", "fifoOverride"].includes(field)) {
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
  getExportData(response) {
    return response.data;
  }
  async search(page = 0, sortkey = null, sortby = null) {
    if (sortkey !== null) this.sortkey = sortkey;
    if (sortby !== null) this.sortby = sortby;
    this.setState({ pageno: page });
    this.setState({ isLoading: true });
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);
    this.getTotals();
    if (response.success) {
      this.dropdowns = response.data.iiDropdown;
      this.props.setOptions(this.dropdowns);
      this.setState({
        data: response.data.outwardInventory.content,
        pages: response.data.outwardInventory.totalPages,
        totalRecords: response.data.outwardInventory.totalElements,
      });
    }
  }

  async goToDetails() {
    if (this.state && this.state.pageno !== undefined) {
      await this.search(this.state.pageno);

      this.setState({
        showDetails: true,
        showTotal: false,
        selectedData: this.state.data.filter(
          (x) => x.outwardid === this.state.selectedData.outwardid
        )[0],
      });
    }
  }
  


  scrollBottom(){
    window.scrollTo(0, document.body.scrollHeight);
  }
  
  showDetail=(row)=>{
  this.setState({showDetails: true,
  showTotal: false,
  selectedData: row
  })
  this.scrollBottom()
  }

  getTotals = async () => {
    const params = this.prepareRequestBody();
    const response = await API.POST(apiEndpoints.getOutwardInventoryTotals, params);
    if (response.success) {
      this.setState({ totals: response?.data ?? [] });
    }
  }

  render() {
    return (
      <div
        className={
          this.state.showTotal ? "split" : ""
        }
      >
        <div className="list-section">
          <div className="filter-section">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                clearTimeout(this.searchDebounceTimer);
                this.search(0);
              }}
            >
              <input
                className="global-search-input"
                type="text"
                placeholder="Search by ID, product, contractor, slip no..."
                value={this.state.globalSearchText}
                onChange={(e) => {
                  const val = e.target.value;
                  this.setState({ globalSearchText: val });
                  clearTimeout(this.searchDebounceTimer);
                  this.searchDebounceTimer = setTimeout(() => this.search(0), 3000);
                }}
              />
            </form>
            <div className="top-button-wrapper">
              <Button
                onClick={() => {
                  this.setState({ showTotal: true, showDetail: false });
                }}
                buttonClass="filter"
                innerRef={this.filterRef}
                label={messages.common.total}
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
              edit={this.props.edit}
              delete={(row) => this.delete(row)}
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
        <DetailsPopup
          open={this.state.showDetails}
          onClose={() => this.setState({ showDetails: false, key: this.state.key + 1 })}
        >
          {this.state.showDetails && (
            <Details
              data={this.state.selectedData}
              edit={this.props.edit}
              delete={(row) => this.delete(row)}
              goToDetails={() => this.goToDetails()}
              close={() =>
                this.setState({ showDetails: false, key: this.state.key + 1 })
              }
            />
          )}
        </DetailsPopup>
        <Slide
          direction="right"
          in={this.state.showTotal}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <Total
            totals={this.state.totals}
            close={() => this.setState({ showTotal: false })}
          />
        </Slide>
      </div>
    );
  }
}

export default withSnackbar(List);
