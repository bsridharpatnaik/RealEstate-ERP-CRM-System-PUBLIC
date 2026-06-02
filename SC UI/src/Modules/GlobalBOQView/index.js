import React, { Component } from "react";
import Select from "react-select";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import { withSnackbar } from "notistack";
import List from "./../BOQStatus/list";
import "./style.scss";

class GlobalBOQView extends Component {
  state = {
    tenants: [],
    selectedTenant: null,
    dropdowns: {},
  };

  async componentDidMount() {
    const response = await API.GET(apiEndpoints.getAllTennants);
    if (response.success && Array.isArray(response.data)) {
      this.setState({
        tenants: response.data.map((t) => ({
          value: t.tenantCode,
          label: t.tenantName,
        })),
      });
    }
  }

  render() {
    const { tenants, selectedTenant, dropdowns } = this.state;

    return (
      <div className="page global-boq-page">
        <div className="header-info">
          <div>
            <h2 className="page-title">{messages.common.globalBOQ}</h2>
          </div>
        </div>

        <div className="global-boq-project-selector">
          <label className="selector-label">Select Project</label>
          <Select
            options={tenants}
            value={selectedTenant}
            onChange={(opt) => this.setState({ selectedTenant: opt, dropdowns: {} })}
            placeholder="Select a project to view BOQ..."
            isClearable
            className="project-select"
          />
        </div>

        {selectedTenant ? (
          <div className="global-boq-list-wrapper">
            <List
              key={selectedTenant.value}
              tenantId={selectedTenant.value}
              readOnly={true}
              setOptions={(opts) => this.setState({ dropdowns: opts })}
            />
          </div>
        ) : (
          <div className="global-boq-placeholder">
            <p>Select a project above to view its BOQ.</p>
          </div>
        )}
      </div>
    );
  }
}

export default withSnackbar(GlobalBOQView);
