//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";

//component
import Table from "./../../Shared/Table";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import { canEditInventoryModules } from "./../../helper";

class List extends ListCommon {
  deleteKey = "machineryId";
  deleteUrl = apiEndpoints.individualMachinery;
  title = messages.common.machinery;

  state = { data: [], options: [] };
  tableData = {
    headers: [messages.fields.machineryName, messages.common.description],
    keys: ["machineryName", "machineryDescription"],
  };
  url = apiEndpoints.getMachinery;
  exportUrl = exportURL.getMachinery;
  exportFile = messages.exportFiles.machinery;

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
    return response.data.machineries.content;
  }
  async search(page = 0) {
    const params = this.prepareRequestBody();

    const response = await this.getData(page, params);

    if (response.success) {
      this.setState({
        data: response.data.machineries.content,
        pages: response.data.machineries.totalPages,
        options: response.data.machineryNames,
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
            hideedit={this.props.hideedit}
            hidedelete={!canEditInventoryModules()}
          />
        )}
        {this.renderPagination()}
      </div>
    );
  }
}

export default withSnackbar(List);
