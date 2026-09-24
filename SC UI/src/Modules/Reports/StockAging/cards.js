import React, { Component } from 'react';
import Tooltip from '@material-ui/core/Tooltip';
import { getProjectColor } from '../projectColors';

const BUCKET_STYLE = {
  '0-30':  { color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  '31-60': { color: '#f39c12', bg: '#fef9e7', border: '#fad7a0' },
  '61-90': { color: '#e67e22', bg: '#fdf2e9', border: '#f0b27a' },
  '90+':   { color: '#e74c3c', bg: '#fdedec', border: '#f1948a' },
};

// Worst bucket ordering for card header summary
const BUCKET_RANK = { '0-30': 1, '31-60': 2, '61-90': 3, '90+': 4 };

function BucketBadge({ bucket, large }) {
  const st = BUCKET_STYLE[bucket] || { color: '#888', bg: '#f2f3f4', border: '#ccc' };
  return (
    <span style={{
      padding: large ? '4px 12px' : '2px 8px',
      borderRadius: 12,
      fontSize: large ? 13 : 11,
      fontWeight: 700,
      background: st.bg, color: st.color,
      border: `1.5px solid ${st.border}`,
      whiteSpace: 'nowrap',
    }}>
      {bucket || '—'}
    </span>
  );
}

function fmt(val, decimals) {
  if (val == null) return '—';
  return Number(val).toLocaleString('en-IN', { maximumFractionDigits: decimals != null ? decimals : 2 });
}

function ProductCard({ productName, productCode, unit, category, rows, onRowClick, tenantMap }) {
  const [expanded, setExpanded] = React.useState(true);

  // Worst bucket across all projects for the card header indicator
  const worstBucket = rows.reduce((worst, r) => {
    const rank = BUCKET_RANK[r.agingBucket] || 0;
    return rank > (BUCKET_RANK[worst] || 0) ? r.agingBucket : worst;
  }, '');

  const totalQty     = rows.reduce((s, r) => s + (r.totalQtyInHand || 0), 0);
  const totalPog     = rows.reduce((s, r) => s + (r.pog || 0), 0);
  const projectCount = rows.length;
  const worstSt      = BUCKET_STYLE[worstBucket] || {};

  return (
    <div style={{
      border: `1px solid ${worstSt.border || '#e2e8f0'}`,
      borderRadius: 10,
      marginBottom: 12,
      overflow: 'hidden',
      boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
      background: '#fff',
    }}>
      {/* ── Card Header ── */}
      <div
        onClick={() => setExpanded(!expanded)}
        style={{
          background: worstSt.bg || '#f8f9fa',
          padding: '12px 16px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        {/* Product name + code */}
        <div style={{ flex: '1 1 200px', minWidth: 0 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: '#1a202c', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {productName || '—'}
          </div>
          <div style={{ fontSize: 11, color: '#a0aec0', marginTop: 1 }}>
            {[productCode, unit, category].filter(Boolean).join(' · ')}
          </div>
        </div>

        {/* Worst bucket badge */}
        {worstBucket && <BucketBadge bucket={worstBucket} large />}

        {/* Summary pills */}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flex: '0 0 auto', flexWrap: 'wrap' }}>
          <span style={{
            fontSize: 11, fontWeight: 600, color: '#4a5568',
            background: '#edf2f7', padding: '2px 8px', borderRadius: 8,
            border: '1px solid #cbd5e0',
          }}>
            {projectCount} project{projectCount !== 1 ? 's' : ''}
          </span>
          <span style={{
            fontSize: 11, fontWeight: 600, color: '#2d3748',
            background: '#edf2f7', padding: '2px 8px', borderRadius: 8,
            border: '1px solid #cbd5e0',
          }}>
            {fmt(totalQty)} total qty
          </span>
          {totalPog > 0 && (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#2d3748',
              background: '#edf2f7', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #cbd5e0',
            }}>
              ₹{fmt(totalPog, 0)} POG
            </span>
          )}
          <span style={{
            fontSize: 16, color: '#718096', userSelect: 'none',
            display: 'inline-block',
            transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)',
            transition: 'transform 0.2s',
          }}>
            ▾
          </span>
        </div>
      </div>

      {/* ── Project Rows ── */}
      {expanded && (
        <div className="report-card-lines">
          {/* Column headers */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '1.5fr 90px 95px 80px 80px 90px 100px',
            padding: '6px 16px',
            background: '#f7f8fa',
            borderTop: '1px solid #e8ecf0',
            borderBottom: '1px solid #e8ecf0',
            gap: 8,
          }}>
            {['Project', 'Qty in Hand', 'Oldest Stock', 'Aging', 'Bucket', 'POG (₹)', 'PO Rate (₹)'].map((h) => (
              <div key={h} style={{
                fontSize: 11, fontWeight: 700, color: '#718096',
                textTransform: 'uppercase', letterSpacing: 0.5,
              }}>
                {h}
              </div>
            ))}
          </div>

          {rows.map((row, idx) => {
            const bst = BUCKET_STYLE[row.agingBucket] || {};
            return (
              <div
                key={idx}
                onClick={() => onRowClick && onRowClick(row)}
                title="Click to view warehouse breakdown"
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1.5fr 90px 95px 80px 80px 90px 100px',
                  padding: '9px 16px',
                  gap: 8,
                  background: idx % 2 === 0 ? '#ffffff' : '#fafbfc',
                  borderBottom: idx < rows.length - 1 ? '1px solid #f0f2f5' : 'none',
                  alignItems: 'center',
                  cursor: 'pointer',
                  transition: 'background 0.1s',
                }}
                onMouseEnter={e => e.currentTarget.style.background = '#f0f4ff'}
                onMouseLeave={e => e.currentTarget.style.background = idx % 2 === 0 ? '#ffffff' : '#fafbfc'}
              >
                {/* Project */}
                <div style={{ fontSize: 13, fontWeight: 500, color: '#2d3748' }}>
                  {(() => { const pc = getProjectColor(row.tenantSchema); return (
                    <span style={{
                      background: pc.bg, color: pc.color, border: `1px solid ${pc.border}`,
                      fontSize: 12, fontWeight: 600, padding: '2px 8px', borderRadius: 8,
                    }}>
                      {(tenantMap && tenantMap[row.tenantSchema]) || row.tenantSchema || '—'}
                    </span>
                  ); })()}
                </div>

                {/* Qty in Hand */}
                <div style={{ fontSize: 13, fontWeight: 600, color: '#2d3748' }}>
                  {fmt(row.totalQtyInHand)}
                </div>

                {/* Last Inward */}
                <div style={{ fontSize: 12, color: '#718096' }}>
                  {row.lastInwardDate || '—'}
                </div>

                {/* Aging days */}
                <div style={{ fontSize: 13, fontWeight: 700, color: bst.color || '#555' }}>
                  {row.minAgingDays === 9999 ? 'No Inward' : (row.minAgingDays != null ? `${row.minAgingDays}d` : '—')}
                </div>

                {/* Bucket badge */}
                <div>
                  {row.agingBucket ? <BucketBadge bucket={row.agingBucket} /> : '—'}
                </div>

                {/* POG */}
                <div style={{ fontSize: 13, color: '#2d3748' }}>
                  {row.pog != null ? `₹${fmt(row.pog, 0)}` : '—'}
                </div>

                {/* PO Rate */}
                <Tooltip title={row.lastPoDate ? `Last PO: ${row.lastPoDate}` : ''} placement="top">
                  <div style={{ fontSize: 13, color: '#2d3748' }}>
                    {row.lastPoRate != null ? `₹${fmt(row.lastPoRate)}` : '—'}
                  </div>
                </Tooltip>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

class StockAgingCards extends Component {
  render() {
    const { rows, onRowClick, tenantMap } = this.props;

    if (!rows || rows.length === 0) {
      return (
        <div style={{ textAlign: 'center', padding: '48px 0', color: '#a0aec0', fontSize: 14 }}>
          No stock aging records found.
        </div>
      );
    }

    // Group consecutive rows by productId (backend sorts by productName, tenantSchema)
    const groups = [];
    let current = null;
    for (const row of rows) {
      if (!current || current.productId !== row.productId) {
        current = {
          productId:   row.productId,
          productName: row.productName,
          productCode: row.productCode,
          unit:        row.unit,
          category:    row.category,
          rows:        [],
        };
        groups.push(current);
      }
      current.rows.push(row);
    }

    return (
      <div>
        {groups.map((g) => (
          <ProductCard key={g.productId} {...g} onRowClick={onRowClick} tenantMap={tenantMap} />
        ))}
      </div>
    );
  }
}

export default StockAgingCards;
