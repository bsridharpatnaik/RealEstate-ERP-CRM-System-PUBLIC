import React, { Component } from 'react';
import { API } from '../../../axios';
import { apiEndpoints } from '../../../endpoints';
import { withSnackbar } from 'notistack';
import CircularProgress from '@material-ui/core/CircularProgress';
import Modal from '@material-ui/core/Modal';

const BUCKET_STYLE = {
  on_track: { color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf', label: 'On Track' },
  at_risk:  { color: '#d35400', bg: '#fdf2e9', border: '#f5cba7', label: 'At Risk'  },
  over:     { color: '#c0392b', bg: '#fdedec', border: '#f1948a', label: 'Exceeded' },
  no_boq:   { color: '#7f8c8d', bg: '#f2f3f4', border: '#d5d8dc', label: 'No BOQ'  },
};

const GAP_FILTERS = [
  { value: '',                   label: 'All Products' },
  { value: 'BOQ_NO_OUTWARD',    label: 'BOQ set, no outward' },
  { value: 'OUTWARD_EXCEEDS_BOQ', label: 'Outward exceeds BOQ' },
  { value: 'NO_BOQ_HAS_ACTIVITY', label: 'No BOQ, has activity' },
  { value: 'BOQ_NO_INDENT',     label: 'BOQ set, no indent' },
];

const BUCKET_FILTERS = [
  { value: '',         label: 'All Statuses' },
  { value: 'on_track', label: 'On Track' },
  { value: 'at_risk',  label: 'At Risk (80–100%)' },
  { value: 'over',     label: 'Exceeded BOQ' },
  { value: 'no_boq',   label: 'No BOQ Set' },
];

const CONSUMPTION_FILTERS = [
  { value: '',       label: 'Any % Consumed' },
  { value: 'zero',   label: '0% (nothing used)' },
  { value: 'lt50',   label: '< 50%' },
  { value: '50_80',  label: '50–80%' },
  { value: '80_100', label: '80–100%' },
  { value: 'gt100',  label: '> 100% (over)' },
  { value: 'no_boq', label: 'No BOQ' },
];

// Client-side match on a row's consumedPct. No-BOQ products are ALWAYS surfaced under a
// percentage bucket — they're the ones still needing a BOQ, so we never hide them. The
// explicit 'no_boq' option selects only No-BOQ rows.
function matchesConsumption(row, filter) {
  if (!filter) return true;
  const noBoq = row.boqPlanned == null;
  if (filter === 'no_boq') return noBoq;
  if (noBoq) return true;
  const p = row.consumedPct;
  if (p == null) return true;
  switch (filter) {
    case 'zero':   return p === 0;
    case 'lt50':   return p > 0 && p < 50;
    case '50_80':  return p >= 50 && p < 80;
    case '80_100': return p >= 80 && p <= 100;
    case 'gt100':  return p > 100;
    default:       return true;
  }
}

class BOQTracker extends Component {
  // Props:
  //   tenantCode  — when set (global page), all API calls send this as tenant-id header
  //                 when absent (project page), axios uses the Redux tenant-id as normal

  state = {
    rows: [],
    allCategories: [],
    globalTotals: null,
    loading: true,
    search: '',
    selectedCategory: '',
    gapFilter: '',
    bucketFilter: '',       // client-side status filter — no re-fetch needed
    consumptionFilter: '',  // client-side % consumed filter — no re-fetch needed
    isFiltered: false,
    drillModal: null,
  };

  componentDidMount() {
    if (!('tenantCode' in this.props) || this.props.tenantCode) this.fetchUnfiltered();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.tenantCode !== this.props.tenantCode && this.props.tenantCode) {
      this.setState({ rows: [], allCategories: [], globalTotals: null, search: '', selectedCategory: '', gapFilter: '', bucketFilter: '', consumptionFilter: '', isFiltered: false });
      this.fetchUnfiltered();
    }
  }

  apiConfig() {
    // When tenantCode prop is provided (global mode), override tenant-id header per request
    return this.props.tenantCode
      ? { headers: { 'tenant-id': this.props.tenantCode } }
      : {};
  }

  async fetchUnfiltered() {
    this.setState({ loading: true });
    const res = await API.GET(apiEndpoints.boqTracker, this.apiConfig());
    if (res.success) {
      const data = res.data || [];
      const allCategories = [...new Set(data.map(r => r.categoryName).filter(Boolean))].sort();
      this.setState({ rows: data, allCategories, globalTotals: this.computeTotals(data), loading: false, isFiltered: false });
    } else {
      this.props.enqueueSnackbar(res.errorMessage || 'Failed to load BOQ Tracker', { variant: 'error' });
      this.setState({ loading: false });
    }
  }

  computeTotals(data) {
    return {
      totalProducts: data.length,
      noBOQWithOutward: data.filter(r => r.bucket === 'no_boq' && r.totalOutward != null).length,
      atRiskCount: data.filter(r => r.bucket === 'at_risk').length,
      exceededCount: data.filter(r => r.bucket === 'over').length,
      noBOQCount: data.filter(r => r.bucket === 'no_boq').length,
    };
  }

  applyFilters = async () => {
    const { selectedCategory, search, gapFilter } = this.state;
    const isFiltered = !!(selectedCategory || search || gapFilter);
    this.setState({ loading: true });
    const params = new URLSearchParams();
    if (selectedCategory) params.append('category', selectedCategory);
    if (search) params.append('product', search);
    if (gapFilter) params.append('gapFilter', gapFilter);
    const url = apiEndpoints.boqTracker + (params.toString() ? '?' + params.toString() : '');
    const res = await API.GET(url, this.apiConfig());
    if (res.success) {
      this.setState({ rows: res.data || [], loading: false, isFiltered });
    } else {
      this.props.enqueueSnackbar(res.errorMessage || 'Failed to load BOQ Tracker', { variant: 'error' });
      this.setState({ loading: false });
    }
  };

  clearFilters = () =>
    this.setState({ search: '', selectedCategory: '', gapFilter: '', bucketFilter: '', consumptionFilter: '' }, this.fetchUnfiltered);

  async openDrill(row) {
    this.setState({ drillModal: { productId: row.productId, productName: row.productName, unit: row.unit, data: null, loading: true } });
    const res = await API.GET(apiEndpoints.boqTrackerCombinedDrill(row.productId), this.apiConfig());
    if (res.success) {
      this.setState(prev => ({ drillModal: { ...prev.drillModal, data: res.data || [], loading: false } }));
    } else {
      this.props.enqueueSnackbar('Failed to load drill-down', { variant: 'error' });
      this.setState(prev => ({ drillModal: { ...prev.drillModal, loading: false } }));
    }
  }

  closeDrill = () => this.setState({ drillModal: null });

  exportExcel = async () => {
    const { selectedCategory, search, gapFilter } = this.state;
    const params = new URLSearchParams();
    if (selectedCategory) params.append('category', selectedCategory);
    if (search) params.append('product', search);
    if (gapFilter) params.append('gapFilter', gapFilter);
    const url = apiEndpoints.boqTrackerExport + (params.toString() ? '?' + params.toString() : '');
    const r = await API.GETBlob(url, this.apiConfig());
    if (r.success) {
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(new Blob([r.data]));
      link.setAttribute('download', 'BOQ_Tracker.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } else {
      this.props.enqueueSnackbar('Export failed', { variant: 'error' });
    }
  };

  fmt(val) {
    if (val == null) return '—';
    return Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }

  renderPct(pct, boqQty) {
    if (boqQty == null || boqQty === 0) {
      return (
        <span style={{ fontSize: 10, color: '#7f8c8d', background: '#f2f3f4', border: '1px solid #d5d8dc', borderRadius: 6, padding: '1px 6px', whiteSpace: 'nowrap' }}>
          No BOQ
        </span>
      );
    }
    if (pct == null) return <span style={{ color: '#a0aec0' }}>—</span>;
    const clamped = Math.min(pct, 100);
    const barColor = pct > 100 ? '#c0392b' : pct >= 80 ? '#e67e22' : '#27ae60';
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'flex-end' }}>
        <div style={{ width: 60, height: 6, background: '#e8ecf0', borderRadius: 3, overflow: 'hidden', flexShrink: 0 }}>
          <div style={{ width: `${clamped}%`, height: '100%', background: barColor, borderRadius: 3 }} />
        </div>
        <span style={{ fontSize: 11, fontWeight: 700, color: barColor, whiteSpace: 'nowrap' }}>
          {pct.toFixed(1)}%
        </span>
      </div>
    );
  }

  render() {
    const { rows, allCategories, globalTotals, loading, search, selectedCategory, gapFilter, bucketFilter, consumptionFilter, isFiltered, drillModal } = this.state;
    const { tenantCode } = this.props;

    // If in global mode and no project selected yet, show a prompt
    if (tenantCode === undefined && this.props.requireTenant) {
      return (
        <div style={{ padding: 48, textAlign: 'center', color: '#a0aec0', fontSize: 14 }}>
          Select a project above to view BOQ Tracker data.
        </div>
      );
    }

    // Client-side status + % consumed filters applied on top of server-fetched rows
    let visibleRows = bucketFilter ? rows.filter(r => r.bucket === bucketFilter) : rows;
    if (consumptionFilter) visibleRows = visibleRows.filter(r => matchesConsumption(r, consumptionFilter));

    const tiles = globalTotals ? [
      { label: 'Total Products',      value: isFiltered || bucketFilter || consumptionFilter ? `${visibleRows.length} / ${globalTotals.totalProducts}` : globalTotals.totalProducts, color: '#2980b9', bg: '#ebf5fb', border: '#aed6f1' },
      { label: 'Outward Without BOQ', value: globalTotals.noBOQWithOutward, color: '#e67e22', bg: '#fef9e7', border: '#f9e79f',  tooltip: 'Products with outward consumption but no BOQ planned' },
      { label: '80–100% Consumed',    value: globalTotals.atRiskCount,      color: '#d35400', bg: '#fdf2e9', border: '#f5cba7',  tooltip: 'Products where outward is 80–100% of BOQ (nearing limit)' },
      { label: 'Exceeded BOQ',        value: globalTotals.exceededCount,    color: '#c0392b', bg: '#fdedec', border: '#f1948a',  tooltip: 'Products where outward exceeds planned BOQ quantity' },
      { label: 'No BOQ Set',          value: globalTotals.noBOQCount,       color: '#7f8c8d', bg: '#f2f3f4', border: '#d5d8dc', tooltip: 'Products with activity but no BOQ defined' },
    ] : [];

    const hasAnyFilter = !!(selectedCategory || search || gapFilter || bucketFilter || consumptionFilter);

    return (
      <div style={{ padding: '16px 16px', fontFamily: 'inherit' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
          <div>
            <h2 style={{ margin: 0, fontSize: 20, fontWeight: 700, color: '#1a202c' }}>BOQ End-to-End Tracker</h2>
            <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 13 }}>
              Track planned BOQ vs indented, inward received, and outward consumed per product.
              Click BOQ Planned or Outward Used to drill down by structure.
            </p>
          </div>
          <button
            onClick={this.exportExcel}
            style={{ padding: '8px 16px', borderRadius: 6, border: '1px solid #2980b9', background: '#ebf5fb', color: '#2980b9', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}
          >
            Export Excel
          </button>
        </div>

        {/* Summary tiles */}
        {!loading && globalTotals && (
          <div className="list-stat-cards" style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap', alignItems: 'center' }}>
            {tiles.map(tile => (
              <div key={tile.label} title={tile.tooltip || ''} style={{
                background: tile.bg, border: `1px solid ${tile.border}`,
                borderRadius: 10, padding: '12px 18px', minWidth: 140,
                cursor: tile.tooltip ? 'help' : 'default',
              }}>
                <div style={{ fontSize: 11, color: tile.color, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}>{tile.label}</div>
                <div style={{ fontSize: 22, fontWeight: 700, color: tile.color, marginTop: 4 }}>{tile.value}</div>
              </div>
            ))}
            {hasAnyFilter && (
              <div style={{ fontSize: 11, color: '#e67e22', fontStyle: 'italic', alignSelf: 'center' }}>
                Totals show full project. Table shows filtered results ({visibleRows.length} products).
              </div>
            )}
          </div>
        )}

        {/* Filters */}
        <div style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <input
            type="text"
            placeholder="Search product name or code..."
            value={search}
            onChange={e => this.setState({ search: e.target.value })}
            onKeyDown={e => e.key === 'Enter' && this.applyFilters()}
            style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13, width: 220, outline: 'none' }}
          />
          <select
            value={selectedCategory}
            onChange={e => this.setState({ selectedCategory: e.target.value })}
            style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13, background: '#fff', outline: 'none', cursor: 'pointer', minWidth: 170 }}
          >
            <option value="">All Categories</option>
            {allCategories.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
          <select
            value={gapFilter}
            onChange={e => this.setState({ gapFilter: e.target.value })}
            style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13, background: '#fff', outline: 'none', cursor: 'pointer', minWidth: 190 }}
          >
            {GAP_FILTERS.map(g => <option key={g.value} value={g.value}>{g.label}</option>)}
          </select>
          <select
            value={bucketFilter}
            onChange={e => this.setState({ bucketFilter: e.target.value })}
            style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13, background: '#fff', outline: 'none', cursor: 'pointer', minWidth: 170 }}
          >
            {BUCKET_FILTERS.map(b => <option key={b.value} value={b.value}>{b.label}</option>)}
          </select>
          <select
            value={consumptionFilter}
            onChange={e => this.setState({ consumptionFilter: e.target.value })}
            title="Filter by consumption % (Outward ÷ BOQ Planned)"
            style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13, background: '#fff', outline: 'none', cursor: 'pointer', minWidth: 170 }}
          >
            {CONSUMPTION_FILTERS.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}
          </select>
          <button
            onClick={this.applyFilters}
            style={{ padding: '8px 16px', borderRadius: 6, background: '#2980b9', color: '#fff', border: 'none', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}
          >
            Apply
          </button>
          {hasAnyFilter && (
            <button
              onClick={this.clearFilters}
              style={{ padding: '8px 14px', borderRadius: 6, background: '#f2f3f4', color: '#4a5568', border: '1px solid #d1d5db', fontSize: 13, cursor: 'pointer' }}
            >
              Clear
            </button>
          )}
        </div>

        {/* Table */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: 48 }}><CircularProgress size={32} /></div>
        ) : visibleRows.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48, color: '#a0aec0', fontSize: 14 }}>No data found.</div>
        ) : (
          <div className="x-scroll">
            <table style={{ minWidth: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ background: '#f7f8fa', borderBottom: '2px solid #e2e8f0' }}>
                  {[
                    { label: 'Category',        align: 'left' },
                    { label: 'Product',         align: 'left' },
                    { label: 'Code',            align: 'left' },
                    { label: 'Unit',            align: 'center' },
                    { label: 'BOQ Planned',     align: 'right', note: 'click to drill down' },
                    { label: 'Indented',        align: 'right' },
                    { label: 'Inward Received', align: 'right' },
                    { label: 'Outward Used',    align: 'right', note: 'click to drill down' },
                    { label: 'Balance',         align: 'right' },
                    { label: '% Consumed',      align: 'right', note: 'outward ÷ BOQ' },
                    { label: 'Status',          align: 'center' },
                  ].map(h => (
                    <th key={h.label} style={{ padding: '8px 6px', textAlign: h.align, fontSize: 11, fontWeight: 700, color: '#718096', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                      {h.label}
                      {h.note && <div style={{ fontSize: 9, color: '#2980b9', fontWeight: 400, textTransform: 'none', letterSpacing: 0 }}>{h.note}</div>}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {visibleRows.map((row, idx) => {
                  const bs = BUCKET_STYLE[row.bucket] || BUCKET_STYLE.no_boq;
                  const balanceColor = row.boqBalance == null ? '#a0aec0' : row.boqBalance < 0 ? '#c0392b' : '#27ae60';
                  return (
                    <tr key={idx} style={{ background: idx % 2 === 0 ? '#fff' : '#fafbfc', borderBottom: '1px solid #f0f2f5' }}>
                      <td style={{ padding: '8px 6px', color: '#718096', fontSize: 12 }}>{row.categoryName || '—'}</td>
                      <td style={{ padding: '8px 6px', fontWeight: 500, color: '#2d3748' }}>{row.productName || '—'}</td>
                      <td style={{ padding: '8px 6px', color: '#a0aec0', fontSize: 12 }}>{row.productCode || '—'}</td>
                      <td style={{ padding: '8px 6px', textAlign: 'center', color: '#718096' }}>{row.unit || '—'}</td>
                      <td style={{ padding: '8px 6px', textAlign: 'right' }}>
                        {row.boqPlanned != null ? (
                          <span onClick={() => this.openDrill(row)} style={{ color: '#2980b9', fontWeight: 600, cursor: 'pointer', borderBottom: '1px dashed #2980b9' }} title="Click to see structure breakdown">
                            {this.fmt(row.boqPlanned)}
                          </span>
                        ) : <span style={{ color: '#a0aec0' }}>—</span>}
                      </td>
                      <td style={{ padding: '8px 6px', textAlign: 'right', color: '#2d3748' }}>
                        {row.totalIndented != null ? this.fmt(row.totalIndented) : <span style={{ color: '#a0aec0' }}>—</span>}
                      </td>
                      <td style={{ padding: '8px 6px', textAlign: 'right', color: '#2d3748' }}>
                        {row.totalInward != null ? this.fmt(row.totalInward) : <span style={{ color: '#a0aec0' }}>—</span>}
                      </td>
                      <td style={{ padding: '8px 6px', textAlign: 'right' }}>
                        {row.totalOutward != null ? (
                          <span onClick={() => this.openDrill(row)} style={{ color: '#7d3c98', fontWeight: 600, cursor: 'pointer', borderBottom: '1px dashed #7d3c98' }} title="Click to see structure breakdown">
                            {this.fmt(row.totalOutward)}
                          </span>
                        ) : <span style={{ color: '#a0aec0' }}>—</span>}
                      </td>
                      <td style={{ padding: '8px 6px', textAlign: 'right', fontWeight: 600, color: balanceColor }}>
                        {row.boqBalance != null ? this.fmt(row.boqBalance) : <span style={{ color: '#a0aec0' }}>—</span>}
                      </td>
                      <td style={{ padding: '8px 6px', textAlign: 'right' }}>
                        {this.renderPct(row.consumedPct, row.boqPlanned)}
                      </td>
                      <td style={{ padding: '8px 6px', textAlign: 'center' }}>
                        <span style={{ fontSize: 11, fontWeight: 700, color: bs.color, background: bs.bg, padding: '2px 8px', borderRadius: 8, border: `1px solid ${bs.border}`, whiteSpace: 'nowrap' }}>
                          {bs.label}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Drill-down modal — combined BOQ vs Outward */}
        {drillModal && (
          <Modal open onClose={this.closeDrill}>
            <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', background: '#fff', borderRadius: 12, padding: 28, width: '95%', maxWidth: 820, maxHeight: '85vh', overflow: 'auto', outline: 'none', boxShadow: '0 8px 32px rgba(0,0,0,0.18)' }}>
              {/* Modal header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
                <div>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#1a202c' }}>BOQ vs Outward Breakdown</div>
                  <div style={{ fontSize: 13, color: '#718096', marginTop: 2 }}>{drillModal.productName} · {drillModal.unit}</div>
                </div>
                <button onClick={this.closeDrill} style={{ background: 'none', border: 'none', fontSize: 20, cursor: 'pointer', color: '#718096', lineHeight: 1 }}>×</button>
              </div>

              {/* Column headers */}
              {!drillModal.loading && drillModal.data && drillModal.data.length > 0 && (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 110px 110px 140px', gap: 8, padding: '6px 12px', background: '#f7f8fa', borderRadius: 6, marginBottom: 12, fontSize: 11, fontWeight: 700, color: '#718096', textTransform: 'uppercase', letterSpacing: 0.4 }}>
                  <span>Location</span>
                  <span style={{ textAlign: 'right', color: '#2980b9' }}>BOQ Planned</span>
                  <span style={{ textAlign: 'right', color: '#7d3c98' }}>Outward Used</span>
                  <span style={{ textAlign: 'right' }}>% Consumed</span>
                </div>
              )}

              {drillModal.loading ? (
                <div style={{ textAlign: 'center', padding: 32 }}><CircularProgress size={28} /></div>
              ) : !drillModal.data || drillModal.data.length === 0 ? (
                <div style={{ textAlign: 'center', color: '#a0aec0', padding: 32 }}>No data found.</div>
              ) : (
                drillModal.data.map((typeGroup, ti) => (
                  <div key={ti} style={{ marginBottom: 16 }}>
                    {/* Building Type row */}
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 110px 110px 140px', gap: 8, padding: '10px 12px', background: '#eef2fb', borderRadius: 8, marginBottom: 6, fontWeight: 700 }}>
                      <span style={{ color: '#2d3748', fontSize: 13 }}>{typeGroup.buildingTypeName}</span>
                      <span style={{ textAlign: 'right', color: typeGroup.boqTotal ? '#2980b9' : '#a0aec0', fontSize: 13 }}>
                        {typeGroup.boqTotal ? this.fmt(typeGroup.boqTotal) : <span style={{ fontSize: 11, fontWeight: 400 }}>No BOQ</span>}
                      </span>
                      <span style={{ textAlign: 'right', color: typeGroup.outwardTotal ? '#7d3c98' : '#a0aec0', fontSize: 13 }}>
                        {typeGroup.outwardTotal ? this.fmt(typeGroup.outwardTotal) : '—'}
                      </span>
                      <span style={{ textAlign: 'right' }}>{this.renderPct(typeGroup.pct, typeGroup.boqTotal)}</span>
                    </div>

                    {/* Location rows */}
                    {typeGroup.locations && typeGroup.locations.map((loc, li) => (
                      <div key={li} style={{ marginLeft: 16, marginBottom: 8 }}>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 110px 110px 140px', gap: 8, padding: '7px 12px', background: '#f9fafb', borderLeft: '3px solid #d1d5db', borderRadius: '0 6px 6px 0', marginBottom: 2, fontWeight: 600, color: '#4a5568', fontSize: 12 }}>
                          <span>{loc.locationName}</span>
                          <span style={{ textAlign: 'right', color: loc.boqTotal ? '#2980b9' : '#a0aec0' }}>
                            {loc.boqTotal ? this.fmt(loc.boqTotal) : <span style={{ fontSize: 10, fontWeight: 400 }}>No BOQ</span>}
                          </span>
                          <span style={{ textAlign: 'right', color: loc.outwardTotal ? '#7d3c98' : '#a0aec0' }}>
                            {loc.outwardTotal ? this.fmt(loc.outwardTotal) : '—'}
                          </span>
                          <span style={{ textAlign: 'right' }}>{this.renderPct(loc.pct, loc.boqTotal)}</span>
                        </div>

                        {/* Area rows */}
                        {loc.areas && loc.areas.map((area, ai) => (
                          <div key={ai} style={{ display: 'grid', gridTemplateColumns: '1fr 110px 110px 140px', gap: 8, padding: '4px 12px 4px 24px', fontSize: 12, color: '#718096', borderBottom: '1px solid #f4f5f7' }}>
                            <span>{area.areaName || '(No final location)'}</span>
                            <span style={{ textAlign: 'right', color: area.boqQty ? '#4a90d9' : '#a0aec0' }}>
                              {area.boqQty ? this.fmt(area.boqQty) : <span style={{ fontSize: 10 }}>No BOQ</span>}
                            </span>
                            <span style={{ textAlign: 'right', color: area.outwardQty ? '#9b59b6' : '#a0aec0' }}>
                              {area.outwardQty ? this.fmt(area.outwardQty) : '—'}
                            </span>
                            <span style={{ textAlign: 'right' }}>{this.renderPct(area.pct, area.boqQty)}</span>
                          </div>
                        ))}
                      </div>
                    ))}
                  </div>
                ))
              )}
            </div>
          </Modal>
        )}
      </div>
    );
  }
}

export default withSnackbar(BOQTracker);
