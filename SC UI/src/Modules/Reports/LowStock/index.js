import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';
import Tooltip from '@material-ui/core/Tooltip';

import ListCommon from '../../../Shared/List';
import Table from './table';
import Filter from './filter';
import IconButtons from '../../../Shared/Button/IconButtons';
import { API } from '../../../axios';
import { apiEndpoints } from '../../../endpoints';
import { messages } from '../../../messages';

const TILES = [
  { key: 'last1Day',   label: 'New Today',      color: '#e74c3c', bg: '#fdedec', days: 1  },
  { key: 'last3Days',  label: 'Last 3 Days',    color: '#e67e22', bg: '#fdf2e9', days: 3  },
  { key: 'last7Days',  label: 'Last 7 Days',    color: '#f39c12', bg: '#fef9e7', days: 7  },
  { key: 'last30Days', label: 'Last 30 Days',   color: '#27ae60', bg: '#eafaf1', days: 30 },
  { key: 'total',      label: 'Total Low Stock', color: '#5e81f4', bg: '#eef1fe', days: null },
];

function ProjectBreakdown({ byProject }) {
  if (!byProject || byProject.length === 0) {
    return <span style={{ fontSize: 12 }}>No data</span>;
  }
  return (
    <div style={{ minWidth: 160 }}>
      <div style={{ fontWeight: 600, marginBottom: 4, fontSize: 12, borderBottom: '1px solid rgba(255,255,255,0.3)', paddingBottom: 4 }}>
        By Project
      </div>
      {byProject.map((p, i) => (
        <div key={i} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, fontSize: 12, padding: '1px 0' }}>
          <span>{p.project || '—'}</span>
          <span style={{ fontWeight: 600 }}>{p.count}</span>
        </div>
      ))}
    </div>
  );
}

class LowStockReportList extends ListCommon {
  filterData = {};
  sortkey = 'lowStockSince';
  sortby = 'desc';

  state = {
    data: [],
    pages: 0,
    totalRecords: 0,
    isLoading: false,
    filterOpen: false,
    syncing: false,
    tenantOptions: [],
    dropdowns: { categories: [] },
    tiles: {},
    activeTile: null,
  };

  tableData = {
    headers: [],
    keys: [],
  };

  url = apiEndpoints.lowStockList;
  pageSize = 100;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.search();
    this.fetchTenants();
    this.fetchDropdowns();
    this.fetchTiles();
  }

  async fetchDropdowns() {
    const res = await API.GET(apiEndpoints.lowStockDropdowns);
    if (res.success && res.data) {
      this.setState({ dropdowns: res.data });
    }
  }

  async fetchTiles() {
    const res = await API.GET(apiEndpoints.lowStockTiles);
    if (res.success && res.data) {
      this.setState({ tiles: res.data });
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

  handleTileClick(tile) {
    if (tile.days === null) {
      // "total" tile — clear any active filter
      const isActive = this.state.activeTile === tile.key;
      if (isActive) {
        delete this.filterData.startDate;
        delete this.filterData.endDate;
        this.setState({ activeTile: null }, () => this.search(0));
      }
      return;
    }

    const isActive = this.state.activeTile === tile.key;
    if (isActive) {
      delete this.filterData.startDate;
      delete this.filterData.endDate;
      this.setState({ activeTile: null }, () => this.search(0));
      return;
    }

    const end = new Date();
    const start = new Date();
    // "New Today" (days=1) → start = today (same calendar day)
    // "Last N Days" (days>1) → start = midnight (N-1) days ago, so N calendar days total
    if (tile.days === 1) {
      start.setHours(0, 0, 0, 0);
    } else {
      start.setDate(start.getDate() - (tile.days - 1));
      start.setHours(0, 0, 0, 0);
    }

    const fmt = (d) => {
      const dd = String(d.getDate()).padStart(2, '0');
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      return `${dd}-${mm}-${d.getFullYear()}`;
    };

    this.filterData.startDate = fmt(start);
    this.filterData.endDate = fmt(end);
    this.setState({ activeTile: tile.key }, () => this.search(0));
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
    const r = await API.POSTBlob(apiEndpoints.lowStockExport, this.prepareRequestBody());
    if (r.success) {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'low_stock_report.xlsx');
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
      const r = await API.POST(apiEndpoints.lowStockSync, {});
      this.props.enqueueSnackbar((r.data && r.data.message) || 'Sync triggered', { variant: 'info' });
      setTimeout(() => { this.search(0); this.fetchTiles(); }, 2000);
    } catch (e) {
      this.props.enqueueSnackbar('Sync failed', { variant: 'error' });
    } finally {
      this.setState({ syncing: false });
    }
  }

  renderTiles() {
    const { tiles, activeTile } = this.state;
    return (
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        {TILES.map((t) => {
          const data = tiles[t.key] || { total: 0, byProject: [] };
          const isActive = activeTile === t.key;
          const isClickable = t.days !== null;

          const tileEl = (
            <div
              key={t.key}
              onClick={() => this.handleTileClick(t)}
              style={{
                flex: '1 1 130px',
                minWidth: 120,
                background: isActive ? t.color : t.bg,
                border: `2px solid ${t.color}`,
                borderRadius: 8,
                padding: '12px 16px',
                cursor: 'pointer',
                textAlign: 'center',
                transition: 'all 0.15s',
              }}
            >
              <div style={{ fontSize: 24, fontWeight: 700, color: isActive ? '#fff' : t.color }}>
                {data.total || 0}
              </div>
              <div style={{ fontSize: 12, color: isActive ? '#fff' : '#555', marginTop: 2 }}>
                {t.label}
              </div>
              <div style={{ fontSize: 10, color: isActive ? 'rgba(255,255,255,0.8)' : '#aaa', marginTop: 3 }}>
                {isActive ? 'click to clear' : 'click to filter'}
              </div>
            </div>
          );

          return (
            <Tooltip
              key={t.key}
              title={<ProjectBreakdown byProject={data.byProject} />}
              placement="bottom"
              arrow
            >
              {tileEl}
            </Tooltip>
          );
        })}
      </div>
    );
  }

  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            <h2 className="page-title">Low Stock Report</h2>
          </div>
        </div>

        <div className="list-section">
          {this.renderTiles()}

          <div style={{
            fontSize: 12, color: '#888', marginBottom: 8,
            display: 'flex', alignItems: 'center', gap: 4,
          }}>
            <span>🕐</span>
            <span>Data auto-refreshes every 30 minutes. Use <strong>Sync Now</strong> for latest data.</span>
          </div>

          <div className="filter-section">
            <div>
              {this.state.totalRecords > 0 && (
                <span style={{ fontSize: 13, color: '#555' }}>
                  {this.state.totalRecords} product{this.state.totalRecords !== 1 ? 's' : ''} low on stock
                  {this.state.activeTile ? ` · ${TILES.find(t => t.key === this.state.activeTile)?.label}` : ''}
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
                categories: this.state.dropdowns.categories || [],
              }}
              search={(data) => {
                this.filterData = data;
                this.setState({ filterOpen: false, activeTile: null });
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
              tenantMap={Object.fromEntries(this.state.tenantOptions.map(t => [t.id, t.name]))}
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

export default withSnackbar(LowStockReportList);
