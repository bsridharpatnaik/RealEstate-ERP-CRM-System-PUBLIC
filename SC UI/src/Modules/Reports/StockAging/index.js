import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';

import ListCommon from '../../../Shared/List';
import Cards from './cards';
import Filter from './filter';
import DetailPopup from './detailPopup';
import IconButtons from '../../../Shared/Button/IconButtons';
import { API } from '../../../axios';
import { apiEndpoints } from '../../../endpoints';
import { messages } from '../../../messages';

const BUCKET_CONFIG = [
  { key: '0-30',  label: '0–30 Days',  color: '#27ae60', bg: '#eafaf1' },
  { key: '31-60', label: '31–60 Days', color: '#f39c12', bg: '#fef9e7' },
  { key: '61-90', label: '61–90 Days', color: '#e67e22', bg: '#fdf2e9' },
  { key: '90+',   label: '90+ Days',   color: '#e74c3c', bg: '#fdedec' },
];

class StockAgingList extends ListCommon {
  filterData = {};
  state = {
    data: [],
    pages: 0,
    totalRecords: 0,
    isLoading: false,
    filterOpen: false,
    syncing: false,
    tenantOptions: [],
    dropdowns: { products: [], categories: [] },
    tiles: { '0-30': 0, '31-60': 0, '61-90': 0, '90+': 0 },
    activeBucket: null,
    detailOpen: false,
    detailRow: null,
    detailData: [],
    detailLoading: false,
  };

  url = apiEndpoints.stockAgingList;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.search();
    this.fetchTenants();
    this.fetchDropdowns();
    this.fetchTiles();
  }

  async fetchTiles() {
    const res = await API.GET(apiEndpoints.stockAgingTiles);
    if (res.success && res.data) {
      this.setState({ tiles: res.data });
    }
  }

  async fetchDropdowns() {
    const res = await API.GET(apiEndpoints.stockAgingDropdowns);
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

    // Active bucket tile acts as a filter
    if (this.state.activeBucket) {
      params.filterData.push({ attrName: 'agingBucket', attrValue: [this.state.activeBucket] });
    }

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
    this.url = apiEndpoints.stockAgingList;
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
    const r = await API.POSTBlob(apiEndpoints.stockAgingExport, this.prepareRequestBody());
    if (r.success) {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'stock_aging_report.xlsx');
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
      const r = await API.POST(apiEndpoints.stockAgingSync, {});
      this.props.enqueueSnackbar((r.data && r.data.message) || 'Sync triggered', { variant: 'info' });
      setTimeout(() => {
        this.search(0);
        this.fetchTiles();
      }, 3000);
    } catch (e) {
      this.props.enqueueSnackbar('Sync failed', { variant: 'error' });
    } finally {
      this.setState({ syncing: false });
    }
  }

  handleTileClick(bucketKey) {
    const newBucket = this.state.activeBucket === bucketKey ? null : bucketKey;
    this.setState({ activeBucket: newBucket }, () => this.search(0));
  }

  async openDetail(row) {
    this.setState({ detailOpen: true, detailRow: row, detailData: [], detailLoading: true });
    const url = `${apiEndpoints.stockAgingDetail(row.productId)}?tenantSchema=${encodeURIComponent(row.tenantSchema)}`;
    const res = await API.GET(url);
    if (res.success && Array.isArray(res.data)) {
      this.setState({ detailData: res.data, detailLoading: false });
    } else {
      this.setState({ detailLoading: false });
    }
  }

  renderTiles() {
    const { tiles, activeBucket } = this.state;
    return (
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        {BUCKET_CONFIG.map((b) => (
          <div
            key={b.key}
            onClick={() => this.handleTileClick(b.key)}
            style={{
              flex: '1 1 120px',
              background: activeBucket === b.key ? b.color : b.bg,
              border: `2px solid ${b.color}`,
              borderRadius: 8,
              padding: '12px 16px',
              cursor: 'pointer',
              textAlign: 'center',
              transition: 'all 0.15s',
              minWidth: 100,
            }}
          >
            <div style={{
              fontSize: 22,
              fontWeight: 700,
              color: activeBucket === b.key ? '#fff' : b.color,
            }}>
              {tiles[b.key] || 0}
            </div>
            <div style={{
              fontSize: 12,
              color: activeBucket === b.key ? '#fff' : '#555',
              marginTop: 2,
            }}>
              {b.label}
            </div>
          </div>
        ))}
      </div>
    );
  }

  render() {
    const { detailOpen, detailRow, detailData, detailLoading } = this.state;

    return (
      <div className="page">
        <div className="header-info">
          <div>
            <h2 className="page-title">Stock Aging Report</h2>
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
                  {this.state.totalRecords} project-product row{this.state.totalRecords !== 1 ? 's' : ''}
                  {this.state.activeBucket ? ` · ${this.state.activeBucket} days` : ''}
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
                categories: this.state.dropdowns.categories || [],
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
            <Cards rows={this.state.data} onRowClick={(row) => this.openDetail(row)} tenantMap={Object.fromEntries(this.state.tenantOptions.map(t => [t.id, t.name]))} />
          )}

          {this.renderPagination()}
        </div>

        {/* Per-warehouse detail popup */}
        <Dialog
          open={detailOpen}
          onClose={() => this.setState({ detailOpen: false })}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>
              {detailRow ? `${detailRow.productName} — ${detailRow.tenantSchema}` : 'Warehouse Breakdown'}
            </span>
            <IconButton size="small" onClick={() => this.setState({ detailOpen: false })}>
              <CloseIcon />
            </IconButton>
          </DialogTitle>
          <DialogContent style={{ paddingBottom: 20 }}>
            <DetailPopup
              row={detailRow}
              data={detailData}
              loading={detailLoading}
            />
          </DialogContent>
        </Dialog>
      </div>
    );
  }
}

export default withSnackbar(StockAgingList);
