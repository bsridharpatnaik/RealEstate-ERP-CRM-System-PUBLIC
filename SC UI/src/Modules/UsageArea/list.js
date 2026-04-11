//react
import React from "react";
import { withSnackbar } from "notistack";

//Third Party

//component
import ListCommon from "./../../Shared/List";
import Table from "./../../Shared/Table";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";

class List extends ListCommon {
  deleteKey = "usageAreaId";
  deleteUrl = apiEndpoints.individualUsageArea;
  title = messages.common.finalLocation;

  state = { data: [], options: [] };
  tableData = {
    headers: [messages.fields.usageAreaName, messages.common.description],
    keys: ["usageAreaName", "usageAreaDescription"],
  };
  url = apiEndpoints.getUsageArea;
  exportUrl = exportURL.getUsageArea;
  exportFile = messages.exportFiles.finalLocation;
  componentDidMount() {
    this.search();
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    params.filterData.push({
      attrName: "name",
      attrValue: this.searchValue,
    });
    return params;
  }
  getExportData(response) {
    return response.data.usageAreas.content;
  }
  async search(page = 0) {
    const params = this.prepareRequestBody();

    const response = await this.getData(page, params);

    if (response.success) {
      this.setState({
        data: response.data.usageAreas.content,
        pages: response.data.usageAreas.totalPages,
        options: response.data.names,
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
          <div className="top-button-wrapper">{this.renderExport()}</div>
        </div>
        {this.state.isLoading ? (
          this.renderLoader()
        ) : (
          <Table
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
          />
        )}
        {this.renderPagination()}
      </div>
    );
  }
}

export default withSnackbar(List);
