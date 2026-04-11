//react
import React from "react";
import { withSnackbar } from "notistack";

//component
import Table from "./../../Shared/Table";
import ListCommon from "./../../Shared/List";

//misc
import { API } from "./../../axios";
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";

class List extends ListCommon {
  state = { data: [] };
  title = messages.common.firm;
  tableData = {
    headers: [
      "Firm Name",
      "Description",
      "Address",
      "GST Number",
      "PAN Number",
      "Contact Number",
    ],
    keys: [
      "firmName",
      "firmDescription",
      "firmAddress",
      "firmGstNumber",
      "firmPanNumber",
      "firmContactNumber",
    ],
  };
  url = apiEndpoints.getFirm;
  exportUrl = apiEndpoints.getFirm;
  exportFile = messages.exportFiles.firm;

  componentDidMount() {
    this.search();
  }

  async getExportAPIData() {
    const response = await API.GET(apiEndpoints.getFirm);
    return response;
  }

  getExportData(response) {
    const data = response.data;
    return Array.isArray(data) ? data : (data && data.content) || [];
  }

  async search(page = 0) {
    if (this.props.isLoading) {
      this.props.isLoading(true);
    }
    this.setState({ isLoading: true });
    const response = await API.GET(apiEndpoints.getFirm);
    this.setState({ isLoading: false });
    if (this.props.isLoading) {
      this.props.isLoading(false);
    }
    this.showToaster(response);
    if (response.success) {
      const data = response.data;
        const list = Array.isArray(data) ? data : (data && data.content) || [];
        const enriched = list.map((firm) => ({
          ...firm,
          firmAddress: [
            firm.addr_line1,
            firm.addr_line2,
            firm.city,
            firm.state,
            firm.zip,
          ].filter(Boolean).join(", ") || firm.firmAddress || "",
        }));
        this.setState({ data: enriched });
    }
  }

  render() {
    return (
      <div className="list-section">
        <div className="filter-section">
          <div></div>
          <div className="top-button-wrapper">{this.renderExport()}</div>
        </div>
        {this.state.isLoading ? (
          this.renderLoader()
        ) : (
          <Table
            tableData={this.tableData}
            rows={this.state.data}
            hidedelete={true}
            hideedit={!this.props.canEdit}
            edit={this.props.edit}
            sortby={this.sortby}
            sortkey={this.sortkey}
            search={(sortkey, sortby) => {
              this.sortby = sortby;
              this.sortkey = sortkey;
              this.search();
            }}
          />
        )}
      </div>
    );
  }
}

export default withSnackbar(List);
