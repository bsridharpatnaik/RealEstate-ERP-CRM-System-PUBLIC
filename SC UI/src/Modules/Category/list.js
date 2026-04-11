//react
import React from "react";
import { withSnackbar } from "notistack";

//component
import Table from "./../../Shared/Table";
import ListCommon from "./../../Shared/List";

//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import { canEditInventoryModules } from "./../../helper";
class List extends ListCommon {
  deleteKey = "categoryId";
  state = { data: [], options: [] };
  deleteUrl = apiEndpoints.individualCategory;
  title = messages.common.category;
  tableData = {
    headers: [messages.fields.categoryName, messages.common.description],
    keys: ["categoryName", "categoryDescription"],
  };
  url = apiEndpoints.getCategory;
  exportUrl = exportURL.getCategory;
  exportFile = messages.exportFiles.category;
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
    return response.data.categories.content;
  }
  async search(page = 0) {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);

    if (response.success) {
      this.setState({
        data: response.data.categories.content,
        pages: response.data.categories.totalPages,
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
              this.search();
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
            sortkey={this.sortkey}
            sortby={this.sortby}
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
