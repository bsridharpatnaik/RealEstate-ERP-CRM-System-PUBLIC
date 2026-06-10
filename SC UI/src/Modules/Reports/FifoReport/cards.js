import React, { Component } from 'react';
import Tooltip from '@material-ui/core/Tooltip';
import { getProjectColor } from '../projectColors';

// Batch detail columns shown inside each card
const BATCH_COLS = [
  { header: 'Product',       key: 'productName',       flex: '2 1 160px' },
  { header: 'Unit',          key: 'measurementUnit',   flex: '0 0 50px'  },
  { header: 'Lot #',         key: 'batchLotNumber',    flex: '1 1 90px'  },
  { header: 'Brand',         key: 'batchBrand',        flex: '1 1 90px'  },
  { header: 'Recv. Date',    key: 'batchReceivedDate', flex: '0 0 90px'  },
  { header: 'Expiry Date',   key: 'batchExpiryDate',   flex: '0 0 90px'  },
  { header: 'Qty',           key: 'qtyConsumed',       flex: '0 0 70px'  },
  { header: 'Override Reason', key: 'overrideComment', flex: '2 1 160px' },
];

function dash(v) {
  if (v == null || v === '') return <span style={{ color: '#ccc' }}>—</span>;
  return v;
}

function OutwardCard({ outwardId, outwardDate, project, warehouse, contractor, structure, finalLocation, performedBy, rows, tenantMap }) {
  const [expanded, setExpanded] = React.useState(true);
  const totalQty   = rows.reduce((s, r) => s + (r.qtyConsumed || 0), 0);
  const batchCount = rows.length;

  return (
    <div style={{
      border: '1px solid #e8d5f5',
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
          background: '#fdf5ff',
          padding: '12px 16px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        {/* Outward ID + override badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: '0 0 auto' }}>
          <span style={{ fontSize: 15, fontWeight: 700, color: '#1a202c' }}>
            Outward #{outwardId}
          </span>
          <span style={{
            padding: '3px 10px', borderRadius: 12,
            fontSize: 11, fontWeight: 700,
            background: '#f5eef8', color: '#8e44ad',
            border: '1px solid #d7bde2',
          }}>
            ⚡ FIFO Override
          </span>
        </div>

        {/* Meta: project, date, warehouse */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: '0 0 auto', flexWrap: 'wrap' }}>
          {project && (() => { const pc = getProjectColor(project); return (
            <span style={{
              background: pc.bg, color: pc.color, border: `1px solid ${pc.border}`,
              fontSize: 11, fontWeight: 600, padding: '2px 8px', borderRadius: 8,
            }}>
              {(tenantMap && tenantMap[project]) || project}
            </span>
          ); })()}
          {outwardDate && (
            <span style={{ fontSize: 12, color: '#718096' }}>📅 {outwardDate}</span>
          )}
          {warehouse && (
            <span style={{ fontSize: 12, color: '#718096' }}>🏭 {warehouse}</span>
          )}
          {contractor && (
            <span style={{ fontSize: 12, color: '#718096' }}>👷 {contractor}</span>
          )}
        </div>

        {/* Location breadcrumb */}
        {(structure || finalLocation) && (
          <div style={{ flex: '1 1 180px', fontSize: 12, color: '#a0aec0', minWidth: 0 }}>
            <Tooltip title={[structure, finalLocation].filter(Boolean).join(' → ')} placement="top">
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                📍 {[structure, finalLocation].filter(Boolean).join(' → ')}
              </span>
            </Tooltip>
          </div>
        )}

        {/* Summary pills */}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flex: '0 0 auto' }}>
          <span style={{
            fontSize: 11, fontWeight: 600, color: '#8e44ad',
            background: '#f5eef8', padding: '2px 8px', borderRadius: 8,
            border: '1px solid #d7bde2',
          }}>
            {batchCount} batch{batchCount !== 1 ? 'es' : ''}
          </span>
          <span style={{
            fontSize: 11, fontWeight: 600, color: '#2d3748',
            background: '#edf2f7', padding: '2px 8px', borderRadius: 8,
            border: '1px solid #cbd5e0',
          }}>
            {totalQty.toLocaleString('en-IN', { maximumFractionDigits: 2 })} total qty
          </span>
          {performedBy && (
            <span style={{ fontSize: 11, color: '#a0aec0' }}>by {performedBy}</span>
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

      {/* ── Batch Rows ── */}
      {expanded && (
        <div>
          {/* Column header */}
          <div style={{
            display: 'flex',
            padding: '6px 16px',
            background: '#f7f8fa',
            borderTop: '1px solid #e8ecf0',
            borderBottom: '1px solid #e8ecf0',
            gap: 8,
          }}>
            {BATCH_COLS.map((col) => (
              <div key={col.key} style={{
                flex: col.flex,
                fontSize: 11, fontWeight: 700, color: '#718096',
                textTransform: 'uppercase', letterSpacing: 0.5,
                overflow: 'hidden', minWidth: 0,
              }}>
                {col.header}
              </div>
            ))}
          </div>

          {/* Data rows */}
          {rows.map((row, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                padding: '8px 16px',
                gap: 8,
                background: idx % 2 === 0 ? '#ffffff' : '#fafbfc',
                borderBottom: idx < rows.length - 1 ? '1px solid #f0f2f5' : 'none',
                alignItems: 'center',
              }}
            >
              {BATCH_COLS.map((col) => {
                const val = row[col.key];
                const isStr = typeof val === 'string' && val.length > 0;
                return (
                  <Tooltip key={col.key} title={isStr ? val : ''} placement="top" disableHoverListener={!isStr}>
                    <div style={{
                      flex: col.flex,
                      fontSize: 13,
                      color: col.key === 'overrideComment' ? '#c0392b' : '#2d3748',
                      fontWeight: col.key === 'qtyConsumed' ? 600 : (col.key === 'overrideComment' ? 500 : 400),
                      overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                      minWidth: 0,
                    }}>
                      {col.key === 'qtyConsumed'
                        ? (val != null ? Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '—')
                        : dash(val)
                      }
                    </div>
                  </Tooltip>
                );
              })}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

class FifoReportCards extends Component {
  render() {
    const { rows, tenantMap } = this.props;
    if (!rows || rows.length === 0) {
      return (
        <div style={{ textAlign: 'center', padding: '48px 0', color: '#a0aec0', fontSize: 14 }}>
          No FIFO override records found.
        </div>
      );
    }

    // Group consecutive rows by outwardId (backend sorts by outwardId so same-outward rows are contiguous)
    const groups = [];
    let current = null;
    for (const row of rows) {
      if (!current || current.outwardId !== row.outwardId) {
        current = {
          outwardId:     row.outwardId,
          outwardDate:   row.outwardDate,
          project:       row.tenantSchema,
          warehouse:     row.warehouseName,
          contractor:    row.contractorName,
          structure:     row.usageLocationName,
          finalLocation: row.usageAreaName,
          performedBy:   row.performedBy,
          rows:          [],
        };
        groups.push(current);
      }
      current.rows.push(row);
    }

    return (
      <div>
        {groups.map((g) => (
          <OutwardCard key={g.outwardId} {...g} tenantMap={tenantMap} />
        ))}
      </div>
    );
  }
}

export default FifoReportCards;
