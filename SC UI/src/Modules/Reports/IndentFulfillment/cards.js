import React, { Component } from 'react';
import Tooltip from '@material-ui/core/Tooltip';
import { getProjectColor } from '../projectColors';

const INDENT_STATUS_STYLE = {
  'CLOSED':         { color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  'PO COMPLETED':   { color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
  'INWARD PARTIAL': { color: '#f39c12', bg: '#fef9e7', border: '#fad7a0' },
  'PO PARTIAL':     { color: '#f39c12', bg: '#fef9e7', border: '#fad7a0' },
  'PO CREATED':     { color: '#2980b9', bg: '#ebf5fb', border: '#aed6f1' },
  'APPROVED':       { color: '#8e44ad', bg: '#f5eef8', border: '#d7bde2' },
  'NEW':            { color: '#7f8c8d', bg: '#f2f3f4', border: '#d5d8dc' },
  'CANCELLED':      { color: '#c0392b', bg: '#fdedec', border: '#f1948a' },
  'REJECTED':       { color: '#c0392b', bg: '#fdedec', border: '#f1948a' },
  'SHORT CLOSED':   { color: '#27ae60', bg: '#eafaf1', border: '#a9dfbf' },
};

const PO_STATUS_COLOR = {
  'COMPLETED':    '#27ae60',
  'OPEN':         '#2980b9',
  'SHORT_CLOSED': '#d35400',
  'CANCELLED':    '#c0392b',
};

const LINE_STATUS_STYLE = {
  'INWARD COMPLETE': { color: '#27ae60', icon: '✓' },
  'INWARD PARTIAL':  { color: '#f39c12', icon: '◑' },
  'SHORT CLOSED':    { color: '#d35400', icon: '⊘' },
  'PO CREATED':      { color: '#2980b9', icon: '📋' },
  'CANCELLED':       { color: '#c0392b', icon: '✕' },
  'NEW':             { color: '#7f8c8d', icon: '○' },
};

function StatusBadge({ status }) {
  const st = INDENT_STATUS_STYLE[status] || { color: '#555', bg: '#eee', border: '#ccc' };
  return (
    <span style={{
      padding: '3px 10px', borderRadius: 12,
      fontSize: 11, fontWeight: 700, letterSpacing: 0.3,
      background: st.bg, color: st.color,
      border: `1px solid ${st.border}`,
      whiteSpace: 'nowrap',
    }}>
      {status}
    </span>
  );
}

function ProgressBar({ pct, receivedTotal, requestedTotal, unit }) {
  const clamped = Math.min(pct || 0, 100);
  const color = clamped >= 100 ? '#27ae60' : clamped >= 60 ? '#f39c12' : clamped > 0 ? '#e67e22' : '#e74c3c';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
      <div style={{ flex: 1, height: 7, background: '#e8ecf0', borderRadius: 4, overflow: 'hidden' }}>
        <div style={{
          width: `${clamped}%`, height: '100%', background: color,
          borderRadius: 4, transition: 'width 0.4s ease',
        }} />
      </div>
      <span style={{ fontSize: 12, color, fontWeight: 700, whiteSpace: 'nowrap', minWidth: 36 }}>
        {clamped.toFixed(0)}%
      </span>
      <span style={{ fontSize: 11, color: '#888', whiteSpace: 'nowrap' }}>
        {(receivedTotal || 0).toLocaleString('en-IN', { maximumFractionDigits: 1 })}/
        {(requestedTotal || 0).toLocaleString('en-IN', { maximumFractionDigits: 1 })} {unit || ''}
      </span>
    </div>
  );
}

// Mini 3-step stage bar for a single product line
function StageBar({ requestedQty, poQty, receivedQty, unit }) {
  const fmt = (n) => (n || 0).toLocaleString('en-IN', { maximumFractionDigits: 1 });
  const hasPo = poQty != null && poQty > 0;
  const hasInward = receivedQty > 0;

  const stageStyle = (active, done) => ({
    flex: 1,
    padding: '4px 8px',
    borderRadius: 4,
    background: done ? '#eafaf1' : active ? '#ebf5fb' : '#f7f8fa',
    border: `1px solid ${done ? '#a9dfbf' : active ? '#aed6f1' : '#e0e4e8'}`,
    textAlign: 'center',
    fontSize: 11,
  });
  const labelStyle = (done, active) => ({
    display: 'block', fontWeight: 700,
    color: done ? '#27ae60' : active ? '#2980b9' : '#a0aec0',
    fontSize: 13,
  });
  const subStyle = { display: 'block', color: '#888', fontSize: 10, marginTop: 1 };
  const arrowStyle = { fontSize: 14, color: '#c8d0da', alignSelf: 'center', padding: '0 2px' };

  return (
    <div style={{ display: 'flex', alignItems: 'stretch', gap: 0, marginTop: 4 }}>
      {/* Stage 1: Requested */}
      <div style={stageStyle(true, false)}>
        <span style={labelStyle(false, true)}>{fmt(requestedQty)}</span>
        <span style={subStyle}>{unit} · Requested</span>
      </div>
      <span style={arrowStyle}>→</span>

      {/* Stage 2: PO */}
      <div style={stageStyle(hasPo, false)}>
        {hasPo ? (
          <span style={labelStyle(false, true)}>{fmt(poQty)}</span>
        ) : (
          <span style={{ ...labelStyle(false, false), fontSize: 16 }}>—</span>
        )}
        <span style={subStyle}>PO'd</span>
      </div>
      <span style={arrowStyle}>→</span>

      {/* Stage 3: Received */}
      <div style={stageStyle(hasInward, hasInward && receivedQty >= requestedQty)}>
        <span style={labelStyle(hasInward && receivedQty >= requestedQty, hasInward)}>
          {hasInward ? fmt(receivedQty) : '—'}
        </span>
        <span style={subStyle}>Received</span>
      </div>
    </div>
  );
}

function IndentCard({ indentId, project, indentDate, indentStatus, lines, tenantMap }) {
  const [expanded, setExpanded] = React.useState(true);

  // Exclude short-closed lines from pending — their remaining qty was intentionally waived
  const activeLines        = lines.filter(l => l.lineItemStatus !== 'SHORT CLOSED' && l.lineItemStatus !== 'CANCELLED');
  const totalRequested     = activeLines.reduce((s, l) => s + (l.requestedQty || 0), 0);
  const totalReceived      = lines.reduce((s, l) => s + (l.receivedQty  || 0), 0);   // all received counts
  const totalPending       = activeLines.reduce((s, l) => s + (l.pendingQty   || 0), 0);
  const pct = lines.reduce((s, l) => s + (l.requestedQty || 0), 0) > 0
    ? (totalReceived / lines.reduce((s, l) => s + (l.requestedQty || 0), 0)) * 100
    : 0;
  const unit = lines.length === 1 ? (lines[0].unit || '') : '';

  const st = INDENT_STATUS_STYLE[indentStatus] || {};

  // SHORT CLOSED = intentional close-out → treated as completed, not cancelled
  const isCancelled        = indentStatus === 'CANCELLED' || indentStatus === 'REJECTED';
  const isShortClosed      = indentStatus === 'SHORT CLOSED';
  // Fully received: only when goods are actually received
  const isFullyReceived    = !isShortClosed && !isCancelled
                             && (indentStatus === 'CLOSED'
                                 || (totalRequested > 0 && totalReceived >= totalRequested));
  // Fully PO'd: all qty has PO coverage but nothing (or not all) received yet
  const totalPOd           = lines.reduce((s, l) => s + (l.poQty || 0), 0);
  const totalAllRequested  = lines.reduce((s, l) => s + (l.requestedQty || 0), 0);
  const isFullyPOd         = !isFullyReceived && !isShortClosed && !isCancelled
                             && (indentStatus === 'PO COMPLETED'
                                 || (totalAllRequested > 0 && totalPOd >= totalAllRequested));
  const isPartial          = !isFullyReceived && !isFullyPOd && totalReceived > 0;
  const hasPendingInFlight = !isFullyReceived && !isFullyPOd && !isPartial && totalPending > 0;
  const isNotStarted       = !isFullyReceived && !isFullyPOd && !isPartial && !hasPendingInFlight && !isShortClosed && !isCancelled;

  return (
    <div style={{
      border: `1px solid ${st.border || '#dde3ea'}`,
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
          background: st.bg || '#f8f9fa',
          padding: '12px 16px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: '0 0 auto' }}>
          <span style={{ fontSize: 15, fontWeight: 700, color: '#1a202c', letterSpacing: 0.2 }}>
            {indentId}
          </span>
          <StatusBadge status={indentStatus} />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: '0 0 auto' }}>
          {(() => { const pc = getProjectColor(project); return (
            <span style={{
              background: pc.bg, color: pc.color, border: `1px solid ${pc.border}`,
              fontSize: 11, fontWeight: 600, padding: '2px 8px', borderRadius: 8,
            }}>
              {(tenantMap && tenantMap[project]) || project || '—'}
            </span>
          ); })()}
          <span style={{ fontSize: 12, color: '#718096' }}>📅 {indentDate || '—'}</span>
        </div>

        <div style={{ flex: 1, minWidth: 160 }}>
          <ProgressBar pct={pct} receivedTotal={totalReceived} requestedTotal={totalRequested} unit={unit} />
        </div>

        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flex: '0 0 auto' }}>
          {isCancelled ? (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#7f8c8d',
              background: '#f2f3f4', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #bdc3c7',
            }}>
              {'✕ ' + indentStatus.charAt(0) + indentStatus.slice(1).toLowerCase()}
            </span>
          ) : isShortClosed ? (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#d35400',
              background: '#fdf2e9', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #f0b27a',
            }}>
              ⊘ Short Closed
            </span>
          ) : isFullyReceived ? (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#27ae60',
              background: '#eafaf1', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #a9dfbf',
            }}>
              ✓ Fully Received
            </span>
          ) : isFullyPOd ? (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#1f618d',
              background: '#eaf2ff', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #85c1e9',
            }}>
              📦 Fully PO'd
            </span>
          ) : isPartial ? (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#e67e22',
              background: '#fdf2e9', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #f0b27a',
            }}>
              ◑ Partially Received
            </span>
          ) : hasPendingInFlight ? (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#2980b9',
              background: '#ebf5fb', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #aed6f1',
            }}>
              🔄 PO in Progress
            </span>
          ) : (
            <span style={{
              fontSize: 11, fontWeight: 600, color: '#95a5a6',
              background: '#f2f3f4', padding: '2px 8px', borderRadius: 8,
              border: '1px solid #d5d8dc',
            }}>
              ○ Not Started
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

      {/* ── Line Items ── */}
      {expanded && (
        <div style={{ padding: '0 0 4px 0' }}>
          {lines.map((line, idx) => {
            const lst = LINE_STATUS_STYLE[line.lineItemStatus] || {};
            const poColor = PO_STATUS_COLOR[line.poStatus] || '#888';

            return (
              <div
                key={idx}
                style={{
                  padding: '10px 16px',
                  background: idx % 2 === 0 ? '#ffffff' : '#fafbfc',
                  borderTop: '1px solid #f0f2f5',
                }}
              >
                {/* Row: product info + status */}
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, flexWrap: 'wrap' }}>
                  {/* Product name + code + need-by */}
                  <div style={{ flex: '2 1 200px', minWidth: 0 }}>
                    {line.categoryName && (
                      <div style={{ fontSize: 11, color: '#7f8c8d', marginBottom: 2 }}>
                        {line.categoryName}
                        <span style={{ margin: '0 4px', color: '#a0aec0' }}>→</span>
                      </div>
                    )}
                    <Tooltip title={line.productName || ''} placement="top">
                      <div style={{
                        fontSize: 13, fontWeight: 600, color: '#2d3748',
                        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                      }}>
                        {line.productName || '—'}
                        {line.unit && (
                          <span style={{ fontSize: 11, color: '#a0aec0', fontWeight: 400, marginLeft: 6 }}>
                            ({line.unit})
                          </span>
                        )}
                      </div>
                    </Tooltip>
                    {line.productCode && (
                      <div style={{ fontSize: 11, color: '#a0aec0', marginTop: 1 }}>{line.productCode}</div>
                    )}
                    {line.leadTimeDays != null && (
                      <div style={{ fontSize: 11, color: '#7f8c8d', marginTop: 2 }}>
                        ⏱ Lead time: {line.leadTimeDays}d
                      </div>
                    )}
                  </div>

                  {/* PO info chip */}
                  {line.poNumber ? (
                    <div style={{ flex: '0 0 auto', textAlign: 'right' }}>
                      <span style={{ fontSize: 11, color: '#2980b9', fontWeight: 600 }}>
                        📋 {line.poNumber}
                      </span>
                      {line.poStatus && (
                        <span style={{
                          marginLeft: 6, fontSize: 10, fontWeight: 700,
                          color: poColor, textTransform: 'uppercase',
                        }}>
                          [{line.poStatus}]
                        </span>
                      )}
                    </div>
                  ) : (
                    <div style={{ flex: '0 0 auto', fontSize: 11, color: '#ccc' }}>No PO yet</div>
                  )}

                  {/* Line status */}
                  <div style={{ flex: '0 0 auto', fontSize: 11, fontWeight: 700, color: lst.color || '#555' }}>
                    {lst.icon && <span style={{ marginRight: 3 }}>{lst.icon}</span>}
                    {line.lineItemStatus || '—'}
                  </div>
                </div>

                {/* 3-stage pipeline */}
                <StageBar
                  requestedQty={line.requestedQty}
                  poQty={line.poQty}
                  receivedQty={line.receivedQty}
                  unit={line.unit}
                />
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

class IndentFulfillmentCards extends Component {
  render() {
    const { rows, tenantMap } = this.props;
    if (!rows || rows.length === 0) {
      return (
        <div style={{ textAlign: 'center', padding: '48px 0', color: '#a0aec0', fontSize: 14 }}>
          No indent records found.
        </div>
      );
    }

    // Group consecutive rows by indentId (backend sorts by indentId for grouping)
    const groups = [];
    let current = null;
    for (const row of rows) {
      if (!current || current.indentId !== row.indentId) {
        current = {
          indentId:     row.indentId,
          project:      row.project,
          indentDate:   row.indentDate,
          indentStatus: row.indentStatus,
          lines: [],
        };
        groups.push(current);
      }
      current.lines.push(row);
    }

    return (
      <div>
        {groups.map((g) => (
          <IndentCard key={g.indentId} {...g} tenantMap={tenantMap} />
        ))}
      </div>
    );
  }
}

export default IndentFulfillmentCards;
