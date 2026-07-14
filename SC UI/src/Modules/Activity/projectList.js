import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';

import ListCommon from '../../Shared/List';
import Table from './table';
import Filter from './projectFilter';
import IconButtons from '../../Shared/Button/IconButtons';
import { API } from '../../axios';
import { apiEndpoints } from '../../endpoints';
import { messages } from '../../messages';

class ProjectList extends ListCommon {
  filterData = {};
  sortkey = 'activityTime';
  sortby = 'desc';

  state = { data: [], pages: 0, totalRecords: 0, isLoading: false, filterOpen: false };

  tableData = {
    headers: ['Time', 'Action', 'Entity Type', 'Entity ID', 'Description', 'Performed By'],
    keys: ['activityTime', 'action', 'entityType', 'entityId', 'description', 'performedBy'],
  };

  url = apiEndpoints.activityLogList;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.search();
  }

  prepareRequestBody() {
    const params = { filterData: [] };
    if (this.filterData) {
      for (const field in this.filterData) {
        const value = this.filterData[field];
        if (value === undefined || value === null || value === '') continue;
        params.filterData.push({
          attrName: field,
          attrValue: Array.isArray(value) ? value.map(String) : [String(value)],
        });
      }
    }
    return params;
  }

  async search(page = 0) {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);
    if (response.success) {
      const paged = response.data;
      this.setState({
        data: paged.content || [],
        pages: paged.totalPages || 0,
        totalRecords: paged.totalElements || 0,
      });
    }
  }

  async exportExcel() {
    const r = await API.POSTBlob(apiEndpoints.activityLogExport, this.prepareRequestBody());
    if (r.success) {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Activity_Log.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } else {
      this.props.enqueueSnackbar(r.errorMessage || 'Export failed', { variant: 'error' });
    }
  }

  render() {
    return (
      <div className="list-section">
        <div className="filter-section">
          <div />
          <div className="top-button-wrapper" style={{ display: 'flex', gap: '8px' }}>
            <IconButtons
              onClick={() => this.exportExcel()}
              buttonClass="filterIcon"
              label="Export Excel"
              icon="DownloadSVG"
            />
            <IconButtons
              onClick={() => this.setState({ filterOpen: true })}
              buttonClass="filterIcon"
              label={messages.common.filter}
              icon="FilterSVG"
              innerRef={this.filterRef}
            />
          </div>
        </div>

        <Popper
          open={this.state.filterOpen}
          anchorEl={this.filterRef && this.filterRef.current}
          placement="bottom-end"
          style={{ zIndex: 1300 }}
        >
          <Filter
            filterData={this.filterData}
            search={(data) => {
              this.filterData = data;
              this.setState({ filterOpen: false });
              this.search(0);
            }}
            close={() => this.setState({ filterOpen: false })}
          />
        </Popper>

        {this.state.isLoading ? (
          this.renderLoader()
        ) : (
          <Table
            tableData={this.tableData}
            rows={this.state.data}
            hideedit
            hidedelete
            sortkey={this.sortkey}
            sortby={this.sortby}
            search={(sortkey, sortby) => {
              this.sortkey = sortkey;
              this.sortby = sortby;
              this.search();
            }}
          />
        )}

        {this.renderPagination()}
      </div>
    );
  }
}

export default withSnackbar(ProjectList);
