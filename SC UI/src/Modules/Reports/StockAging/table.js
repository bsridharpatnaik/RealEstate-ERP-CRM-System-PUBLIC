import React from 'react';
import CommonTable from '../../../Shared/Table';
import { messages } from '../../../messages';
import Tooltip from '@material-ui/core/Tooltip';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';

const BUCKET_COLORS = {
  '0-30':  { color: '#27ae60', bg: '#eafaf1' },
  '31-60': { color: '#f39c12', bg: '#fef9e7' },
  '61-90': { color: '#e67e22', bg: '#fdf2e9' },
  '90+':   { color: '#e74c3c', bg: '#fdedec' },
};

const COLUMNS = [
  { header: 'Project',         key: 'tenantSchema',    width: 110, align: 'left'  },
  { header: 'Product',         key: 'productName',     width: 160, align: 'left'  },
  { header: 'Code',            key: 'productCode',     width: 75,  align: 'left'  },
  { header: 'Unit',            key: 'unit',            width: 50,  align: 'center'},
  { header: 'Category',        key: 'category',        width: 100, align: 'left'  },
  { header: 'Qty in Hand',     key: 'totalQtyInHand',  width: 90,  align: 'right' },
  { header: 'Oldest Stock',    key: 'lastInwardDate',  width: 100, align: 'left'  },
  { header: 'Aging (Days)',    key: 'minAgingDays',    width: 90,  align: 'right' },
  { header: 'Bucket',          key: 'agingBucket',     width: 80,  align: 'center'},
  { header: 'POG (₹)',         key: 'pog',             width: 90,  align: 'right' },
  { header: 'PO Rate (₹)',     key: 'lastPoRate',      width: 85,  align: 'right' },
  { header: 'Last PO Date',    key: 'lastPoDate',      width: 100, align: 'left'  },
];

const TOTAL_WIDTH = COLUMNS.reduce((sum, c) => sum + c.width, 0);

const cellBase = {
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  padding: '7px 8px',
  borderBottom: '1px solid #f0f0f0',
  fontSize: 13,
};

const thBase = {
  ...cellBase,
  background: '#f5f5f5',
  fontWeight: 600,
  fontSize: 12,
  borderBottom: '2px solid #ddd',
  color: '#333',
  position: 'sticky',
  top: 0,
  zIndex: 1,
};

function formatNum(val) {
  if (val == null) return <span style={{ color: '#ccc' }}>—</span>;
  return Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

class StockAgingTable extends CommonTable {

  componentDidMount() {
    // Override base class — we use COLUMNS constant, not tableData prop
    this.setState({ rows: this.props.rows || [] });
  }

  componentDidUpdate(prevProps) {
    if (prevProps.rows !== this.props.rows) {
      this.setState({ rows: this.props.rows || [] });
    }
  }

  sortCol(key) {
    const { sortkey, sortby } = this.props;
    let newSortBy;
    if (sortkey === key) {
      newSortBy = sortby === 'asc' ? 'desc' : sortby === 'desc' ? null : 'asc';
    } else {
      newSortBy = 'asc';
    }
    this.props.search && this.props.search(newSortBy ? key : null, newSortBy);
  }

  renderSortIcon(key) {
    const { sortkey, sortby } = this.props;
    if (sortkey !== key) return null;
    return sortby === 'desc'
      ? <ArrowDropDownIcon style={{ fontSize: 16, verticalAlign: 'middle' }} />
      : <ArrowDropUpIcon style={{ fontSize: 16, verticalAlign: 'middle' }} />;
  }

  renderHeader() {
    return (
      <tr>
        {COLUMNS.map((col) => (
          <th
            key={col.key}
            onClick={() => this.sortCol(col.key)}
            style={{ ...thBase, width: col.width, textAlign: col.align, cursor: 'pointer', userSelect: 'none' }}
          >
            {col.header}{this.renderSortIcon(col.key)}
          </th>
        ))}
      </tr>
    );
  }

  renderCell(col, row) {
    const val = row[col.key];

    if (col.key === 'agingBucket') {
      if (!val) return <span style={{ color: '#ccc' }}>—</span>;
      const style = BUCKET_COLORS[val] || {};
      return (
        <span style={{
          background: style.bg,
          color: style.color,
          padding: '2px 8px',
          borderRadius: 10,
          fontWeight: 600,
          fontSize: 11,
          border: `1px solid ${style.color}`,
          whiteSpace: 'nowrap',
        }}>
          {val}
        </span>
      );
    }

    if (col.key === 'minAgingDays') {
      if (val == null) return <span style={{ color: '#ccc' }}>—</span>;
      const bucket = row.agingBucket;
      const style = BUCKET_COLORS[bucket] || {};
      return (
        <span style={{ color: style.color || '#333', fontWeight: 600 }}>
          {val === 9999 ? 'No Inward' : val}
        </span>
      );
    }

    if (col.key === 'pog' || col.key === 'lastPoRate') {
      return formatNum(val);
    }

    if (col.key === 'totalQtyInHand') {
      return val != null ? Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '—';
    }

    if (val == null || val === '') return <span style={{ color: '#ccc' }}>—</span>;
    return String(val);
  }

  renderBody() {
    const rows = this.state.rows || [];
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={COLUMNS.length} style={{ textAlign: 'center', padding: 32, color: '#999', fontSize: 13 }}>
            {messages.common.noRecords}
          </td>
        </tr>
      );
    }
    return rows.map((row, index) => (
      <tr
        key={index}
        style={{ background: index % 2 === 0 ? '#ffffff' : '#fafafa', cursor: 'pointer' }}
        onClick={() => this.props.onRowClick && this.props.onRowClick(row)}
        title="Click to view warehouse breakdown"
      >
        {COLUMNS.map((col) => {
          const val = row[col.key];
          const tooltipTitle = (typeof val === 'string' && val.length > 0) ? val : '';
          return (
            <Tooltip key={col.key} title={tooltipTitle} placement="top" disableHoverListener={!tooltipTitle}>
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
          <colgroup>
            {COLUMNS.map((col) => <col key={col.key} style={{ width: col.width }} />)}
          </colgroup>
          <thead>{this.renderHeader()}</thead>
          <tbody>{this.renderBody()}</tbody>
        </table>
      </div>
    );
  }
}

export default StockAgingTable;
