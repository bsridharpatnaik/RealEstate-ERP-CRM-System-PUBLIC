//react
import React from "react";
import { withSnackbar } from "notistack";

//component
import Table from "./../../Shared/Table";
import ListCommon from "./../../Shared/List";

//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
class List extends ListCommon {
  deleteKey = "typeId";
  state = { data: [], options: [] };
  deleteUrl = apiEndpoints.individualBuildingType;
  title = messages.common.buildingType;
  tableData = {
    headers: [messages.fields.buildingTypeName, messages.common.description],
   keys: ["typeName", "typeDescription"],
   
  };
  url = apiEndpoints.getBuildingType;
  exportUrl = exportURL.getBuildingType;
  exportFile = messages.exportFiles.buildingType;
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
    return response.data.buildingTypes.content;
  }
  async search(page = 0) {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);

    if (response.success) {
      this.setState({
        data: response.data.buildingTypes.content,
        pages: response.data.buildingTypes.totalPages,
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
          />
        )}
        {this.renderPagination()}
      </div>
    );
  }
}

export default withSnackbar(List);
