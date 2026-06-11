import React, { Component } from 'react';
import Tooltip from '@material-ui/core/Tooltip';
import { getProjectColor } from '../projectColors';

const PO_STATUS_STYLE = {
  COMPLETED:    { color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  OPEN:         { color: '#2980b9', bg: '#ebf5fb', border: '#aed6f1' },
  SHORT_CLOSED: { color: '#d35400', bg: '#fdf2e9', border: '#f0b27a' },
  CANCELLED:    { color: '#c0392b', bg: '#fdedec', border: '#f1948a' },
};

const RECON_STATUS = {
  COMPLETE:    { color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf', label: 'Complete' },
  PARTIAL:     { color: '#f39c12', bg: '#fef9e7', border: '#fad7a0', label: 'Partial'  },
  NOT_STARTED: { color: '#95a5a6', bg: '#f2f3f4', border: '#d5d8dc', label: 'Not Started' },
};

function PoStatusBadge({ status }) {
  const st = PO_STATUS_STYLE[status] || { color: '#555', bg: '#eee', border: '#ccc' };
  return (
    <span style={{
      padding: '3px 10px', borderRadius: 12,
      fontSize: 11, fontWeight: 700, letterSpacing: 0.3,
      background: st.bg, color: st.color,
      border: `1px solid ${st.border}`,
      whiteSpace: 'nowrap',
    }}>
      {status || '—'}
    </span>
  );
}

function ProgressBar({ pct, receivedTotal, orderedTotal }) {
  const clamped = Math.min(pct || 0, 100);
  const color = clamped >= 100 ? '#27ae60' : clamped >= 50 ? '#f39c12' : clamped > 0 ? '#e67e22' : '#e74c3c';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
      <div style={{ flex: 1, height: 7, background: '#e8ecf0', borderRadius: 4, overflow: 'hidden' }}>
        <div style={{ width: `${clamped}%`, height: '100%', background: color, borderRadius: 4, transition: 'width 0.4s' }} />
      </div>
      <span style={{ fontSize: 12, color, fontWeight: 700, whiteSpace: 'nowrap', minWidth: 36 }}>
        {clamped.toFixed(0)}%
      </span>
      <span style={{ fontSize: 11, color: '#888', whiteSpace: 'nowrap' }}>
        {(receivedTotal || 0).toLocaleString('en-IN', { maximumFractionDigits: 1 })}/
        {(orderedTotal  || 0).toLocaleString('en-IN', { maximumFractionDigits: 1 })}
      </span>
    </div>
  );
}

function PoCard({ purchaseOrderId, project, poDate, poStatus, supplier, lines, tenantMap }) {
  const [expanded, setExpanded] = React.useState(true);

  const totalOrdered   = lines.reduce((s, l) => s + (l.orderedQty  || 0), 0);
  const totalReceived  = lines.reduce((s, l) => s + (l.receivedQty || 0), 0);
  const totalBalance   = lines.reduce((s, l) => s + (l.balanceQty  || 0), 0);
  const pct = totalOrdered > 0 ? (totalReceived / totalOrdered) * 100 : 0;

  // Aggregate recon status for the card
  const allComplete  = lines.every(l => l.reconciliationStatus === 'COMPLETE');
  const anyPartial   = lines.some(l => l.reconciliationStatus === 'PARTIAL');
  const overallRecon = allComplete ? 'COMPLETE' : anyPartial ? 'PARTIAL' : 'NOT_STARTED';
  const rs = RECON_STATUS[overallRecon] || RECON_STATUS.NOT_STARTED;
  const st = PO_STATUS_STYLE[poStatus] || { color: '#555', bg: '#f8f9fa', border: '#dde3ea' };

  return (
    <div style={{
      border: `1px solid ${st.border}`,
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
          background: st.bg,
          padding: '12px 16px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        {/* PO ID + status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: '0 0 auto' }}>
          <span style={{ fontSize: 15, fontWeight: 700, color: '#1a202c', letterSpacing: 0.2 }}>
            {purchaseOrderId}
          </span>
          <PoStatusBadge status={poStatus} />
        </div>

        {/* Project + date + supplier */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: '0 0 auto', flexWrap: 'wrap' }}>
          {project && (() => { const pc = getProjectColor(project); return (
            <span style={{
              background: pc.bg, color: pc.color, border: `1px solid ${pc.border}`,
              fontSize: 11, fontWeight: 600, padding: '2px 8px', borderRadius: 8,
            }}>
              {(tenantMap && tenantMap[project]) || project}
            </span>
          ); })()}
          {poDate && <span style={{ fontSize: 12, color: '#718096' }}>📅 {poDate}</span>}
          {supplier && <span style={{ fontSize: 12, color: '#718096' }}>🏭 {supplier}</span>}
        </div>

        {/* Progress bar */}
        <div style={{ flex: 1, minWidth: 160 }}>
          <ProgressBar pct={pct} receivedTotal={totalReceived} orderedTotal={totalOrdered} />
        </div>

        {/* Summary badges */}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flex: '0 0 auto' }}>
          <span style={{
            fontSize: 11, fontWeight: 700,
            color: rs.color, background: rs.bg,
            padding: '2px 8px', borderRadius: 8,
            border: `1px solid ${rs.border}`,
          }}>
            {rs.label}
          </span>
          {totalBalance > 0 && (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#c0392b',
              background: '#fdedec', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #f1948a',
            }}>
              {totalBalance.toLocaleString('en-IN', { maximumFractionDigits: 1 })} balance
            </span>
          )}
          <span style={{ fontSize: 12, color: '#a0aec0', fontWeight: 600 }}>
            {lines.length} item{lines.length !== 1 ? 's' : ''}
          </span>
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

      {/* ── Product Lines ── */}
      {expanded && (
        <div>
          {/* Column headers */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '2fr 70px 110px 110px 110px 95px 110px',
            padding: '6px 16px',
            background: '#f7f8fa',
            borderTop: '1px solid #e8ecf0',
            borderBottom: '1px solid #e8ecf0',
          }}>
            {['Product', 'Unit', 'Ordered', 'Received', 'Balance', '% Received', 'Status'].map((h) => (
              <div key={h} style={{ fontSize: 11, fontWeight: 700, color: '#718096', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                {h}
              </div>
            ))}
          </div>

          {lines.map((line, idx) => {
            const rs2 = RECON_STATUS[line.reconciliationStatus] || RECON_STATUS.NOT_STARTED;
            const pctLine = line.orderedQty > 0 ? (line.receivedQty / line.orderedQty) * 100 : 0;
            const balColor = (line.balanceQty || 0) > 0 ? '#c0392b' : '#27ae60';

            return (
              <div
                key={idx}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '2fr 70px 110px 110px 110px 95px 110px',
                  padding: '9px 16px',
                  background: idx % 2 === 0 ? '#ffffff' : '#fafbfc',
                  borderBottom: idx < lines.length - 1 ? '1px solid #f0f2f5' : 'none',
                  alignItems: 'center',
                }}
              >
                {/* Product */}
                <div>
                  <Tooltip title={line.productName || ''} placement="top">
                    <div style={{
                      fontSize: 13, fontWeight: 500, color: '#2d3748',
                      overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                    }}>
                      {line.productName || '—'}
                    </div>
                  </Tooltip>
                  {line.productCode && (
                    <div style={{ fontSize: 11, color: '#a0aec0' }}>{line.productCode}</div>
                  )}
                  {line.leadTimeDays != null && (
                    <div style={{ fontSize: 11, marginTop: 2 }}>
                      {line.isOverdue ? (
                        <span style={{ color: '#e74c3c', fontWeight: 700 }}>
                          ⚠ {line.daysOverdue}d overdue (LT {line.leadTimeDays}d)
                        </span>
                      ) : (
                        <span style={{ color: '#7f8c8d' }}>⏱ Lead: {line.leadTimeDays}d</span>
                      )}
                    </div>
                  )}
                </div>

                {/* Unit */}
                <div style={{ fontSize: 12, color: '#718096' }}>{line.unit || '—'}</div>

                {/* Ordered */}
                <div style={{ fontSize: 13, color: '#2d3748', fontWeight: 500 }}>
                  {(line.orderedQty || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                </div>

                {/* Received */}
                <div>
                  <div style={{ fontSize: 13, color: pctLine >= 100 ? '#27ae60' : '#2d3748', fontWeight: pctLine >= 100 ? 700 : 400 }}>
                    {(line.receivedQty || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                  </div>
                  {pctLine > 0 && pctLine < 100 && (
                    <div style={{ marginTop: 3, height: 3, background: '#e8ecf0', borderRadius: 2 }}>
                      <div style={{ width: `${Math.min(pctLine, 100).toFixed(0)}%`, height: '100%', background: '#f39c12', borderRadius: 2 }} />
                    </div>
                  )}
                </div>

                {/* Balance */}
                <div style={{ fontSize: 13, fontWeight: 600, color: balColor }}>
                  {(line.balanceQty || 0) > 0
                    ? (line.balanceQty).toLocaleString('en-IN', { maximumFractionDigits: 2 })
                    : <span style={{ fontSize: 16 }}>✓</span>
                  }
                </div>

                {/* % Received */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <div style={{ flex: 1, height: 5, background: '#eee', borderRadius: 3, overflow: 'hidden' }}>
                    <div style={{
                      width: `${Math.min(pctLine, 100).toFixed(0)}%`, height: '100%',
                      background: pctLine >= 100 ? '#27ae60' : pctLine >= 50 ? '#f39c12' : '#e74c3c',
                      borderRadius: 3,
                    }} />
                  </div>
                  <span style={{ fontSize: 11, fontWeight: 600, color: '#555', minWidth: 28 }}>
                    {pctLine.toFixed(0)}%
                  </span>
                </div>

                {/* Recon Status */}
                <div>
                  <span style={{
                    fontSize: 11, fontWeight: 700,
                    color: rs2.color, background: rs2.bg,
                    padding: '2px 8px', borderRadius: 8,
                    border: `1px solid ${rs2.border}`,
                    whiteSpace: 'nowrap',
                  }}>
                    {rs2.label}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

class PoReconciliationCards extends Component {
  render() {
    const { rows, tenantMap } = this.props;
    if (!rows || rows.length === 0) {
      return (
        <div style={{ textAlign: 'center', padding: '48px 0', color: '#a0aec0', fontSize: 14 }}>
          No purchase order records found.
        </div>
      );
    }

    // Group consecutive rows by purchaseOrderId
    const groups = [];
    let current = null;
    for (const row of rows) {
      if (!current || current.purchaseOrderId !== row.purchaseOrderId) {
        current = {
          purchaseOrderId: row.purchaseOrderId,
          project:         row.project,
          poDate:          row.poDate,
          poStatus:        row.poStatus,
          supplier:        row.supplier,
          lines: [],
        };
        groups.push(current);
      }
      current.lines.push(row);
    }

    return (
      <div>
        {groups.map((g) => (
          <PoCard key={g.purchaseOrderId} {...g} tenantMap={tenantMap} />
        ))}
      </div>
    );
  }
}

export default PoReconciliationCards;
