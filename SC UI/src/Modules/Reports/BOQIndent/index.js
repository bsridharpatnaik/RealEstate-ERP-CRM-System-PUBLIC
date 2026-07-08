import React, { Component } from 'react';
import { API } from '../../../axios';
import { apiEndpoints } from '../../../endpoints';
import { withSnackbar } from 'notistack';
import CircularProgress from '@material-ui/core/CircularProgress';
import Tooltip from '@material-ui/core/Tooltip';

const BUCKET_STYLE = {
  over:  { color: '#c0392b', bg: '#fdedec', border: '#f1948a', label: 'Over-indented' },
  under: { color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf', label: 'Within BOQ'    },
  none:  { color: '#7f8c8d', bg: '#f2f3f4', border: '#d5d8dc', label: 'No BOQ'        },
};

class BOQIndentReport extends Component {
  state = {
    rows: [],
    loading: true,
    search: '',
    hideNoBOQ: false,
    selectedCategory: '',
    expanded: {},   // { [rowKey]: true }
  };

  componentDidMount() {
    this.fetchData();
  }

  rowKey(row) {
    return `${row.productCode || ''}|${row.productName || ''}`;
  }

  toggleRow(key) {
    this.setState(s => {
      const expanded = { ...s.expanded };
      if (expanded[key]) delete expanded[key];
      else expanded[key] = true;
      return { expanded };
    });
  }

  toggleAll(visibleRows) {
    const allOpen = visibleRows.length > 0 && visibleRows.every(r => this.state.expanded[this.rowKey(r)]);
    if (allOpen) {
      this.setState({ expanded: {} });
    } else {
      const expanded = {};
      visibleRows.forEach(r => { expanded[this.rowKey(r)] = true; });
      this.setState({ expanded });
    }
  }

  async fetchData() {
    this.setState({ loading: true });
    const res = await API.GET(apiEndpoints.boqIndentSummary);
    if (res.success) {
      this.setState({ rows: res.data || [], loading: false });
    } else {
      this.props.enqueueSnackbar(res.errorMessage || 'Failed to load BOQ vs Indent data', { variant: 'error' });
      this.setState({ loading: false });
    }
  }

  fmt(val) {
    if (val == null) return '—';
    return Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 });
  }

  render() {
    const { rows, loading, search, hideNoBOQ, selectedCategory, expanded } = this.state;

    const categoryOptions = [...new Set(rows.map(r => r.categoryName).filter(Boolean))].sort();

    const filtered = rows.filter(r => {
      if (hideNoBOQ && r.bucket === 'none') return false;
      if (selectedCategory && r.categoryName !== selectedCategory) return false;
      return !search || (r.productName || '').toLowerCase().includes(search.toLowerCase());
    });

    const totalBOQ      = rows.reduce((s, r) => s + (r.boqPlanned || 0), 0);
    const totalIndented = rows.reduce((s, r) => s + (r.totalIndented || 0), 0);
    const overCount     = rows.filter(r => r.bucket === 'over').length;
    const noBOQCount    = rows.filter(r => r.bucket === 'none').length;

    return (
      <div style={{ padding: '24px 28px', fontFamily: 'inherit' }}>
        {/* Header */}
        <div style={{ marginBottom: 20 }}>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 700, color: '#1a202c' }}>
            BOQ vs Indent Report
          </h2>
          <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 13 }}>
            Compares BOQ planned quantities against total indents raised for this project.
          </p>
        </div>

        {/* Summary tiles */}
        {!loading && (
          <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
            {[
              { label: 'Total Products', value: rows.length, color: '#2980b9', bg: '#ebf5fb', border: '#aed6f1' },
              { label: 'Total BOQ Planned', value: this.fmt(totalBOQ), color: '#1a7a40', bg: '#eafaf1', border: '#a9dfbf' },
              { label: 'Total Indented', value: this.fmt(totalIndented), color: '#7d3c98', bg: '#f5eef8', border: '#d2b4de' },
              { label: 'Over-indented', value: overCount, color: '#c0392b', bg: '#fdedec', border: '#f1948a' },
              { label: 'No BOQ Set', value: noBOQCount, color: '#7f8c8d', bg: '#f2f3f4', border: '#d5d8dc' },
            ].map(tile => (
              <div key={tile.label} style={{
                background: tile.bg, border: `1px solid ${tile.border}`,
                borderRadius: 10, padding: '12px 18px', minWidth: 130,
              }}>
                <div style={{ fontSize: 11, color: tile.color, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                  {tile.label}
                </div>
                <div style={{ fontSize: 20, fontWeight: 700, color: tile.color, marginTop: 4 }}>
                  {tile.value}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Search + filter row */}
        <div style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <input
            type="text"
            placeholder="Search product..."
            value={search}
            onChange={e => this.setState({ search: e.target.value })}
            style={{
              padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db',
              fontSize: 13, width: 240, outline: 'none',
            }}
          />
          <select
            value={selectedCategory}
            onChange={e => this.setState({ selectedCategory: e.target.value })}
            style={{
              padding: '8px 12px', borderRadius: 6, border: '1px solid #d1d5db',
              fontSize: 13, color: selectedCategory ? '#2d3748' : '#a0aec0',
              background: '#fff', outline: 'none', cursor: 'pointer', minWidth: 180,
            }}
          >
            <option value="">All Categories</option>
            {categoryOptions.map(c => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: '#4a5568', cursor: 'pointer', userSelect: 'none' }}>
            <input
              type="checkbox"
              checked={hideNoBOQ}
              onChange={e => this.setState({ hideNoBOQ: e.target.checked })}
              style={{ cursor: 'pointer' }}
            />
            Hide products with no BOQ
          </label>
          {!loading && filtered.length > 0 && (
            <button
              onClick={() => this.toggleAll(filtered)}
              style={{
                marginLeft: 'auto', padding: '8px 14px', borderRadius: 6,
                border: '1px solid #cbd5e0', background: '#fff', color: '#4a5568',
                fontSize: 13, fontWeight: 600, cursor: 'pointer', outline: 'none',
              }}
            >
              {filtered.every(r => expanded[this.rowKey(r)]) ? '▾ Collapse all' : '▸ Expand all'}
            </button>
          )}
        </div>

        {/* Table */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <CircularProgress size={32} />
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48, color: '#a0aec0', fontSize: 14 }}>
            No data found.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ background: '#f7f8fa', borderBottom: '2px solid #e2e8f0' }}>
                  <th style={{ width: 34 }} />
                  {['Category', 'Product', 'Code', 'Unit', 'BOQ Planned', 'Total Indented', 'Balance', 'Coverage %', 'Status'].map(h => (
                    <th key={h} style={{
                      padding: '10px 12px',
                      textAlign: ['Category', 'Product', 'Code'].includes(h) ? 'left' : 'right',
                      fontSize: 11, fontWeight: 700, color: '#718096',
                      textTransform: 'uppercase', letterSpacing: 0.5,
                      ...(h === 'Status' ? { textAlign: 'center' } : {}),
                    }}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filtered.map((row, idx) => {
                  const bs = BUCKET_STYLE[row.bucket] || BUCKET_STYLE.none;
                  const balanceColor = row.bucket === 'over' ? '#c0392b' : row.bucket === 'under' ? '#27ae60' : '#7f8c8d';
                  const pct = row.coveragePct != null ? row.coveragePct : null;
                  const pctClamped = pct != null ? Math.min(pct, 200) : 0;
                  const key = this.rowKey(row);
                  const isOpen = !!expanded[key];

                  return (
                    <React.Fragment key={idx}>
                    <tr
                      onClick={() => this.toggleRow(key)}
                      style={{
                        background: isOpen ? '#f0f6ff' : (idx % 2 === 0 ? '#fff' : '#fafbfc'),
                        borderBottom: '1px solid #f0f2f5', cursor: 'pointer',
                      }}
                    >
                      {/* Expand chevron */}
                      <td style={{ padding: '10px 8px', textAlign: 'center', color: '#a0aec0', fontSize: 11, userSelect: 'none' }}>
                        {isOpen ? '▾' : '▸'}
                      </td>
                      {/* Category */}
                      <td style={{ padding: '10px 12px', color: '#718096', fontSize: 12 }}>
                        {row.categoryName || '—'}
                      </td>
                      {/* Product */}
                      <td style={{ padding: '10px 12px', fontWeight: 500, color: '#2d3748' }}>
                        {row.productName || '—'}
                      </td>
                      {/* Code */}
                      <td style={{ padding: '10px 12px', color: '#a0aec0', fontSize: 12 }}>
                        {row.productCode || '—'}
                      </td>
                      {/* Unit */}
                      <td style={{ padding: '10px 12px', textAlign: 'right', color: '#718096' }}>
                        {row.unit || '—'}
                      </td>
                      {/* BOQ Planned */}
                      <td style={{ padding: '10px 12px', textAlign: 'right', color: '#2d3748', fontWeight: 500 }}>
                        {row.boqPlanned > 0 ? this.fmt(row.boqPlanned) : <span style={{ color: '#a0aec0' }}>—</span>}
                      </td>
                      {/* Total Indented */}
                      <td style={{ padding: '10px 12px', textAlign: 'right', color: '#2d3748' }}>
                        {this.fmt(row.totalIndented)}
                      </td>
                      {/* Balance */}
                      <td style={{ padding: '10px 12px', textAlign: 'right', fontWeight: 600, color: balanceColor }}>
                        {row.boqPlanned > 0 ? this.fmt(row.balance) : <span style={{ color: '#a0aec0' }}>—</span>}
                      </td>
                      {/* Coverage % */}
                      <td style={{ padding: '10px 12px', textAlign: 'right' }}>
                        {pct != null ? (
                          <Tooltip title={`${pct.toFixed(1)}% of BOQ indented`} placement="top">
                            <div style={{ display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'flex-end' }}>
                              <div style={{ width: 60, height: 5, background: '#e8ecf0', borderRadius: 3, overflow: 'hidden' }}>
                                <div style={{
                                  width: `${Math.min(pctClamped / 2, 100)}%`, height: '100%',
                                  background: pct > 100 ? '#c0392b' : pct >= 80 ? '#f39c12' : '#27ae60',
                                  borderRadius: 3,
                                }} />
                              </div>
                              <span style={{
                                fontSize: 12, fontWeight: 700,
                                color: pct > 100 ? '#c0392b' : pct >= 80 ? '#e67e22' : '#27ae60',
                              }}>
                                {pct.toFixed(1)}%
                              </span>
                            </div>
                          </Tooltip>
                        ) : <span style={{ color: '#a0aec0' }}>—</span>}
                      </td>
                      {/* Status */}
                      <td style={{ padding: '10px 12px', textAlign: 'center' }}>
                        <span style={{
                          fontSize: 11, fontWeight: 700,
                          color: bs.color, background: bs.bg,
                          padding: '2px 8px', borderRadius: 8,
                          border: `1px solid ${bs.border}`,
                          whiteSpace: 'nowrap',
                        }}>
                          {bs.label}
                        </span>
                      </td>
                    </tr>
                    {isOpen && (
                      <tr style={{ background: '#f7fbff', borderBottom: '1px solid #e2e8f0' }}>
                        <td colSpan={10} style={{ padding: '4px 12px 14px 46px' }}>
                          <div style={{ fontSize: 11, color: '#718096', fontWeight: 700, textTransform: 'uppercase', letterSpacing: 0.5, margin: '6px 0 8px' }}>
                            Indent status breakup {row.unit ? `(${row.unit})` : ''}
                          </div>
                          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                            {[
                              { label: 'Requested', value: row.totalIndented, color: '#2d3748', bg: '#edf2f7', border: '#cbd5e0' },
                              { label: 'Received', value: row.totalReceived, color: '#1a7a40', bg: '#eafaf1', border: '#a9dfbf' },
                              { label: 'Pending', value: row.totalPending, color: '#2980b9', bg: '#ebf5fb', border: '#aed6f1' },
                              { label: 'Short Closed', value: row.totalShortClosed, color: '#b9770e', bg: '#fef5e7', border: '#f5cba7' },
                            ].map(c => (
                              <div key={c.label} style={{
                                background: c.bg, border: `1px solid ${c.border}`,
                                borderRadius: 8, padding: '8px 16px', minWidth: 120,
                              }}>
                                <div style={{ fontSize: 10, color: c.color, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.4 }}>
                                  {c.label}
                                </div>
                                <div style={{ fontSize: 16, fontWeight: 700, color: c.color, marginTop: 3 }}>
                                  {this.fmt(c.value)}
                                </div>
                              </div>
                            ))}
                          </div>
                          <div style={{ fontSize: 11, color: '#a0aec0', marginTop: 8 }}>
                            Requested = Received + Pending + Short Closed. Short Closed = ordered qty that was closed out and will not arrive.
                          </div>
                        </td>
                      </tr>
                    )}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }
}

export default withSnackbar(BOQIndentReport);
