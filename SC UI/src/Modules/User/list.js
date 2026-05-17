//react
import React from "react";
import { connect } from "react-redux";
//Third Party
import TextField from "@material-ui/core/TextField";
import SearchIcon from "@material-ui/icons/Search";
import InputAdornment from "@material-ui/core/InputAdornment";
import { withSnackbar } from "notistack";

//component
import Table from "./../../Shared/Table";
import ListCommon from "./../../Shared/List";

//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { getRoles } from "./../../actions/roles";
import { messages } from "./../../messages";
//style
import "./style.scss";
import { getAllTennants } from "./../../actions/tennant";
import { API } from "./../../axios";

class List extends ListCommon {
  searchValue = "";
  state = { data: [] };
  tableData = {
    headers: [
      messages.fields.username,
      messages.fields.status,
      messages.fields.passwordExpired,
      messages.fields.role,
      "Projects",
    ],
    keys: ["userName", "status", "passwordExpired", "role", "tenantList"],
  };
  url = apiEndpoints.getUsers;
  exportUrl = exportURL.getUsers;
  exportFile = messages.exportFiles.user;
  componentDidMount() {
    if (this.props.allTenants.length === 0) {
      this.getTennant();
    } else {
      this.search();
    }
  }
  async getTennant() {
    const response = await API.GET(apiEndpoints.getAllTennants);
    if (response.success) {
      this.props.getAllTennants(response.data);
    }
  }
  replaceSortKey(sortKey) {
    if (sortKey === "role") {
      return "roles.name";
    }
    return sortKey;
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    params.filterData.push({
      attrName: "username",
      attrValue: [this.searchValue],
    });
    return params;
  }
  getExportData(response) {
    return response.data.userDetails.content;
  }
  async search(page = 0) {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);

    if (response.success) {
      this.props.getRoles(response.data.roles);
      let data = response.data.userDetails.content;
      data = data.map((item) => {
        let role = item.roles;
        role = role.map((r) => r.name);
        let tenantList = item.tenantList;
        tenantList = tenantList.map(item => `${item.tenant.tenantLongName}-${item.authorization.charAt(0)}`);
        return { ...item, role: role, tenantList };
      });
      this.setState({
        data: data,
        pages: response.data.userDetails.totalPages,
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
            <TextField
              variant="outlined"
              margin="normal"
              fullWidth
              id="username"
              name="username"
              InputLabelProps={{ shrink: true }}
              placeholder={messages.common.searchByUsername}
              onChange={(e) => {
                this.searchValue = e.target.value;
              }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon style={{ color: "rgba(108,108,108,0.6)" }} />
                  </InputAdornment>
                ),
              }}
            />
          </form>
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
        {this.renderPagination()}
      </div>
    );
  }
}
const mapStateToProps = (state) => {
  return {
    allTenants: state.allTennant.tennants,
  };
};

export default connect(mapStateToProps, { getRoles, getAllTennants }, null, {
  forwardRef: true,
})(withSnackbar(List));
