import React from 'react';
import CommonTable from '../../../Shared/Table';
import { messages } from '../../../messages';
import Tooltip from '@material-ui/core/Tooltip';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';

const STATUS_STYLE = {
  COMPLETE:    { color: '#27ae60', bg: '#eafaf1', label: 'Complete' },
  PARTIAL:     { color: '#f39c12', bg: '#fef9e7', label: 'Partial' },
  NOT_STARTED: { color: '#95a5a6', bg: '#f2f3f4', label: 'Not Started' },
};

const PO_STATUS_STYLE = {
  COMPLETED:    { color: '#27ae60' },
  OPEN:         { color: '#3498db' },
  SHORT_CLOSED: { color: '#e67e22' },
  CANCELLED:    { color: '#e74c3c' },
};

const COLUMNS = [
  { header: 'Project',     key: 'project',          width: 100, align: 'left'  },
  { header: 'PO Number',   key: 'purchaseOrderId',  width: 90,  align: 'left'  },
  { header: 'PO Date',     key: 'poDate',           width: 90,  align: 'left'  },
  { header: 'PO Status',   key: 'poStatus',         width: 90,  align: 'center'},
  { header: 'Supplier',    key: 'supplier',         width: 120, align: 'left'  },
  { header: 'Product',     key: 'productName',      width: 150, align: 'left'  },
  { header: 'Code',        key: 'productCode',      width: 70,  align: 'left'  },
  { header: 'Unit',        key: 'unit',             width: 45,  align: 'center'},
  { header: 'Ordered',     key: 'orderedQty',       width: 80,  align: 'right' },
  { header: 'Received',    key: 'receivedQty',      width: 80,  align: 'right' },
  { header: 'Balance',     key: 'balanceQty',       width: 80,  align: 'right' },
  { header: '% Received',  key: 'percentReceived',  width: 95,  align: 'center'},
  { header: 'Status',      key: 'reconciliationStatus', width: 100, align: 'center'},
];

const TOTAL_WIDTH = COLUMNS.reduce((sum, c) => sum + c.width, 0);

const cellBase = {
  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
  padding: '7px 8px', borderBottom: '1px solid #f0f0f0', fontSize: 13,
};
const thBase = {
  ...cellBase, background: '#f5f5f5', fontWeight: 600, fontSize: 12,
  borderBottom: '2px solid #ddd', color: '#333',
  position: 'sticky', top: 0, zIndex: 1,
};

function pctBar(pct) {
  const clamped = Math.min(pct || 0, 100);
  const color = clamped >= 100 ? '#27ae60' : clamped >= 50 ? '#f39c12' : '#e74c3c';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{
        flex: 1, height: 6, background: '#eee', borderRadius: 3, overflow: 'hidden',
      }}>
        <div style={{ width: `${clamped}%`, height: '100%', background: color, borderRadius: 3, transition: 'width 0.3s' }} />
      </div>
      <span style={{ fontSize: 11, color, fontWeight: 600, minWidth: 32, textAlign: 'right' }}>
        {clamped.toFixed(0)}%
      </span>
    </div>
  );
}

class PoReconTable extends CommonTable {

  componentDidMount() { this.setState({ rows: this.props.rows || [] }); }
  componentDidUpdate(prevProps) {
    if (prevProps.rows !== this.props.rows) this.setState({ rows: this.props.rows || [] });
  }

  sortCol(key) {
    const { sortkey, sortby } = this.props;
    const newSortBy = sortkey === key
      ? (sortby === 'asc' ? 'desc' : sortby === 'desc' ? null : 'asc')
      : 'asc';
    this.props.search && this.props.search(newSortBy ? key : null, newSortBy);
  }

  renderSortIcon(key) {
    const { sortkey, sortby } = this.props;
    if (sortkey !== key) return null;
    return sortby === 'desc'
      ? <ArrowDropDownIcon style={{ fontSize: 16, verticalAlign: 'middle' }} />
      : <ArrowDropUpIcon   style={{ fontSize: 16, verticalAlign: 'middle' }} />;
  }

  renderHeader() {
    return (
      <tr>
        {COLUMNS.map((col) => (
          <th key={col.key} onClick={() => this.sortCol(col.key)}
            style={{ ...thBase, width: col.width, textAlign: col.align, cursor: 'pointer', userSelect: 'none' }}>
            {col.header}{this.renderSortIcon(col.key)}
          </th>
        ))}
      </tr>
    );
  }

  renderCell(col, row) {
    const val = row[col.key];

    if (col.key === 'reconciliationStatus') {
      const st = STATUS_STYLE[val] || {};
      return val ? (
        <span style={{
          background: st.bg, color: st.color, padding: '2px 8px',
          borderRadius: 10, fontWeight: 600, fontSize: 11,
          border: `1px solid ${st.color}`, whiteSpace: 'nowrap',
        }}>{st.label || val}</span>
      ) : null;
    }

    if (col.key === 'poStatus') {
      const st = PO_STATUS_STYLE[val] || {};
      return <span style={{ color: st.color || '#555', fontWeight: 600, fontSize: 12 }}>{val || '—'}</span>;
    }

    if (col.key === 'percentReceived') {
      return pctBar(val);
    }

    if (col.key === 'balanceQty') {
      const num = val == null ? 0 : Number(val);
      const color = num > 0 ? '#e74c3c' : num < 0 ? '#e67e22' : '#27ae60';
      return <span style={{ color, fontWeight: 600 }}>{num.toLocaleString('en-IN', { maximumFractionDigits: 2 })}</span>;
    }

    if (col.key === 'orderedQty' || col.key === 'receivedQty') {
      return val != null ? Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '—';
    }

    if (val == null || val === '') return <span style={{ color: '#ccc' }}>—</span>;
    return String(val);
  }

  renderBody() {
    const rows = this.state.rows || [];
    if (!rows.length) {
      return (
        <tr>
          <td colSpan={COLUMNS.length} style={{ textAlign: 'center', padding: 32, color: '#999', fontSize: 13 }}>
            {messages.common.noRecords}
          </td>
        </tr>
      );
    }

    // Assign a group index per PO so we can alternate shading
    let poGroupIndex = -1;
    let lastPoId = null;
    const groupIndices = rows.map((row) => {
      if (row.purchaseOrderId !== lastPoId) { poGroupIndex++; lastPoId = row.purchaseOrderId; }
      return poGroupIndex;
    });

    // PO-level fields — shown only on first row of each PO group, rest dimmed
    const PO_LEVEL_COLS = new Set(['project', 'purchaseOrderId', 'poDate', 'poStatus', 'supplier']);

    return rows.map((row, i) => {
      const grpIdx = groupIndices[i];
      const isFirstInGroup = i === 0 || row.purchaseOrderId !== rows[i - 1].purchaseOrderId;
      const bg = grpIdx % 2 === 0 ? '#ffffff' : '#f5f7fa';
      const groupBorderTop = isFirstInGroup && i > 0 ? '2px solid #d0d7de' : undefined;

      return (
        <tr key={i} style={{ background: bg }}>
          {COLUMNS.map((col) => {
            // Dim repeated PO-level cells (show only on first row of group)
            const isDimmed = PO_LEVEL_COLS.has(col.key) && !isFirstInGroup;
            const val = row[col.key];
            const tip = !isDimmed && typeof val === 'string' && val.length > 0 ? val : '';
            return (
              <Tooltip key={col.key} title={tip} placement="top" disableHoverListener={!tip}>
                <td style={{
                  ...cellBase,
                  width: col.width,
                  textAlign: col.align,
                  borderTop: groupBorderTop,
                  opacity: isDimmed ? 0 : 1,
                  color: isDimmed ? 'transparent' : undefined,
                }}>
                  {isDimmed ? null : this.renderCell(col, row)}
                </td>
              </Tooltip>
            );
          })}
        </tr>
      );
    });
  }

  render() {
    return (
      <div style={{ overflowX: 'auto', width: '100%', border: '1px solid #e8e8e8', borderRadius: 6 }}>
        <table style={{ tableLayout: 'fixed', width: TOTAL_WIDTH, minWidth: '100%', borderCollapse: 'collapse' }}>
          <colgroup>{COLUMNS.map((c) => <col key={c.key} style={{ width: c.width }} />)}</colgroup>
          <thead>{this.renderHeader()}</thead>
          <tbody>{this.renderBody()}</tbody>
        </table>
      </div>
    );
  }
}

export default PoReconTable;
