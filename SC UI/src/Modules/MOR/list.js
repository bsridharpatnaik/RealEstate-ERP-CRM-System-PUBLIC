//react
import React from "react";
import { withSnackbar } from "notistack";
//Third Party
import Popper from "@material-ui/core/Popper";
//component
import Table from "./table";
import ListCommon from "./../../Shared/List";

//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
//style
import "./style.scss";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import Filter from "./filter";
import { Slide } from "@material-ui/core";
import Details from "./details";

class List extends ListCommon {
  deleteKey = "morid";
  deleteUrl = apiEndpoints.individualMOR;
  title = messages.common.mor;
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
      messages.fields.machinery,
      messages.fields.supplier,
      messages.fields.location,
      messages.fields.mode,
    ],
    keys: ["morid", "date", "machinery", "supplier", "usageLocation", "mode"],
  };

  url = apiEndpoints.getMOR;
  exportUrl = exportURL.getMOR;
  exportFile = messages.exportFiles.mor;
  componentDidMount() {
    this.search();
    this.filterRef = React.createRef();
  }
  replaceSortKey(sortKey) {
    if (sortKey === "machinery") {
      return "machinery.machineryName";
    } else if (sortKey === "usageLocation") {
      return "usageLocation.locationName";
    } else if (sortKey === "supplier") {
      return "supplier.name";
    }
    return sortKey;
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
          if (
            ["machineryNames", "supplierNames", "usageLocationNames"].includes(
              field
            )
          ) {
            value = value.map((v) => v.name);
          } else {
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
  async search(page = 0) {
    const params = this.prepareRequestBody();

    const response = await this.getData(page, params);

    if (response.success) {
      let data = response.data.machineryOnRent.content;
      this.dropdowns = response.data.morDropdown;
      this.props.setOptions(this.dropdowns);

      this.setState({
        data: data,
        pages: response.data.machineryOnRent.totalPages,
        options: response.data.morDropdown.machinery,
        totalRecords: response.data.machineryOnRent.totalElements,
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
                this.search();
              }}
            >
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

              {this.renderAutoComplete(
                this.state.options,
                "Search by Machinery",
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
