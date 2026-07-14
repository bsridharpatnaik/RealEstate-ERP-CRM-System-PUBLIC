import React from 'react';
import CommonTable from '../../Shared/Table';
import Tooltip from '@material-ui/core/Tooltip';
import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';
import { getProjectColor } from '../Reports/projectColors';

const COLUMNS = [
  { header: '',              key: '_expand',      width: 28,  align: 'center' },
  { header: 'Project',      key: 'tenantSchema',  width: 110, align: 'left'  },
  { header: 'Product',      key: 'productName',   width: 180, align: 'left'  },
  { header: 'Code',         key: 'productCode',   width: 75,  align: 'left'  },
  { header: 'Unit',         key: 'unit',          width: 55,  align: 'center'},
  { header: 'Category',     key: 'category',      width: 100, align: 'left'  },
  { header: 'Qty (Dead)',   key: 'totalQty',      width: 90,  align: 'right' },
  { header: 'PO Rate (₹)', key: 'lastPoRate',    width: 95,  align: 'right' },
  { header: 'Value (₹)',   key: '_value',         width: 105, align: 'right' },
  { header: 'Batches',      key: '_batches',      width: 60,  align: 'center'},
];

const BATCH_COLUMNS = [
  { header: 'Lot #',     key: 'batchLotNumber',   width: 110 },
  { header: 'Brand',     key: 'batchBrand',        width: 110 },
  { header: 'Received',  key: 'batchReceivedDate', width: 95  },
  { header: 'Expiry',    key: 'batchExpiryDate',   width: 95  },
  { header: 'Qty',       key: 'batchQtyRemaining', width: 80  },
  { header: 'Value (₹)',key: '_batchValue',        width: 100 },
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

function fmt(val) {
  if (val == null) return <span style={{ color: '#ccc' }}>—</span>;
  return Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

class DeadStockTable extends CommonTable {
  state = { rows: [], expandedKeys: {} };

  componentDidMount() {
    this.setState({ rows: this.props.rows || [] });
  }

  componentDidUpdate(prevProps) {
    if (prevProps.rows !== this.props.rows) {
      this.setState({ rows: this.props.rows || [], expandedKeys: {} });
    }
  }

  toggleExpand(key) {
    this.setState(prev => ({
      expandedKeys: { ...prev.expandedKeys, [key]: !prev.expandedKeys[key] }
    }));
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
    const sortableKeys = new Set(['tenantSchema', 'productName', 'productCode', 'category', 'totalQty']);
    return (
      <tr>
        {COLUMNS.map(col => (
          <th
            key={col.key}
            onClick={() => sortableKeys.has(col.key) && this.sortCol(col.key)}
            style={{
              ...thBase,
              width: col.width,
              textAlign: col.align,
              cursor: sortableKeys.has(col.key) ? 'pointer' : 'default',
              userSelect: 'none',
            }}
          >
            {col.header}{sortableKeys.has(col.key) && this.renderSortIcon(col.key)}
          </th>
        ))}
      </tr>
    );
  }

  renderBatchRows(group) {
    const key = `${group.tenantSchema}__${group.productId}`;
    return group.batches.map((b, bi) => {
      const batchVal = group.lastPoRate != null
        ? (b.batchQtyRemaining || 0) * group.lastPoRate
        : null;
      return (
        <tr key={`${key}_batch_${bi}`} style={{ background: '#fffde7' }}>
          <td style={{ ...cellBase, borderLeft: '3px solid #ffc107' }} />
          <td colSpan={2} style={{ ...cellBase, paddingLeft: 28, color: '#777', fontSize: 12 }}>
            Batch {bi + 1}
          </td>
          <td style={{ ...cellBase, fontSize: 12, color: '#555' }}>{b.batchLotNumber || '—'}</td>
          <td style={{ ...cellBase, fontSize: 12, color: '#555', textAlign: 'center' }}>{b.batchBrand || '—'}</td>
          <td style={{ ...cellBase, fontSize: 12, color: '#555' }}>{b.batchReceivedDate || '—'}</td>
          <td style={{ ...cellBase, fontSize: 12, color: '#555', textAlign: 'right' }}>
            {b.batchQtyRemaining != null
              ? Number(b.batchQtyRemaining).toLocaleString('en-IN', { maximumFractionDigits: 2 })
              : '—'}
          </td>
          <td style={{ ...cellBase, fontSize: 12, color: '#555', textAlign: 'right' }}>
            {b.batchExpiryDate || '—'}
          </td>
          <td style={{ ...cellBase, fontSize: 12, textAlign: 'right', color: '#6a1b9a', fontWeight: 600 }}>
            {batchVal != null ? `₹${Number(batchVal).toLocaleString('en-IN', { maximumFractionDigits: 0 })}` : '—'}
          </td>
          <td style={cellBase} />
        </tr>
      );
    });
  }

  renderBody() {
    const { rows = [], expandedKeys } = this.state;
    const { tenantMap = {} } = this.props;

    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={COLUMNS.length} style={{ textAlign: 'center', padding: 32, color: '#999', fontSize: 13 }}>
            No dead stock found.
          </td>
        </tr>
      );
    }

    // Group rows by tenantSchema + productId
    const groups = {};
    const groupOrder = [];
    for (const row of rows) {
      const key = `${row.tenantSchema}__${row.productId}`;
      if (!groups[key]) {
        groups[key] = {
          tenantSchema: row.tenantSchema,
          productId: row.productId,
          productName: row.productName,
          productCode: row.productCode,
          unit: row.unit,
          category: row.category,
          totalQty: row.qty,
          lastPoRate: row.lastPoRate,
          batches: [],
        };
        groupOrder.push(key);
      }
      if (row.batchId) {
        groups[key].batches.push(row);
      }
    }

    const result = [];
    groupOrder.forEach((key, index) => {
      const g = groups[key];
      const isExpanded = expandedKeys[key];
      const hasBatches = g.batches.length > 0;
      const projectName = tenantMap[g.tenantSchema] || g.tenantSchema;
      const pc = getProjectColor(g.tenantSchema);
      const stockValue = g.lastPoRate != null ? (g.totalQty || 0) * g.lastPoRate : null;

      result.push(
        <tr
          key={key}
          style={{
            background: index % 2 === 0 ? '#ffffff' : '#fafafa',
            cursor: hasBatches ? 'pointer' : 'default',
          }}
          onClick={() => hasBatches && this.toggleExpand(key)}
        >
          {/* Expand toggle */}
          <td style={{ ...cellBase, textAlign: 'center', color: '#888', fontSize: 16 }}>
            {hasBatches ? (isExpanded ? '▲' : '▼') : ''}
          </td>
          {/* Project chip */}
          <td style={cellBase}>
            <span style={{
              background: pc.bg, color: pc.color, border: `1px solid ${pc.border}`,
              fontSize: 11, fontWeight: 600, padding: '2px 7px', borderRadius: 8,
              display: 'inline-block', maxWidth: '100%',
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }}>
              {projectName}
            </span>
          </td>
          <td style={cellBase}>{g.productName || '—'}</td>
          <td style={cellBase}>{g.productCode || <span style={{ color: '#ccc' }}>—</span>}</td>
          <td style={{ ...cellBase, textAlign: 'center' }}>{g.unit || '—'}</td>
          <td style={cellBase}>{g.category || <span style={{ color: '#ccc' }}>—</span>}</td>
          <td style={{ ...cellBase, textAlign: 'right', fontWeight: 600, color: '#b71c1c' }}>
            {fmt(g.totalQty)}
          </td>
          <td style={{ ...cellBase, textAlign: 'right', color: '#555' }}>
            {g.lastPoRate != null
              ? `₹${Number(g.lastPoRate).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`
              : <span style={{ color: '#ccc' }}>—</span>}
          </td>
          <td style={{ ...cellBase, textAlign: 'right', fontWeight: 600, color: '#6a1b9a' }}>
            {stockValue != null
              ? `₹${Number(stockValue).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
              : <span style={{ color: '#ccc' }}>—</span>}
          </td>
          <td style={{ ...cellBase, textAlign: 'center', color: '#888', fontSize: 12 }}>
            {hasBatches ? g.batches.length : '—'}
          </td>
        </tr>
      );

      if (hasBatches && isExpanded) {
        result.push(...this.renderBatchRows(g));
      }
    });

    return result;
  }

  render() {
    return (
      <div style={{ overflowX: 'auto', width: '100%', border: '1px solid #e8e8e8', borderRadius: 6 }}>
        <table style={{ tableLayout: 'fixed', width: TOTAL_WIDTH, minWidth: '100%', borderCollapse: 'collapse' }}>
          <colgroup>
            {COLUMNS.map(col => <col key={col.key} style={{ width: col.width }} />)}
          </colgroup>
          <thead>{this.renderHeader()}</thead>
          <tbody>{this.renderBody()}</tbody>
        </table>
      </div>
    );
  }
}

export default DeadStockTable;
