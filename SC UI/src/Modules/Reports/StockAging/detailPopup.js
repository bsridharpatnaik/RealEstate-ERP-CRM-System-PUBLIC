import React from 'react';
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
    { label: 'Min Aging', value: row.minAgingDays === 9999 ? 'No Inward' : `${row.minAgingDays} days` },
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
              <th style={{ ...thStyle, width: '28%', textAlign: 'left'  }}>Warehouse</th>
              <th style={{ ...thStyle, width: '16%', textAlign: 'right' }}>Qty in Hand</th>
              <th style={{ ...thStyle, width: '18%', textAlign: 'left'  }}>Last Inward Date</th>
              <th style={{ ...thStyle, width: '16%', textAlign: 'right' }}>Aging (Days)</th>
              <th style={{ ...thStyle, width: '12%', textAlign: 'center'}}>Bucket</th>
            </tr>
          </thead>
          <tbody>
            {data.map((d, i) => {
              const bStyle = BUCKET_COLORS[d.agingBucket] || {};
              return (
                <tr key={i} style={{ background: i % 2 === 0 ? '#fff' : '#fafafa' }}>
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
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
