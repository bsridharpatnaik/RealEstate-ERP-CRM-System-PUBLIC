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

const RECON_STATUSES = [
  { key: 'ALL',         label: 'All',          color: '#555',    bg: '#f0f0f0' },
  { key: 'NOT_STARTED', label: 'Not Started',  color: '#95a5a6', bg: '#f2f3f4' },
  { key: 'PARTIAL',     label: 'Partial',      color: '#f39c12', bg: '#fef9e7' },
  { key: 'COMPLETE',    label: 'Complete',     color: '#27ae60', bg: '#eafaf1' },
];

class PoReconList extends ListCommon {
  filterData = {};
  sortkey = 'poDate';
  sortby = 'desc';

  state = {
    data: [],
    pages: 0,
    totalRecords: 0,
    isLoading: false,
    filterOpen: false,
    projects: [],
    stats: { COMPLETE: 0, PARTIAL: 0, NOT_STARTED: 0 },
    activeReconStatus: 'ALL',
  };

  tableData = { headers: [], keys: [] };
  url = apiEndpoints.poReconList;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.fetchProjects();
    this.search();
    this.fetchStats();
  }

  async fetchProjects() {
    const res = await API.GET(apiEndpoints.poReconProjects);
    if (res.success && Array.isArray(res.data)) this.setState({ projects: res.data });
  }

  async fetchStats() {
    const r = await API.POST(apiEndpoints.poReconStats, this.prepareRequestBody());
    if (r.success && r.data) this.setState({ stats: r.data });
  }

  handleStatusChip(key) {
    const isActive = this.state.activeReconStatus === key;
    if (key === 'ALL' || isActive) {
      delete this.filterData.reconciliationStatus;
      this.setState({ activeReconStatus: 'ALL' }, () => this.search(0));
    } else {
      this.filterData.reconciliationStatus = key;
      this.setState({ activeReconStatus: key }, () => this.search(0));
    }
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
    const r = await API.POSTBlob(apiEndpoints.poReconExport, this.prepareRequestBody());
    if (r.success) {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'po_inward_reconciliation.xlsx');
      document.body.appendChild(link); link.click(); link.remove();
    } else {
      this.props.enqueueSnackbar(r.errorMessage || 'Export failed', { variant: 'error' });
    }
  }

  renderStatCards() {
    const { stats, activeReconStatus } = this.state;
    const total = (stats.COMPLETE || 0) + (stats.PARTIAL || 0) + (stats.NOT_STARTED || 0);
    return (
      <div style={{ display: 'flex', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
        {RECON_STATUSES.map((s) => {
          const count = s.key === 'ALL' ? total : (stats[s.key] || 0);
          const isActive = activeReconStatus === s.key;
          return (
            <div
              key={s.key}
              onClick={() => this.handleStatusChip(s.key)}
              style={{
                flex: '1 1 110px', minWidth: 100,
                background: isActive ? s.color : s.bg,
                border: `2px solid ${s.color}`,
                borderRadius: 8, padding: '10px 14px',
                cursor: 'pointer', textAlign: 'center', transition: 'all 0.15s',
              }}
            >
              <div style={{ fontSize: 22, fontWeight: 700, color: isActive ? '#fff' : s.color }}>{count}</div>
              <div style={{ fontSize: 11, color: isActive ? '#fff' : '#666', marginTop: 2 }}>{s.label}</div>
            </div>
          );
        })}
        {total > 0 && (
          <div style={{
            flex: '1 1 160px', minWidth: 150, display: 'flex',
            alignItems: 'center', justifyContent: 'center',
            background: '#f8f9fa', border: '1px solid #dee2e6',
            borderRadius: 8, padding: '10px 14px',
          }}>
            <div style={{ width: '100%' }}>
              <div style={{ height: 8, background: '#eee', borderRadius: 4, overflow: 'hidden', display: 'flex' }}>
                <div style={{ width: `${((stats.COMPLETE || 0) / total * 100).toFixed(1)}%`, background: '#27ae60' }} />
                <div style={{ width: `${((stats.PARTIAL || 0) / total * 100).toFixed(1)}%`, background: '#f39c12' }} />
              </div>
              <div style={{ fontSize: 11, color: '#888', marginTop: 4, textAlign: 'center' }}>
                {total > 0 ? `${((stats.COMPLETE || 0) / total * 100).toFixed(0)}% fully received` : ''}
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  render() {
    return (
      <div className="page">
        <div className="header-info">
          <h2 className="page-title">PO vs Inward Reconciliation</h2>
        </div>

        <div className="list-section">
          {this.renderStatCards()}

          <div style={{ fontSize: 12, color: '#888', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
            <span>ℹ️</span>
            <span>Shows ordered vs received quantity per PO line item. Click a status card to quick-filter.</span>
          </div>

          <div className="filter-section">
            <div>
              {this.state.totalRecords > 0 && (
                <span style={{ fontSize: 13, color: '#555' }}>
                  {this.state.totalRecords} line{this.state.totalRecords !== 1 ? 's' : ''}
                  {this.state.activeReconStatus !== 'ALL' ? ` · ${this.state.activeReconStatus}` : ''}
                </span>
              )}
            </div>
            <div className="top-button-wrapper" style={{ display: 'flex', gap: 8 }}>
              <IconButtons onClick={() => this.exportExcel()} buttonClass="filterIcon"
                label="Export Excel" icon="DownloadSVG" />
              <IconButtons onClick={() => this.setState({ filterOpen: true })} buttonClass="filterIcon"
                label={messages.common.filter} icon="FilterSVG" innerRef={this.filterRef} />
            </div>
          </div>

          <Popper open={this.state.filterOpen} anchorEl={this.filterRef && this.filterRef.current}
            placement="bottom-end" style={{ zIndex: 1300 }}>
            <Filter
              filterData={this.filterData}
              options={{ projects: this.state.projects }}
              search={(data) => {
                this.filterData = data;
                this.setState({ filterOpen: false, activeReconStatus: 'ALL' });
                this.search(0);
                this.fetchStats();
              }}
              close={() => this.setState({ filterOpen: false })}
            />
          </Popper>

          {this.state.isLoading ? this.renderLoader() : (
            <Table
              tableData={this.tableData}
              rows={this.state.data}
              hideedit hidedelete
              sortkey={this.sortkey}
              sortby={this.sortby}
              search={(sortkey, sortby) => { this.sortkey = sortkey; this.sortby = sortby; this.search(); }}
            />
          )}

          {this.renderPagination()}
        </div>
      </div>
    );
  }
}

export default withSnackbar(PoReconList);
