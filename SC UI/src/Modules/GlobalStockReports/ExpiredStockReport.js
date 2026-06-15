import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';

import ListCommon from '../../Shared/List';
import ExpiredStockTable from './ExpiredStockTable';
import ExpiredStockFilter from './ExpiredStockFilter';
import IconButtons from '../../Shared/Button/IconButtons';
import { API } from '../../axios';
import { apiEndpoints } from '../../endpoints';
import { messages } from '../../messages';

/**
 * Tiles:
 *   expired  — daysUntilExpiry < 0
 *   within1  — ≤ 1 day  (cumulative, includes expired)
 *   within10 — ≤ 10 days
 *   within30 — ≤ 30 days
 *   within90 — ≤ 90 days
 *   total    — all with expiry tracked
 */
const TILES = [
  { key: 'expired',  label: 'Already Expired', color: '#b71c1c', bg: '#ffebee', icon: '⚠️', filter: 'expired' },
  { key: 'within1',  label: 'Within 1 Day',    color: '#c62828', bg: '#ffcdd2', icon: '🔴', filter: 'within1' },
  { key: 'within10', label: 'Within 10 Days',  color: '#e65100', bg: '#fbe9e7', icon: '🟠', filter: 'within10' },
  { key: 'within30', label: 'Within 30 Days',  color: '#ef6c00', bg: '#fff3e0', icon: '🟡', filter: 'within30' },
  { key: 'within90', label: 'Within 90 Days',  color: '#f9a825', bg: '#fffde7', icon: '🕐', filter: 'within90' },
  { key: 'total',    label: 'Total Tracked',   color: '#37474f', bg: '#eceff1', icon: '📦', filter: null },
];

class ExpiredStockReport extends ListCommon {
  filterData = {};
  sortkey = 'daysUntilExpiry';
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
    activeTileFilter: null,
  };

  tableData = { headers: [], keys: [] };
  url = apiEndpoints.expiredStockReportList;
  pageSize = 100;

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
    const res = await API.GET(apiEndpoints.expiredStockReportDropdowns);
    if (res.success && res.data) {
      this.setState({ dropdowns: res.data });
    }
  }

  async fetchTiles() {
    this.setState({ tilesLoading: true });
    const res = await API.GET(apiEndpoints.expiredStockReportTiles);
    if (res.success && res.data) {
      this.setState({ tiles: res.data, tilesLoading: false });
    } else {
      this.setState({ tilesLoading: false });
    }
  }

  prepareRequestBody() {
    const params = { filterData: [] };
    const fd = { ...this.filterData };

    // active tile filter injects expiryFilter
    if (this.state.activeTileFilter) {
      fd.expiryFilter = this.state.activeTileFilter;
    }

    if (!fd || Object.keys(fd).length === 0) return params;

    for (const field in fd) {
      const value = fd[field];
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
    const res = await API.POSTBlob(apiEndpoints.expiredStockReportExport, this.prepareRequestBody());
    if (res.success) {
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'expired_stock_report.xlsx');
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
      const res = await API.POST(apiEndpoints.expiredStockReportSync, {});
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

  handleTileClick(tile) {
    if (!tile.filter) {
      // "total" tile — clear tile filter
      this.setState({ activeTileFilter: null }, () => this.search(0));
      return;
    }
    const same = this.state.activeTileFilter === tile.filter;
    this.setState({ activeTileFilter: same ? null : tile.filter }, () => this.search(0));
  }

  renderTiles() {
    const { tiles, tilesLoading, activeTileFilter } = this.state;
    return (
      <div style={{ display: 'flex', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
        {TILES.map(t => {
          const isActive = activeTileFilter === t.filter && t.filter !== null;
          return (
            <div
              key={t.key}
              onClick={() => this.handleTileClick(t)}
              style={{
                flex: '1 1 110px',
                minWidth: 100,
                background: t.bg,
                border: `2px solid ${isActive ? t.color : t.color + '80'}`,
                borderRadius: 8,
                padding: '10px 14px',
                textAlign: 'center',
                cursor: 'pointer',
                boxShadow: isActive ? `0 0 0 2px ${t.color}40` : 'none',
                transition: 'all 0.15s',
              }}
            >
              <div style={{ fontSize: 22, fontWeight: 700, color: t.color }}>
                {tilesLoading ? '…' : (tiles[t.key] != null ? tiles[t.key] : '—')}
              </div>
              <div style={{ fontSize: 11, color: '#555', marginTop: 2 }}>
                {t.icon} {t.label}
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  render() {
    const { tenantOptions, dropdowns, totalRecords, filterOpen, syncing, activeTileFilter } = this.state;
    const tenantMap = Object.fromEntries(tenantOptions.map(t => [t.id, t.name]));
    const activeTile = activeTileFilter ? TILES.find(t => t.filter === activeTileFilter) : null;

    return (
      <div className="page">
        <div className="header-info">
          <div>
            <h2 className="page-title">Expired Stock Report</h2>
          </div>
        </div>

        <div className="list-section">
          {this.renderTiles()}

          {activeTile && (
            <div style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{
                background: activeTile.bg,
                color: activeTile.color,
                border: `1px solid ${activeTile.color}`,
                fontSize: 12,
                fontWeight: 600,
                padding: '3px 10px',
                borderRadius: 12,
              }}>
                {activeTile.icon} {activeTile.label}
              </span>
              <span
                style={{ cursor: 'pointer', color: '#999', fontSize: 12 }}
                onClick={() => this.setState({ activeTileFilter: null }, () => this.search(0))}
              >
                ✕ clear filter
              </span>
            </div>
          )}

          <div style={{ fontSize: 12, color: '#888', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
            <span>🕐</span>
            <span>Data refreshes once daily at 2 AM. Use <strong>Sync Now</strong> for latest data.</span>
          </div>

          <div className="filter-section">
            <div>
              {totalRecords > 0 && (
                <span style={{ fontSize: 13, color: '#555' }}>
                  {totalRecords} batch{totalRecords !== 1 ? 'es' : ''} with expiry data
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
            <ExpiredStockFilter
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
            <ExpiredStockTable
              rows={this.state.data}
              tenantMap={tenantMap}
            />
          )}

          {this.renderPagination()}
        </div>
      </div>
    );
  }
}

export default withSnackbar(ExpiredStockReport);
