import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';

import ListCommon from '../../Shared/List';
import DeadStockTable from './DeadStockTable';
import DeadStockFilter from './DeadStockFilter';
import IconButtons from '../../Shared/Button/IconButtons';
import { API } from '../../axios';
import { apiEndpoints } from '../../endpoints';
import { messages } from '../../messages';

const TILES = [
  { key: 'uniqueProducts',       label: 'Unique Products',        color: '#b71c1c', bg: '#ffebee', icon: '📦' },
  { key: 'projectsAffected',    label: 'Projects Affected',      color: '#1565c0', bg: '#e3f2fd', icon: '🏗️' },
  { key: 'totalValue',          label: 'Total Dead Stock Value',  color: '#4a148c', bg: '#f3e5f5', icon: '₹',  isValue: true },
  { key: 'batchTrackedProducts', label: 'Batch-Tracked Products', color: '#e65100', bg: '#fff3e0', icon: '🏷️' },
];

class DeadStockReport extends ListCommon {
  filterData = {};
  sortkey = 'productName';
  sortby = 'asc';

  state = {
    data: [],
    pages: 0,
    totalRecords: 0,
    isLoading: false,
    syncing: false,
    filterOpen: false,
    tenantOptions: [],
    dropdowns: { categories: [] },
    tiles: {},
    tilesLoading: false,
  };

  tableData = { headers: [], keys: [] };
  url = apiEndpoints.deadStockReportList;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.fetchTenants();
    this.fetchDropdowns();
    this.fetchTiles();
    this.search();
  }

  async fetchTenants() {
    const res = await API.GET(apiEndpoints.getTenants);
    if (res.success) {
      const tenantOptions = (res.data || [])
        .filter(t => t.inventory === true)
        .map(t => ({ name: t.tenantName || t.name || '', id: t.tenantCode }))
        .filter(t => t.name && t.id);
      this.setState({ tenantOptions });
    }
  }

  async fetchDropdowns() {
    const res = await API.GET(apiEndpoints.deadStockReportDropdowns);
    if (res.success && res.data) {
      this.setState({ dropdowns: res.data });
    }
  }

  async fetchTiles() {
    this.setState({ tilesLoading: true });
    const res = await API.GET(apiEndpoints.deadStockReportTiles);
    if (res.success && res.data) {
      this.setState({ tiles: res.data, tilesLoading: false });
    } else {
      this.setState({ tilesLoading: false });
    }
  }

  prepareRequestBody() {
    const params = { filterData: [] };
    if (!this.filterData) return params;
    for (const field in this.filterData) {
      const value = this.filterData[field];
      if (value === undefined || value === null || value === '') continue;
      if (field === 'tenantSchema') {
        const arr = Array.isArray(value) ? value : [value];
        const codes = arr.map(p => (p && typeof p === 'object' ? p.id : p)).filter(Boolean);
        if (codes.length > 0) params.filterData.push({ attrName: 'tenantSchema', attrValue: codes });
        continue;
      }
      params.filterData.push({
        attrName: field,
        attrValue: Array.isArray(value) ? value.map(String) : [String(value)],
      });
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
    const res = await API.POSTBlob(apiEndpoints.deadStockReportExport, this.prepareRequestBody());
    if (res.success) {
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'dead_stock_report.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } else {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar('Export failed', { variant: 'error' });
    }
  }

  async triggerSync() {
    this.setState({ syncing: true });
    try {
      const res = await API.POST(apiEndpoints.deadStockReportSync, {});
      this.props.enqueueSnackbar && this.props.enqueueSnackbar(
        (res.data && res.data.message) || 'Sync triggered', { variant: 'info' }
      );
      setTimeout(() => { this.search(0); this.fetchTiles(); this.fetchDropdowns(); }, 2000);
    } catch (e) {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar('Sync failed', { variant: 'error' });
    } finally {
      this.setState({ syncing: false });
    }
  }

  renderTiles() {
    const { tiles, tilesLoading } = this.state;
    return (
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        {TILES.map(t => (
          <div
            key={t.key}
            style={{
              flex: '1 1 130px',
              minWidth: 120,
              background: t.bg,
              border: `2px solid ${t.color}`,
              borderRadius: 8,
              padding: '12px 16px',
              textAlign: 'center',
            }}
          >
            <div style={{ fontSize: 24, fontWeight: 700, color: t.color }}>
              {tilesLoading ? '…' : (
                t.isValue
                  ? (tiles[t.key] != null ? `₹${Number(tiles[t.key]).toLocaleString('en-IN', { maximumFractionDigits: 0 })}` : '—')
                  : (tiles[t.key] != null ? tiles[t.key] : '—')
              )}
            </div>
            <div style={{ fontSize: 12, color: '#555', marginTop: 2 }}>
              {t.icon} {t.label}
            </div>
          </div>
        ))}
      </div>
    );
  }

  render() {
    const { tenantOptions, dropdowns, totalRecords, filterOpen, syncing } = this.state;
    const tenantMap = Object.fromEntries(tenantOptions.map(t => [t.id, t.name]));

    return (
      <div className="page">
        <div className="header-info">
          <div>
            <h2 className="page-title">Dead Stock Report</h2>
          </div>
        </div>

        <div className="list-section">
          {this.renderTiles()}

          <div style={{ fontSize: 12, color: '#888', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
            <span>🕐</span>
            <span>Data auto-refreshes every 30 minutes. Use <strong>Sync Now</strong> for latest data.</span>
          </div>

          <div className="filter-section">
            <div>
              {totalRecords > 0 && (
                <span style={{ fontSize: 13, color: '#555' }}>
                  {totalRecords} product{totalRecords !== 1 ? 's' : ''} in dead stock
                </span>
              )}
            </div>
            <div className="top-button-wrapper" style={{ display: 'flex', gap: '8px' }}>
              <IconButtons
                onClick={() => this.triggerSync()}
                buttonClass="filterIcon"
                label={syncing ? 'Syncing...' : 'Sync Now'}
                icon="RefreshSVG"
                disabled={syncing}
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
            open={filterOpen}
            anchorEl={this.filterRef && this.filterRef.current}
            placement="bottom-end"
            style={{ zIndex: 1300 }}
          >
            <DeadStockFilter
              filterData={this.filterData}
              options={{
                projects: tenantOptions,
                categories: dropdowns.categories || [],
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
            <DeadStockTable
              tableData={this.tableData}
              rows={this.state.data}
              hideedit
              hidedelete
              sortkey={this.sortkey}
              sortby={this.sortby}
              tenantMap={tenantMap}
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

export default withSnackbar(DeadStockReport);
