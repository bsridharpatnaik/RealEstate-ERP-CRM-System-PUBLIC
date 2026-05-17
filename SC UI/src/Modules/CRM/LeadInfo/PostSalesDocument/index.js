//react
import React from "react";
//Third Party
import ListCommon from "./../../../../Shared/List";
import { withSnackbar } from "notistack";
import { API } from "./../../../../axios";

//component
import Table from "./table";
//misc
import { apiEndpoints } from "./../../../../endpoints";
import { messages } from "./../../../../messages";
class DocumentList extends ListCommon {
  title = messages.common.document;

  state = { data: [], options: [] };
  tableData = {
    headers: [
      messages.common.documentName,
      messages.common.receivedStatus,
      messages.common.upload,
    ],
    keys: ["documentName", "receivedStatus", "upload"],
  };
  
  url = apiEndpoints.getPostSalesDocumentList;

  componentDidMount() {
    this.search();
  }

  async search() {
    const response = await API.GET(this.url.replace('{leadId}', this.props.leadId))
    if (response.success) {
      this.setState({
        data: response.data,
      });
    }
    else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  }

  render() {
    return (
        <React.Fragment>
        <div className="header-info">
          <div>
            <span className="page-heading">{this.title}</span>
          </div>
        </div>
        {this.state.isLoading ? (
          this.renderLoader()
        ) : (
          <Table
            tableData={this.tableData}
            rows={this.state.data}
            edit={this.props.edit}
            leadId={this.props.leadId}
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
      </React.Fragment>
    );
  }
}

export default withSnackbar(DocumentList);
