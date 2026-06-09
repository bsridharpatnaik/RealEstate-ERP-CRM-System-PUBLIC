import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';

import ListCommon from '../../../Shared/List';
import Table from './table';
import Filter from './filter';
import IconButtons from '../../../Shared/Button/IconButtons';
import { API } from '../../../axios';
import { apiEndpoints } from '../../../endpoints';
import { messages } from '../../../messages';

class FifoReportList extends ListCommon {
  filterData = {};
  sortkey = 'outwardDate';
  sortby = 'desc';

  state = {
    data: [],
    pages: 0,
    totalRecords: 0,
    isLoading: false,
    filterOpen: false,
    syncing: false,
    tenantOptions: [],
    dropdowns: { products: [], contractors: [], performedBy: [] },
  };

  tableData = {
    headers: [
      'Date', 'Project', 'Outward ID', 'Product', 'Unit',
      'Warehouse', 'Structure', 'Final Location', 'Contractor',
      'Lot #', 'Brand', 'Recv. Date', 'Expiry Date',
      'Qty', 'Override Reason', 'Performed By',
    ],
    keys: [
      'outwardDate', 'tenantSchema', 'outwardId', 'productName', 'measurementUnit',
      'warehouseName', 'usageLocationName', 'usageAreaName', 'contractorName',
      'batchLotNumber', 'batchBrand', 'batchReceivedDate', 'batchExpiryDate',
      'qtyConsumed', 'overrideComment', 'performedBy',
    ],
  };

  url = apiEndpoints.fifoReportList;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.search();
    this.fetchTenants();
    this.fetchDropdowns();
  }

  async fetchDropdowns() {
    const res = await API.GET(apiEndpoints.fifoReportDropdowns);
    if (res.success && res.data) {
      this.setState({ dropdowns: res.data });
    }
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
    const r = await API.POSTBlob(apiEndpoints.fifoReportExport, this.prepareRequestBody());
    if (r.success) {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'fifo_override_report.xlsx');
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
      const r = await API.POST(apiEndpoints.fifoReportSync, {});
      this.props.enqueueSnackbar((r.data && r.data.message) || 'Sync triggered', { variant: 'info' });
      setTimeout(() => this.search(0), 3000);
    } catch (e) {
      this.props.enqueueSnackbar('Sync failed', { variant: 'error' });
    } finally {
      this.setState({ syncing: false });
    }
  }

  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            <h2 className="page-title">FIFO Override Report</h2>
          </div>
        </div>

        <div className="list-section">
          <div style={{
            fontSize: 12, color: '#888', marginBottom: 8,
            display: 'flex', alignItems: 'center', gap: 4,
          }}>
            <span>🕐</span>
            <span>Data auto-refreshes every hour. Use <strong>Sync Now</strong> for latest data.</span>
          </div>

          <div className="filter-section">
            <div>
              {this.state.totalRecords > 0 && (
                <span style={{ fontSize: 13, color: '#555' }}>
                  {this.state.totalRecords} record{this.state.totalRecords !== 1 ? 's' : ''}
                </span>
              )}
            </div>
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
              options={{
                projects: this.state.tenantOptions,
                products: this.state.dropdowns.products || [],
                contractors: this.state.dropdowns.contractors || [],
                performedBy: this.state.dropdowns.performedBy || [],
              }}
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
      </div>
    );
  }
}

export default withSnackbar(FifoReportList);
