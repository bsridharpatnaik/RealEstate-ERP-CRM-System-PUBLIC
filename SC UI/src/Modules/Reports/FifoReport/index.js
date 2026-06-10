import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';
import Tooltip from '@material-ui/core/Tooltip';

import ListCommon from '../../../Shared/List';
import Cards from './cards';
import Filter from './filter';
import IconButtons from '../../../Shared/Button/IconButtons';
import { API } from '../../../axios';
import { apiEndpoints } from '../../../endpoints';
import { messages } from '../../../messages';

const TILES = [
  { key: 'last7Days',      label: 'Outwards (Last 7d)',  color: '#5e81f4', bg: '#eef1fe', days: 7  },
  { key: 'last30Days',     label: 'Outwards (Last 30d)', color: '#27ae60', bg: '#eafaf1', days: 30 },
  { key: 'last90Days',     label: 'Outwards (Last 90d)', color: '#f39c12', bg: '#fef9e7', days: 90 },
  { key: 'uniqueProducts', label: 'Products Overridden',  color: '#8e44ad', bg: '#f5eef8', days: null },
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

class FifoReportList extends ListCommon {
  filterData = {};
  state = {
    data: [],
    pages: 0,
    totalRecords: 0,
    isLoading: false,
    filterOpen: false,
    syncing: false,
    tenantOptions: [],
    dropdowns: { products: [], contractors: [], performedBy: [] },
    tiles: {},
    activeTile: null,
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
    this.fetchTiles();
  }

  async fetchDropdowns() {
    const res = await API.GET(apiEndpoints.fifoReportDropdowns);
    if (res.success && res.data) {
      this.setState({ dropdowns: res.data });
    }
  }

  async fetchTiles() {
    const res = await API.GET(apiEndpoints.fifoReportTiles);
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

  // Apply a date-window filter when a time-based tile is clicked
  handleTileClick(tile) {
    if (tile.days === null) return; // uniqueProducts tile — info only, no filter

    const isActive = this.state.activeTile === tile.key;
    if (isActive) {
      // Deactivate — clear date filter
      delete this.filterData.startDate;
      delete this.filterData.endDate;
      this.setState({ activeTile: null }, () => this.search(0));
      return;
    }

    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - tile.days);

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
    this.url = apiEndpoints.fifoReportList.replace('size=20', 'size=50');
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
      setTimeout(() => { this.search(0); this.fetchTiles(); }, 3000);
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
              onClick={() => isClickable && this.handleTileClick(t)}
              style={{
                flex: '1 1 130px',
                minWidth: 120,
                background: isActive ? t.color : t.bg,
                border: `2px solid ${t.color}`,
                borderRadius: 8,
                padding: '12px 16px',
                cursor: isClickable ? 'pointer' : 'default',
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
              {isClickable && (
                <div style={{ fontSize: 10, color: isActive ? 'rgba(255,255,255,0.8)' : '#aaa', marginTop: 3 }}>
                  {isActive ? 'click to clear' : 'click to filter'}
                </div>
              )}
            </div>
          );

          // Wrap in tooltip showing project breakdown
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
            <h2 className="page-title">FIFO Override Report</h2>
          </div>
        </div>

        <div className="list-section">
          {this.renderTiles()}

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
                  {this.state.totalRecords} batch override{this.state.totalRecords !== 1 ? 's' : ''}
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
                products: this.state.dropdowns.products || [],
                contractors: this.state.dropdowns.contractors || [],
                performedBy: this.state.dropdowns.performedBy || [],
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
            <Cards rows={this.state.data} tenantMap={Object.fromEntries(this.state.tenantOptions.map(t => [t.id, t.name]))} />
          )}

          {this.renderPagination()}
        </div>
      </div>
    );
  }
}

export default withSnackbar(FifoReportList);
