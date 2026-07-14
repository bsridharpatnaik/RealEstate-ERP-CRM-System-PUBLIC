import React from 'react';
import CommonTable from '../../../Shared/Table';
import { messages } from '../../../messages';
import Tooltip from '@material-ui/core/Tooltip';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';
import { getProjectColor } from '../projectColors';

const COLUMNS = [
  { header: 'Project',         key: 'tenantSchema',   width: 110, align: 'left'  },
  { header: 'Product',         key: 'productName',    width: 160, align: 'left'  },
  { header: 'Code',            key: 'productCode',    width: 75,  align: 'left'  },
  { header: 'Unit',            key: 'unit',           width: 50,  align: 'center'},
  { header: 'Category',        key: 'category',       width: 100, align: 'left'  },
  { header: 'Qty in Hand',     key: 'qtyInHand',      width: 90,  align: 'right' },
  { header: 'Reorder Level',   key: 'reorderLevel',   width: 95,  align: 'right' },
  { header: 'Deficit',         key: 'deficit',        width: 120, align: 'right' },
  { header: 'Low Stock Since', key: 'lowStockSince',  width: 130, align: 'left'  },
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

/** Color the deficit column — larger deficit = more urgent. */
function deficitColor(deficit, reorder) {
  if (!deficit || !reorder || reorder === 0) return '#333';
  const ratio = deficit / reorder;
  if (ratio >= 0.8) return '#e74c3c';
  if (ratio >= 0.5) return '#e67e22';
  if (ratio >= 0.2) return '#f39c12';
  return '#27ae60';
}

class LowStockTable extends CommonTable {

  componentDidMount() {
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

    if (col.key === 'deficit') {
      if (val == null) return <span style={{ color: '#ccc' }}>—</span>;
      const color = deficitColor(val, row.reorderLevel);
      const pct = row.reorderLevel > 0
        ? Math.round((val / row.reorderLevel) * 100)
        : null;
      return (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
          <span style={{ color, fontWeight: 700 }}>
            {Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 })}
          </span>
          {pct != null && (
            <span style={{
              fontSize: 10, fontWeight: 700, color: '#fff',
              background: color, borderRadius: 8, padding: '1px 5px',
              whiteSpace: 'nowrap',
            }}>
              {pct}%
            </span>
          )}
        </span>
      );
    }

    if (col.key === 'tenantSchema') {
      if (!val) return <span style={{ color: '#ccc' }}>—</span>;
      const pc = getProjectColor(val);
      const displayName = (this.props.tenantMap && this.props.tenantMap[val]) || val;
      return (
        <span style={{
          background: pc.bg, color: pc.color, border: `1px solid ${pc.border}`,
          fontSize: 11, fontWeight: 600, padding: '2px 7px', borderRadius: 8,
          display: 'inline-block', maxWidth: '100%',
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>
          {displayName}
        </span>
      );
    }

    if (col.key === 'qtyInHand' || col.key === 'reorderLevel') {
      return formatNum(val);
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
      <tr key={index} style={{ background: index % 2 === 0 ? '#ffffff' : '#fafafa' }}>
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

export default LowStockTable;
