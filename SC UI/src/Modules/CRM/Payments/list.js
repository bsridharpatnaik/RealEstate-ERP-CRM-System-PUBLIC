import React from "react";
import { withSnackbar } from "notistack";
//Third Party
import Popper from "@material-ui/core/Popper";
//component
import Table from "./table";
import ListCommon from "./../../../Shared/List";
import Button from "@material-ui/core/Button"; 
//misc
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";
//style
//import "./style.scss";
import IconButtons from "./../../../Shared/Button/IconButtons.js";
import { getRole, getUserName, getUserId } from '../../../helper';
import Filter from "./filter";
class List extends ListCommon {
  title = messages.common.payments;
  filterData = {};
  disableAssignee = false;
  state = {
    data: [],
    options: [],
    filterOpen: false,
    showDetails: false,
    key: 1,
  };
  tableData = {
    headers: [
      messages.fields.payments.customerId,
      messages.fields.payments.customerName,
      messages.fields.payments.paymentDate,
      messages.fields.payments.amount,
      messages.fields.payments.isReceived,
      "Property Type",
      messages.fields.payments.propertyName,
      messages.fields.payments.paymentFrom,
      messages.fields.payments.assignee,
    ],
    keys: ["leadId", "customerName", "paymentDate", "amount", "isReceived", "propertyType", "propertyName", "isCustomerPayment","assignee"],
  };

  componentDidMount() {
    const currentUserRole = getRole();
    const currentUserId = getUserId();
    const currentUserName = getUserName();
    if (!(currentUserRole.toLowerCase().indexOf("admin") > -1) && !(currentUserRole.toLowerCase().indexOf("crm-manager") > -1)) {
      this.filterData = { ...this.filterData, assignee: [{ name: currentUserName, value: Number(currentUserId) }] }
      this.disableAssignee = true;
    }
    this.search();
    this.filterRef = React.createRef();
  }

  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    if (this.searchValue.length) {
      params.filterData.push({
        attrName: "globalsearch",
        attrValue: this.searchValue,
      });
    }
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];

        if (value && value.length) {
          if (["assignee"].includes(field)) {
            value = value.map((v) => "" + v.value);
          } else if (["startDate", "endDate"].includes(field)) {
            value = [value];
          }
          params.filterData.push({
            attrName: field,
            attrValue: value,
          });
        }
        else if (value && ["isReceived", "isCustomerPayment"].includes(field)) {
          value = value.value;
          params.filterData.push({
            attrName: field,
            attrValue: [value],
          });
        }
      }
    }
    return params;
  }

  async getOption() {
    const response = await API.GET(apiEndpoints.getpaymentDropdowns);
    if (response.success) {
      this.typeahead = response.data.typeAheadDataForGlobalSearch;
      this.dropdowns = response.data.dropdownData;
      this.setState({
        options: response.data.typeAheadDataForGlobalSearch,
        dropdownData: response.data.dropdownData,
      })
    }
  }

  async search(page = 0) {
    this.page = page;
    this.inputRef.current.value = page + 1;

    const params = this.prepareRequestBody();
    let sortParam = "";
    if (this.sortkey) {
      let sortkey = this.replaceSortKey(this.sortkey);
      sortParam = "&sort=" + sortkey;
      if (this.sortby) {
        sortParam += "," + this.sortby;
      }
    }

    this.setState({ isLoading: true });
    if (this.props.isLoading) {
      this.props.isLoading(true);
    }
    const response = await API.POST(
      apiEndpoints.getPayments + "&page=" + page + sortParam,
      params
    );
    this.setState({ isLoading: false, showDetails: false });
    if (this.props.isLoading) {
      this.props.isLoading(false);
    }
    this.showToaster(response);
    if (response.success) {
      this.getOption();
      let data = response.data.content;
      this.setState({
        data: data,
        pages: response.data.totalPages,
        totalRecords: response.data.totalElements,
      });
    }
  }

  replaceSortKey(sortKey) {
    if (sortKey === "customerId") {
      return 'leadId';
    } else if (sortKey === "customerName") {
      return 'customerName';
    } else if (sortKey === "propertyName") {
      return 'propertyName';
    } else if (sortKey === "assignee") {
      return 'assignee';
    }
    return `${sortKey}`;
  }

  async downloadData() {
    const params = this.prepareRequestBody();
    const response = await API.POST(apiEndpoints.getPaymentsExport + "page=0&size=10000", params); // Adjust the size parameter if needed
    if (response.success) {
      const data = response.data.content;
      this.downloadCSV(data);
    } else {
      this.props.enqueueSnackbar(messages.error.downloadFailed, { variant: 'error' });
    }
  }

  downloadCSV(data) {
    const headers = this.tableData.headers;
    const keys = this.tableData.keys;
  
    let csvContent = headers.join(",") + "\n";
    data.forEach(row => {
      let rowContent = keys.map(key => {
        let cell = row[key] !== null && row[key] !== undefined ? row[key] : '';
        cell = cell.toString(); // Ensure cell is a string
        // Escape double quotes
        cell = cell.replace(/"/g, '""');
        // Wrap with double quotes if the cell contains a comma or new line
        if (cell.indexOf(',') >= 0 || cell.indexOf('\n') >= 0) {
          cell = `"${cell}"`;
        }
        return cell;
      }).join(",");
      csvContent += rowContent + "\n";
    });
  
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", "data.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
  

  
  render() {
    return (
      <div className="">
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
                  typeahead={this.typeahead}
                  disableAssignee={this.disableAssignee}
                  search={(data) => {
                    this.filterData = data;
                    this.search();
                  }}
                  close={() => this.setState({ filterOpen: false })}
                />
              </Popper>

              {this.renderAutoComplete(
                this.state.options,
                messages.common.searchByNameorNumber
              )}
            </form>
            <div className="top-button-wrapper">
              <IconButtons
                onClick={() => {
                  this.setState({ filterOpen: true });
                }}
                buttonClass="filterIcon"
                innerRef={this.filterRef}
                label={messages.common.filter}
                icon={"FilterSVG"}
              />
              <Button
                color="primary"
                variant="contained"
                onClick={() => this.downloadData()}
              >
                Download CSV
              </Button>
            </div>
          </div>
          {this.state.isLoading ? (
            this.renderLoader()
          ) : (
            <Table
              key={this.state.key}
              tableData={this.tableData}
              rows={this.state.data}
              sortby={this.sortby}
              sortkey={this.sortkey}
              hideedit={true}
              hidedelete={true}
              search={(sortkey, sortby) => {
                this.sortby = sortby;
                this.sortkey = sortkey;
                this.search();
              }}
            />
          )}
          {this.renderPagination()}
        </div>
      </div>
    );
  }
}

export default withSnackbar(List);