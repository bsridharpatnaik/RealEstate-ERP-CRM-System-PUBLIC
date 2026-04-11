//react
import React from "react";
//Third Party
import ListCommon from "./../../../Shared/List";
import { withSnackbar } from "notistack";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import IconButtons from "./../../../Shared/Button/IconButtons.js";
import { API } from '../../../axios';


//component
import Table from "./table";
//misc
import { apiEndpoints, exportURL } from "./../../../endpoints";
import { getSession, clearSession } from './../../../helper';
import { messages } from "./../../../messages";
class List extends ListCommon {
  title = messages.common.postsales;

  state = { data: [], options: [] };
  tableData = {
    headers: [
      messages.common.id,
      messages.fields.customerName,
      messages.fields.phoneNumber,
      messages.fields.propertyType,
      messages.fields.owner,
      messages.fields.nextPaymentDate,
    ],
    keys: [
      messages.formParams.lead.leadId,
      "customerName",
      "primaryMobile",
      "propertyType",
      "asigneeId",
      "nextPaymentDate",
    ],
  };
  url = apiEndpoints.getpostSales;
  exportUrl = exportURL.getCustomer;
  exportFile = messages.exportFiles.postSales;
  filterData = {};
  pageNumber = 0;
  componentDidMount() {
    const postSalesfilterData = getSession('postSalesfilterdata');
    const page = getSession('postSalesfilterPage');
    this.filterData = postSalesfilterData === null ? {} : postSalesfilterData;
    (page === null || page === undefined) ? this.search() : this.search(page);
    this.filterRef = React.createRef();
  }

  async getOption()
    {
        const response = await API.GET(apiEndpoints.getdropdowns);
        if (response.success) {
            this.props.setOptions(this.props.options);
            this.typeahead = response.data.typeAheadDataForGlobalSearch;
            this.dropdowns = response.data.dropdownData;
            this.setState({
                options: response.data.typeAheadDataForGlobalSearch,
                dropdownData: response.data.dropdownData,
            })
        }
    }


  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];
        if (value && (value.length || ["showOnlyLatest"].includes(field))) {
          if (
            ["source", "broker", "assignee", "createdBy"].includes(field)
          ) {
            value = value.map((v) => "" + v.value);
          } else if (
            [
              "createdStartDate",
              "createdEndDate",
            ].includes(field)
          ) {
            value = [value];
          } 
          params.filterData.push({
            attrName: field,
            attrValue: value,
          });
        } 
      }
    }
    params.filterData.push({
      attrName: "globalSearch",
      attrValue: this.searchValue,
    });

    return params;
  }
  getExportData(response) {
    return response.data.content;
  }

  async search(page = 0) {
    const params = this.prepareRequestBody();

    const response = await this.getData(page, params);
    this.pageNumber = page;
    clearSession('postSalesfilterdata');
    clearSession('postSalesfilterPage');
    if (response.success) {
      this.getOption();
      this.setState({
        data: response.data.content,
        pages: response.data.totalPages,
        totalRecords: response.data.totalElements,
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
              messages.common.searchByName
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
              options={this.dropdowns || {}}
              typeahead={this.typeahead || []}
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
            hidedelete={true}
            hideedit={true}
            tableData={this.tableData}
            rows={this.state.data}
            sortby={this.sortby}
            sortkey={this.sortkey}
            filterData={this.filterData}
            pageNumber={this.pageNumber}
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
