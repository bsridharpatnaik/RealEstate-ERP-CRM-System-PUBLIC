import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';

import ListCommon from '../../Shared/List';
import Table from './table';
import Filter from './filter';
import IconButtons from '../../Shared/Button/IconButtons';
import { API } from '../../axios';
import { apiEndpoints } from '../../endpoints';
import { messages } from '../../messages';

class List extends ListCommon {
  filterData = {};
  sortkey = 'activityTime';
  sortby = 'desc';

  state = { data: [], pages: 0, totalRecords: 0, isLoading: false, filterOpen: false, syncing: false, tenantOptions: [] };

  tableData = {
    headers: ['Time', 'Project', 'Action', 'Entity Type', 'Entity ID', 'Description', 'Performed By'],
    keys: ['activityTime', 'tenantSchema', 'action', 'entityType', 'entityId', 'description', 'performedBy'],
  };

  url = apiEndpoints.activityLogGlobalList;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.search();
    this.fetchTenants();
  }

  async fetchTenants() {
    const res = await API.GET(apiEndpoints.getTenants);
    if (res.success && Array.isArray(res.data)) {
      const tenantOptions = res.data
        .filter((t) => t.inventory === true)
        .map((t) => ({ name: t.tenantName || t.name || '', id: t.tenantCode }))
        .filter((t) => t.name && t.id);
      this.setState({ tenantOptions });
    }
  }

  prepareRequestBody() {
    const params = { filterData: [] };
    if (this.filterData) {
      for (const field in this.filterData) {
        const value = this.filterData[field];
        if (value === undefined || value === null || value === '') continue;

        if (field === 'tenantSchema') {
          const arr = Array.isArray(value) ? value : [value];
          const codes = arr.map((p) => (p && typeof p === 'object' ? p.id : p)).filter(Boolean);
          if (codes.length > 0) params.filterData.push({ attrName: 'tenantSchema', attrValue: codes });
          continue;
        }

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
    const r = await API.POSTBlob(apiEndpoints.activityLogGlobalExport, this.prepareRequestBody());
    if (r.success) {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'Global_Activity_Log.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } else {
      this.props.enqueueSnackbar(r.errorMessage || 'Export failed', { variant: 'error' });
    }
  }

  async triggerSync() {
    this.setState({ syncing: true });
    try {
      const r = await API.POST(apiEndpoints.activityLogGlobalSync, {});
      this.props.enqueueSnackbar(r.data || 'Sync triggered', { variant: 'info' });
      setTimeout(() => this.search(0), 3000);
    } catch (e) {
      this.props.enqueueSnackbar('Sync failed', { variant: 'error' });
    } finally {
      this.setState({ syncing: false });
    }
  }

  render() {
    return (
      <div className="list-section">
        <div className="filter-section">
          <div />
          <div className="top-button-wrapper" style={{ display: 'flex', gap: '8px' }}>
            <IconButtons
              onClick={() => this.triggerSync()}
              buttonClass="filterIcon"
              label={this.state.syncing ? 'Syncing...' : 'Sync Now'}
              icon="RefreshSVG"
            />
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
            options={{ projects: this.state.tenantOptions }}
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

export default withSnackbar(List);
