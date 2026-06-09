import React from 'react';
import CommonTable from '../../../Shared/Table';
import { messages } from '../../../messages';
import Tooltip from '@material-ui/core/Tooltip';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';

const INDENT_STATUS_COLORS = {
  'CLOSED':         { color: '#27ae60', bg: '#eafaf1' },
  'PO COMPLETED':   { color: '#27ae60', bg: '#eafaf1' },
  'INWARD PARTIAL': { color: '#f39c12', bg: '#fef9e7' },
  'PO PARTIAL':     { color: '#f39c12', bg: '#fef9e7' },
  'PO CREATED':     { color: '#3498db', bg: '#ebf5fb' },
  'APPROVED':       { color: '#9b59b6', bg: '#f5eef8' },
  'NEW':            { color: '#95a5a6', bg: '#f2f3f4' },
  'CANCELLED':      { color: '#e74c3c', bg: '#fdedec' },
  'REJECTED':       { color: '#e74c3c', bg: '#fdedec' },
  'SHORT CLOSED':   { color: '#e67e22', bg: '#fdf2e9' },
};

const LINE_STATUS_COLORS = {
  'INWARD COMPLETE': { color: '#27ae60' },
  'INWARD PARTIAL':  { color: '#f39c12' },
  'SHORT CLOSED':    { color: '#e67e22' },
  'PO CREATED':      { color: '#3498db' },
  'CANCELLED':       { color: '#e74c3c' },
  'NEW':             { color: '#95a5a6' },
};

const COLUMNS = [
  { header: 'Project',       key: 'project',          width: 100, align: 'left'  },
  { header: 'Indent ID',     key: 'indentId',         width: 80,  align: 'left'  },
  { header: 'Indent Date',   key: 'indentDate',       width: 90,  align: 'left'  },
  { header: 'Indent Status', key: 'indentStatus',     width: 110, align: 'center'},
  { header: 'Product',       key: 'productName',      width: 145, align: 'left'  },
  { header: 'Code',          key: 'productCode',      width: 65,  align: 'left'  },
  { header: 'Unit',          key: 'unit',             width: 45,  align: 'center'},
  { header: 'Requested',     key: 'requestedQty',     width: 80,  align: 'right' },
  { header: 'Received',      key: 'receivedQty',      width: 80,  align: 'right' },
  { header: 'Pending',       key: 'pendingQty',       width: 75,  align: 'right' },
  { header: '% Fulfilled',   key: 'percentFulfilled', width: 95,  align: 'center'},
  { header: 'PO Number',     key: 'poNumber',         width: 80,  align: 'left'  },
  { header: 'Line Status',   key: 'lineItemStatus',   width: 110, align: 'center'},
  { header: 'Need By',       key: 'needByDate',       width: 85,  align: 'left'  },
];

const TOTAL_WIDTH = COLUMNS.reduce((sum, c) => sum + c.width, 0);

const cellBase = {
  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
  padding: '7px 8px', borderBottom: '1px solid #f0f0f0', fontSize: 13,
};
const thBase = {
  ...cellBase, background: '#f5f5f5', fontWeight: 600, fontSize: 12,
  borderBottom: '2px solid #ddd', color: '#333', position: 'sticky', top: 0, zIndex: 1,
};

function pctBar(pct) {
  const clamped = Math.min(pct || 0, 100);
  const color = clamped >= 100 ? '#27ae60' : clamped >= 50 ? '#f39c12' : '#e74c3c';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ flex: 1, height: 6, background: '#eee', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ width: `${clamped}%`, height: '100%', background: color, borderRadius: 3 }} />
      </div>
      <span style={{ fontSize: 11, color, fontWeight: 600, minWidth: 32, textAlign: 'right' }}>
        {clamped.toFixed(0)}%
      </span>
    </div>
  );
}

class IndentFulfillmentTable extends CommonTable {

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

    if (col.key === 'indentStatus') {
      const st = INDENT_STATUS_COLORS[val] || {};
      return val ? (
        <span style={{
          background: st.bg, color: st.color, padding: '2px 8px',
          borderRadius: 10, fontWeight: 600, fontSize: 11,
          border: `1px solid ${st.color || '#ccc'}`, whiteSpace: 'nowrap',
        }}>{val}</span>
      ) : null;
    }

    if (col.key === 'lineItemStatus') {
      const st = LINE_STATUS_COLORS[val] || {};
      return <span style={{ color: st.color || '#555', fontWeight: 600, fontSize: 12 }}>{val || '—'}</span>;
    }

    if (col.key === 'percentFulfilled') {
      return pctBar(val);
    }

    if (col.key === 'pendingQty') {
      const num = val == null ? 0 : Number(val);
      const color = num > 0 ? '#e74c3c' : '#27ae60';
      return <span style={{ color, fontWeight: 600 }}>{num.toLocaleString('en-IN', { maximumFractionDigits: 2 })}</span>;
    }

    if (col.key === 'requestedQty' || col.key === 'receivedQty') {
      return val != null ? Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '—';
    }

    if (col.key === 'needByDate' && val) {
      // Check if overdue
      const d = new Date(val.replace(/(\d{2})-(\d{2})-(\d{4})/, '$3-$2-$1'));
      const overdue = d < new Date() && row.pendingQty > 0;
      return <span style={{ color: overdue ? '#e74c3c' : '#333', fontWeight: overdue ? 600 : 400 }}>{val}</span>;
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
    return rows.map((row, i) => (
      <tr key={i} style={{ background: i % 2 === 0 ? '#fff' : '#fafafa' }}>
        {COLUMNS.map((col) => {
          const val = row[col.key];
          const tip = typeof val === 'string' && val.length > 0 ? val : '';
          return (
            <Tooltip key={col.key} title={tip} placement="top" disableHoverListener={!tip}>
              <td style={{ ...cellBase, width: col.width, textAlign: col.align }}>
                {this.renderCell(col, row)}
              </td>
            </Tooltip>
          );
        })}
      </tr>
    ));
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

export default IndentFulfillmentTable;
