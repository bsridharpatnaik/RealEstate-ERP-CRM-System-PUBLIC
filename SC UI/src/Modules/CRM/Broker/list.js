//react
import React from "react";
//Third Party
import ListCommon from "./../../../Shared/List";
import { withSnackbar } from "notistack";

//component
import Table from "./../../../Shared/Table";
//misc
import { apiEndpoints, exportURL } from "./../../../endpoints";
import { messages } from "./../../../messages";
class List extends ListCommon {
  deleteKey = "brokerId";
  deleteUrl = apiEndpoints.individualBroker;
  title = messages.common.broker;

  state = { data: [], options: [] };
  tableData = {
    headers: [
      messages.common.broker,
      messages.fields.mobileNo,
      messages.fields.address,
    ],
    keys: ["brokerName", "brokerPhoneno", "brokerAddress"],
  };
  url = apiEndpoints.getBroker;
  exportUrl = exportURL.getBroker;
  exportFile = messages.exportFiles.broker;

  componentDidMount() {
    this.search();
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    params.filterData.push({
      attrName: "globalSearch",
      attrValue: this.searchValue,
    });

    return params;
  }
  getExportData(response) {
    return response.data.brokerDetails.content;
  }
  async search(page = 0) {
    const params = this.prepareRequestBody();

    const response = await this.getData(page, params);

    if (response.success) {
      this.setState({
        data: response.data.brokerDetails.content,
        pages: response.data.brokerDetails.totalPages,
        options: response.data.typeAheadData,
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
