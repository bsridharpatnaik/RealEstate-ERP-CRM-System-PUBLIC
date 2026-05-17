//react
import React from "react";
import { withSnackbar } from "notistack";
//Third Party
import Popper from "@material-ui/core/Popper";
//component
import Table from "./table";
import ListCommon from "./../../Shared/List";

//misc
import { API } from "./../../axios";
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import { getFilenameFromContentDisposition, triggerBlobDownload } from "./../../helper";
//style
import "./style.scss";
import { Slide } from "@material-ui/core";
import Details from "./details";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import Filter from "./filter";
class List extends ListCommon {
  deleteKey = "contactId";
  deleteUrl = apiEndpoints.deleteContact;
  title = messages.common.contact;
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
      messages.fields.contactName,
      messages.fields.mobileNo,
      messages.fields.contactType,
      messages.fields.email,
      messages.fields.city,
    ],
    keys: ["name", "mobileNo", "contactType", "emailId", "city"],
  };
  exportUrl = exportURL.getContact;
  exportFile = messages.exportFiles.contact;
  ignoreQueryParamsExport = true;
  componentDidMount() {
    this.search();
    this.filterRef = React.createRef();
  }
  
  scrollBottom(){
    window.scrollTo(0, document.body.scrollHeight);
  }
  
  showDetail=(row)=>{
    this.setState({ showDetails: true, contact: row });
    this.scrollBottom()
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    if (this.searchValue.length) {
      params.filterData.push({
        attrName: "nameormobile",
        attrValue: this.searchValue,
      });
    }
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];

        if (value && value.length) {
          if (["contacttype"].includes(field)) {
            value = value.map((v) => v.label);
          } else if (typeof value === "string") {
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
  async exportToCSV() {
    const payload = this.prepareRequestBody();
    const response = await API.POSTBlob(apiEndpoints.contactExportExcel, payload);
    if (!response.success) {
      this.props.enqueueSnackbar(response.errorMessage || "Download failed", { variant: "error" });
      return;
    }
    const filename = getFilenameFromContentDisposition(response.headers) || "contacts_export.xlsx";
    triggerBlobDownload(response.data, filename);
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
      apiEndpoints.getContact + "&page=" + page + sortParam,
      params
    );
    this.setState({ isLoading: false, showDetails: false });
    if (this.props.isLoading) {
      this.props.isLoading(false);
    }
    this.showToaster(response);
    if (response.success) {
      let data = response.data.content;
      this.setState({
        data: data,
        pages: response.data.totalPages,
        options: response.data.contactNames,
        totalRecords: response.data.totalElements,
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
                  options={this.state.options}
                  search={(data) => {
                    this.filterData = data;
                    this.search();
                  }}
                  close={() => this.setState({ filterOpen: false })}
                />
              </Popper>
              {this.renderAutoSearch(
                apiEndpoints.contactGlobalSearch,
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
        <Slide
          direction="right"
          in={this.state.showDetails}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <Details
            contact={this.state.contact}
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
