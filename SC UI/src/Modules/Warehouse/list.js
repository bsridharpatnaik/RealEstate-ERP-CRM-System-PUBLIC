//react
import React from "react";
//Third Party
//component
import Table from "./../../Shared/Table";
//misc
import { API } from "./../../axios";
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import ListCommon from "./../../Shared/List";
class List extends ListCommon {
  state = { data: [] };
  tableData = {
    headers: [messages.common.warehouse],
    keys: ["name"],
  };
  exportUrl = exportURL.getWarehouse;
  exportFile = messages.exportFiles.warehouse;
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

    const response = await API.GET(apiEndpoints.getWarehouse + "?" + sortParam);
    return response;
  }
  getExportData(response) {
    return response.data;
  }
  async search(page = 0) {
    if (this.props.isLoading) {
      this.props.isLoading(true);
    }
    this.setState({ isLoading: true });
    let sortParam = "";
    if (this.sortkey) {
      let sortkey = this.replaceSortKey(this.sortkey);
      sortParam = "sort=" + sortkey;
      if (this.sortby) {
        sortParam += "," + this.sortby;
      }
    }

    const response = await API.GET(apiEndpoints.getWarehouse + "?" + sortParam);
    this.setState({ isLoading: false });
    if (this.props.isLoading) {
      this.props.isLoading(false);
    }
    this.showToaster(response);
    if (response.success) {
      let data = response.data;
      this.setState({
        data: data,
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

export default List;
