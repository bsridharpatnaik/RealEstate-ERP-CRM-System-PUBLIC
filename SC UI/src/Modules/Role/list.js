//react
import React from "react";
//components
import Table from "./../../Shared/Table";
//misc
import { API } from "./../../axios";
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import ListCommon from "./../../Shared/List";
class List extends ListCommon {
  state = { data: [] };
  tableData = {
    headers: [messages.fields.name],
    keys: ["name"],
  };
  exportUrl = exportURL.getRoles;
  exportFile = messages.exportFiles.role;
  componentDidMount() {
    this.search();
  }
  async getExportAPIData() {
    let sortParam = "";
    if (this.sortkey) {
      let sortkey = this.replaceSortKey(this.sortkey);
      sortParam = "sort=" + sortkey;
      if (this.sortby) {
        sortParam += "," + this.sortby;
      }
    }
    const response = await API.GET(apiEndpoints.getRoles + "?" + sortParam);
    return response;
  }
  getExportData(response) {
    return response.data.content;
  }

  async search(page = 0) {
    this.setState({ isLoading: true });
    let sortParam = "";
    if (this.sortkey) {
      let sortkey = this.replaceSortKey(this.sortkey);
      sortParam = "sort=" + sortkey;
      if (this.sortby) {
        sortParam += "," + this.sortby;
      }
    }
    const response = await API.GET(apiEndpoints.getRoles + "?" + sortParam);
    this.setState({ isLoading: false });
    this.showToaster(response);
    if (response.success) {
      this.setState({
        data: [...response.data.content],
      });
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
      </div>
    );
  }
}

export default List;
