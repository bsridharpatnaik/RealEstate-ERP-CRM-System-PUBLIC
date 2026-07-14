import React, { useState } from 'react';
import CircularProgress from '@material-ui/core/CircularProgress';

const BUCKET_COLORS = {
  '0-30':  { color: '#27ae60', bg: '#eafaf1' },
  '31-60': { color: '#f39c12', bg: '#fef9e7' },
  '61-90': { color: '#e67e22', bg: '#fdf2e9' },
  '90+':   { color: '#e74c3c', bg: '#fdedec' },
};

const cellStyle = {
  padding: '8px 12px',
  borderBottom: '1px solid #f0f0f0',
  fontSize: 13,
  color: '#333',
};

const thStyle = {
  ...cellStyle,
  background: '#f5f5f5',
  fontWeight: 600,
  fontSize: 12,
  color: '#555',
  borderBottom: '2px solid #ddd',
};

function BucketBadge({ bucket }) {
  if (!bucket) return <span style={{ color: '#ccc' }}>—</span>;
  const s = BUCKET_COLORS[bucket] || {};
  return (
    <span style={{
      background: s.bg,
      color: s.color,
      padding: '2px 8px',
      borderRadius: 10,
      fontWeight: 600,
      fontSize: 11,
      border: `1px solid ${s.color}`,
    }}>
      {bucket}
    </span>
  );
}

export default function DetailPopup({ row, data, loading }) {
  const [expandedWarehouse, setExpandedWarehouse] = useState(null);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 40 }}>
        <CircularProgress size={28} />
        <div style={{ marginTop: 8, color: '#888', fontSize: 13 }}>Loading warehouse breakdown…</div>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: 32, color: '#999', fontSize: 13 }}>
        No warehouse data available.
      </div>
    );
  }

  // Summary from parent row
  const summary = row ? [
    { label: 'Product', value: row.productName },
    { label: 'Code', value: row.productCode || '—' },
    { label: 'Unit', value: row.unit || '—' },
    { label: 'Category', value: row.category || '—' },
    { label: 'Total Qty', value: row.totalQtyInHand != null
        ? Number(row.totalQtyInHand).toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '—' },
    { label: 'Aging (Oldest Stock)', value: row.minAgingDays === 9999 ? 'No Inward' : `${row.minAgingDays} days` },
    { label: 'POG', value: row.pog != null
        ? `₹ ${Number(row.pog).toLocaleString('en-IN', { maximumFractionDigits: 2 })}` : '—' },
    { label: 'Last PO Rate', value: row.lastPoRate != null
        ? `₹ ${Number(row.lastPoRate).toLocaleString('en-IN', { maximumFractionDigits: 2 })}` : '—' },
    { label: 'Last PO Date', value: row.lastPoDate || '—' },
  ] : [];

  return (
    <div>
      {/* Summary chips */}
      {summary.length > 0 && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
          {summary.map((s) => (
            <div key={s.label} style={{
              background: '#f8f9fa',
              border: '1px solid #e9ecef',
              borderRadius: 6,
              padding: '4px 10px',
              fontSize: 12,
            }}>
              <span style={{ color: '#888', marginRight: 4 }}>{s.label}:</span>
              <span style={{ fontWeight: 600, color: '#333' }}>{s.value}</span>
            </div>
          ))}
        </div>
      )}

      {/* Per-warehouse table */}
      <div style={{ overflowX: 'auto', border: '1px solid #e8e8e8', borderRadius: 6 }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
          <thead>
            <tr>
              <th style={{ ...thStyle, width: '24%', textAlign: 'left'  }}>Warehouse</th>
              <th style={{ ...thStyle, width: '14%', textAlign: 'right' }}>Qty in Hand</th>
              <th style={{ ...thStyle, width: '18%', textAlign: 'left'  }}>Oldest Stock Date</th>
              <th style={{ ...thStyle, width: '14%', textAlign: 'right' }}>Aging (Days)</th>
              <th style={{ ...thStyle, width: '12%', textAlign: 'center'}}>Bucket</th>
              <th style={{ ...thStyle, width: '18%', textAlign: 'center'}}></th>
            </tr>
          </thead>
          <tbody>
            {data.map((d, i) => {
              const bStyle = BUCKET_COLORS[d.agingBucket] || {};
              const breakdown = d.ageBreakdown || [];
              const isExpanded = expandedWarehouse === d.warehouseId;
              return (
                <React.Fragment key={i}>
                  <tr style={{ background: i % 2 === 0 ? '#fff' : '#fafafa' }}>
                    <td style={{ ...cellStyle, textAlign: 'left' }}>{d.warehouseName || '—'}</td>
                    <td style={{ ...cellStyle, textAlign: 'right' }}>
                      {d.qtyInHand != null
                        ? Number(d.qtyInHand).toLocaleString('en-IN', { maximumFractionDigits: 2 })
                        : '—'}
                    </td>
                    <td style={{ ...cellStyle, textAlign: 'left' }}>
                      {d.lastInwardDate || <span style={{ color: '#ccc' }}>No Inward</span>}
                    </td>
                    <td style={{ ...cellStyle, textAlign: 'right', fontWeight: 600, color: bStyle.color || '#333' }}>
                      {d.agingDays === 9999 ? 'No Inward' : d.agingDays}
                    </td>
                    <td style={{ ...cellStyle, textAlign: 'center' }}>
                      <BucketBadge bucket={d.agingBucket} />
                    </td>
                    <td style={{ ...cellStyle, textAlign: 'center' }}>
                      {breakdown.length > 0 && (
                        <span
                          onClick={() => setExpandedWarehouse(isExpanded ? null : d.warehouseId)}
                          style={{ color: '#1976d2', cursor: 'pointer', fontSize: 12, fontWeight: 600 }}
                        >
                          {isExpanded ? 'Hide breakdown ▲' : 'Show breakdown ▼'}
                        </span>
                      )}
                    </td>
                  </tr>
                  {isExpanded && (
                    <tr>
                      <td colSpan={6} style={{ padding: '0 12px 10px 32px', background: i % 2 === 0 ? '#fff' : '#fafafa' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid #eee' }}>
                          <thead>
                            <tr>
                              <th style={{ ...thStyle, fontSize: 11, textAlign: 'right', width: '30%' }}>Quantity</th>
                              <th style={{ ...thStyle, fontSize: 11, textAlign: 'left', width: '35%' }}>Inward Date</th>
                              <th style={{ ...thStyle, fontSize: 11, textAlign: 'right', width: '35%' }}>Age (Days)</th>
                            </tr>
                          </thead>
                          <tbody>
                            {breakdown.map((c, j) => (
                              <tr key={j}>
                                <td style={{ ...cellStyle, fontSize: 12, textAlign: 'right' }}>
                                  {Number(c.quantity).toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                                </td>
                                <td style={{ ...cellStyle, fontSize: 12, textAlign: 'left' }}>{c.inwardDate || '—'}</td>
                                <td style={{ ...cellStyle, fontSize: 12, textAlign: 'right' }}>{c.ageDays}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
