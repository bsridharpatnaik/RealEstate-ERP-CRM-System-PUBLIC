//react
import React from "react";
import { withSnackbar } from "notistack";

//Third Party

//component
import ListCommon from "./../../Shared/List";
import Table from "./table";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import Filter from "./filter";
import Popper from "@material-ui/core/Popper";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import { Slide } from "@material-ui/core";
import Details from "./details";

class List extends ListCommon {
  deleteKey = "id";
  deleteUrl = apiEndpoints.individualLost;
  title = messages.common.lost;
  filterData = {};

  state = {
    data: [],
    options: [],
    filterOpen: false,
    showDetails: false,
    key: 1,
  };
  tableData = {
    headers: [
      messages.common.id,
      messages.fields.date,
      messages.common.warehouse,
      messages.common.inventory,
      "Quantity",
      messages.common.category,
      "Closing Stock",
    ],
    keys: [
      "id",
      "date",
      "warehouse",
      "product",
      "quantity",
      "category",
      "closingStock",
    ],
  };
  url = apiEndpoints.getLost;
  exportUrl = exportURL.getLost;
  exportFile = messages.exportFiles.lost;
  componentDidMount() {
    this.search();
    this.filterRef = React.createRef();
  }
  replaceSortKey(sortKey) {
    if (sortKey === "warehouse") {
      return "warehouse.warehouseName";
    } else if (sortKey === "product") {
      return "product.productName";
    } else if (sortKey === "category") {
      return "product.category.categoryName";
    }
    return sortKey;
  }
  getExportData(response) {
    return response.data.lostDamagedInventories.content;
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    params.filterData.push({
      attrName: "globalSearch ",
      attrValue: this.searchValue.map((v) => v.name),
    });
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];

        if (value && value.length) {
          if (
            ["productNames", "categoryNames", "warehouseNames"].includes(field)
          ) {
            value = value.map((v) => v.name);
          } else if (["startDate", "endDate"].includes(field)) {
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
      this.dropdowns = response.data.ldDropdown;
      this.props.setOptions(this.dropdowns);

      this.setState({
        data: response.data.lostDamagedInventories.content,
        pages: response.data.lostDamagedInventories.totalPages,
        totalRecords: response.data.lostDamagedInventories.totalElements,
        options: [...this.dropdowns.product, ...this.dropdowns.warehouse],
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
                this.setState({ showDetails: true, selectedData: row });
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
