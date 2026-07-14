import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';

import ListCommon from '../../../Shared/List';
import Cards from './cards';
import Filter from './filter';
import IconButtons from '../../../Shared/Button/IconButtons';
import { API } from '../../../axios';
import { apiEndpoints } from '../../../endpoints';
import { messages } from '../../../messages';

// Status groups for quick-filter tiles
const STATUS_GROUPS = [
  { key: 'ALL',         label: 'All',          color: '#555',    bg: '#f0f0f0', statuses: null },
  { key: 'PENDING',     label: 'Pending',      color: '#e74c3c', bg: '#fdedec', statuses: ['NEW', 'APPROVED'] },
  { key: 'IN_PROGRESS', label: 'In Progress',  color: '#2980b9', bg: '#ebf5fb', statuses: ['PO CREATED', 'PO PARTIAL', 'INWARD PARTIAL', 'PO COMPLETED'] },
  { key: 'COMPLETED',   label: 'Completed',    color: '#27ae60', bg: '#eafaf1', statuses: ['CLOSED', 'SHORT CLOSED'] },
  { key: 'CANCELLED',   label: 'Cancelled',    color: '#95a5a6', bg: '#f2f3f4', statuses: ['CANCELLED', 'REJECTED'] },
];

// Statuses shown by default — excludes terminal (closed/cancelled) indents
const ACTIVE_STATUSES = ['NEW', 'APPROVED', 'PO CREATED', 'PO PARTIAL', 'INWARD PARTIAL', 'PO COMPLETED'];

class IndentFulfillmentList extends ListCommon {
  filterData = { indentStatus: ACTIVE_STATUSES };

  state = {
    data: [],
    pages: 0,
    totalRecords: 0,
    isLoading: false,
    filterOpen: false,
    projects: [],
    products: [],
    tenantOptions: [],
    stats: {},
    activeGroup: 'ALL',
    usingDefaultFilter: true,
  };

  // Not used for card view but needed by ListCommon
  tableData = { headers: [], keys: [] };
  url = apiEndpoints.indentFulfillmentList;
  pageSize = 100;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.fetchProjects();
    this.fetchProducts();
    this.fetchTenants();
    this.search();
    this.fetchStats();
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

  async fetchProjects() {
    const res = await API.GET(apiEndpoints.indentFulfillmentProjects);
    if (res.success && Array.isArray(res.data)) this.setState({ projects: res.data });
  }

  async fetchProducts() {
    const res = await API.GET(apiEndpoints.indentFulfillmentProducts);
    if (res.success && Array.isArray(res.data)) this.setState({ products: res.data });
  }

  async fetchStats() {
    const r = await API.POST(apiEndpoints.indentFulfillmentStats, this.prepareRequestBody());
    if (r.success && r.data) this.setState({ stats: r.data });
  }

  getGroupCount(group) {
    const { stats } = this.state;
    if (!group.statuses) return Object.values(stats).reduce((s, v) => s + v, 0);
    return group.statuses.reduce((s, st) => s + (stats[st] || 0), 0);
  }

  handleGroupClick(group) {
    const isActive = this.state.activeGroup === group.key;
    if (group.key === 'ALL' || isActive) {
      delete this.filterData.indentStatus;
      this.setState({ activeGroup: 'ALL' }, () => this.search(0));
    } else {
      // Send ALL statuses in this group — backend now supports IN clause
      this.filterData.indentStatus = group.statuses || [];
      this.setState({ activeGroup: group.key }, () => this.search(0));
    }
  }

  prepareRequestBody() {
    const params = { filterData: [] };
    if (this.filterData) {
      for (const field in this.filterData) {
        const value = this.filterData[field];
        if (value === undefined || value === null || value === '' ||
            (Array.isArray(value) && value.length === 0)) continue;
        params.filterData.push({
          attrName: field,
          attrValue: Array.isArray(value) ? value.map(String) : [String(value)],
        });
      }
    }
    return params;
  }

  async search(page = 0) {
    // Use larger page size for card view — more lines per indent make pagination less jarring
    this.url = apiEndpoints.indentFulfillmentList;
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
    const r = await API.POSTBlob(apiEndpoints.indentFulfillmentExport, this.prepareRequestBody());
    if (r.success) {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'indent_fulfillment.xlsx');
      document.body.appendChild(link); link.click(); link.remove();
    } else {
      this.props.enqueueSnackbar(r.errorMessage || 'Export failed', { variant: 'error' });
    }
  }

  renderStatusGroups() {
    const { activeGroup } = this.state;
    return (
      <div style={{ display: 'flex', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
        {STATUS_GROUPS.map((g) => {
          const count = this.getGroupCount(g);
          const isActive = activeGroup === g.key;
          return (
            <div
              key={g.key}
              onClick={() => this.handleGroupClick(g)}
              style={{
                flex: '1 1 110px', minWidth: 100,
                background: isActive ? g.color : g.bg,
                border: `2px solid ${g.color}`,
                borderRadius: 8, padding: '10px 14px',
                cursor: 'pointer', textAlign: 'center', transition: 'all 0.15s',
              }}
            >
              <div style={{ fontSize: 22, fontWeight: 700, color: isActive ? '#fff' : g.color }}>{count}</div>
              <div style={{ fontSize: 11, color: isActive ? '#fff' : '#666', marginTop: 2 }}>{g.label}</div>
            </div>
          );
        })}
      </div>
    );
  }

  render() {
    const activeLabel = STATUS_GROUPS.find(g => g.key === this.state.activeGroup)?.label;

    return (
      <div className="page">
        <div className="header-info">
          <h2 className="page-title">Indent Fulfillment Report</h2>
        </div>

        <div className="list-section">
          {this.renderStatusGroups()}

          <div style={{ fontSize: 12, color: '#888', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 4 }}>
            <span>ℹ️</span>
            <span>
              Each card shows one indent with all its product lines. Click a card header to expand/collapse.
              Overdue need-by dates appear in red.
            </span>
          </div>

          <div className="filter-section" style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
              {this.state.totalRecords > 0 && (
                <span style={{ fontSize: 13, color: '#555' }}>
                  {this.state.totalRecords} line item{this.state.totalRecords !== 1 ? 's' : ''}
                  {this.state.activeGroup !== 'ALL' ? ` · ${activeLabel}` : ''}
                </span>
              )}
              {this.state.usingDefaultFilter && (
                <span style={{
                  display: 'inline-flex', alignItems: 'center', gap: 4,
                  background: '#eef1fe', color: '#3d5afe',
                  border: '1px solid #c5caf9', borderRadius: 12,
                  fontSize: 11, fontWeight: 600, padding: '2px 10px',
                }}>
                  Active indents only
                  <span
                    onClick={() => {
                      delete this.filterData.indentStatus;
                      this.setState({ usingDefaultFilter: false, activeGroup: 'ALL' }, () => {
                        this.search(0);
                        this.fetchStats();
                      });
                    }}
                    style={{ cursor: 'pointer', marginLeft: 2, fontSize: 13, lineHeight: 1 }}
                    title="Show all indents including closed / cancelled"
                  >×</span>
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
              options={{ projects: this.state.projects, products: this.state.products }}
              search={(data) => {
                this.filterData = data;
                this.setState({ filterOpen: false, activeGroup: 'ALL', usingDefaultFilter: false });
                this.search(0);
                this.fetchStats();
              }}
              close={() => this.setState({ filterOpen: false })}
            />
          </Popper>

          {this.state.isLoading
            ? this.renderLoader()
            : <Cards rows={this.state.data} tenantMap={Object.fromEntries(this.state.tenantOptions.map(t => [t.id, t.name]))} />
          }

          {this.renderPagination()}
        </div>
      </div>
    );
  }
}

export default withSnackbar(IndentFulfillmentList);
