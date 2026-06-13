import React, { Component } from 'react';
import { getProjectColor } from '../Reports/projectColors';

/**
 * 3-level tree table: Project+Product → Warehouse → Batch rows
 *
 * Backend returns flat rows (one per batch). We group:
 *   Level 1 key: tenantSchema + productId
 *   Level 2 key: tenantSchema + productId + warehouseId
 */

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

function expiryColor(days) {
  if (days < 0)  return { color: '#b71c1c', bg: '#ffebee', label: 'Expired' };
  if (days <= 1) return { color: '#c62828', bg: '#ffcdd2', label: 'Today/Tomorrow' };
  if (days <= 10) return { color: '#e65100', bg: '#fbe9e7', label: `${days}d` };
  if (days <= 30) return { color: '#ef6c00', bg: '#fff3e0', label: `${days}d` };
  if (days <= 90) return { color: '#f9a825', bg: '#fffde7', label: `${days}d` };
  return { color: '#388e3c', bg: '#e8f5e9', label: `${days}d` };
}

function fmtQty(val) {
  if (val == null) return <span style={{ color: '#ccc' }}>—</span>;
  return Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

class ExpiredStockTable extends Component {
  state = { expandedProducts: {}, expandedWarehouses: {} };

  componentDidUpdate(prevProps) {
    if (prevProps.rows !== this.props.rows) {
      this.setState({ expandedProducts: {}, expandedWarehouses: {} });
    }
  }

  toggleProduct(key) {
    this.setState(prev => ({
      expandedProducts: { ...prev.expandedProducts, [key]: !prev.expandedProducts[key] }
    }));
  }

  toggleWarehouse(key) {
    this.setState(prev => ({
      expandedWarehouses: { ...prev.expandedWarehouses, [key]: !prev.expandedWarehouses[key] }
    }));
  }

  buildTree(rows) {
    // productKey → { meta, warehouses: { warehouseKey → { meta, batches[] } } }
    const products = {};
    const productOrder = [];
    for (const row of rows) {
      const pk = `${row.tenantSchema}__${row.productId}`;
      if (!products[pk]) {
        products[pk] = {
          tenantSchema: row.tenantSchema,
          productId: row.productId,
          productName: row.productName,
          productCode: row.productCode,
          unit: row.unit,
          category: row.category,
          warehouses: {},
          warehouseOrder: [],
          totalQty: 0,
          minDays: Infinity,
        };
        productOrder.push(pk);
      }
      const p = products[pk];
      p.totalQty += (row.qtyRemaining || 0);
      if (row.daysUntilExpiry != null && row.daysUntilExpiry < p.minDays) {
        p.minDays = row.daysUntilExpiry;
      }

      const wk = `${pk}__${row.warehouseId}`;
      if (!p.warehouses[wk]) {
        p.warehouses[wk] = {
          warehouseId: row.warehouseId,
          warehouseName: row.warehouseName,
          batches: [],
          totalQty: 0,
          minDays: Infinity,
        };
        p.warehouseOrder.push(wk);
      }
      const w = p.warehouses[wk];
      w.batches.push(row);
      w.totalQty += (row.qtyRemaining || 0);
      if (row.daysUntilExpiry != null && row.daysUntilExpiry < w.minDays) {
        w.minDays = row.daysUntilExpiry;
      }
    }
    return { products, productOrder };
  }

  render() {
    const { rows = [], tenantMap = {} } = this.props;
    const { expandedProducts, expandedWarehouses } = this.state;

    if (!rows.length) {
      return (
        <div style={{ textAlign: 'center', padding: 40, color: '#999', fontSize: 13 }}>
          No expiry-tracked batches found.
        </div>
      );
    }

    const { products, productOrder } = this.buildTree(rows);
    const tableRows = [];

    productOrder.forEach((pk, pi) => {
      const p = products[pk];
      const projectName = tenantMap[p.tenantSchema] || p.tenantSchema;
      const pc = getProjectColor(p.tenantSchema);
      const isProdExp = expandedProducts[pk];
      const ec = p.minDays !== Infinity ? expiryColor(p.minDays) : { color: '#555', bg: 'transparent', label: '' };

      // Level 1 — Product row
      tableRows.push(
        <tr
          key={pk}
          style={{ background: pi % 2 === 0 ? '#fff' : '#fafafa', cursor: 'pointer' }}
          onClick={() => this.toggleProduct(pk)}
        >
          {/* expand */}
          <td style={{ ...cellBase, textAlign: 'center', color: '#888', fontSize: 16, width: 28 }}>
            {isProdExp ? '▲' : '▼'}
          </td>
          {/* project */}
          <td style={{ ...cellBase, width: 110 }}>
            <span style={{
              background: pc.bg, color: pc.color, border: `1px solid ${pc.border}`,
              fontSize: 11, fontWeight: 600, padding: '2px 7px', borderRadius: 8,
              display: 'inline-block', maxWidth: '100%',
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }}>
              {projectName}
            </span>
          </td>
          {/* product */}
          <td style={{ ...cellBase, fontWeight: 600, width: 180 }}>{p.productName || '—'}</td>
          <td style={{ ...cellBase, width: 75 }}>{p.productCode || <span style={{ color: '#ccc' }}>—</span>}</td>
          <td style={{ ...cellBase, textAlign: 'center', width: 55 }}>{p.unit || '—'}</td>
          <td style={{ ...cellBase, width: 100 }}>{p.category || <span style={{ color: '#ccc' }}>—</span>}</td>
          {/* warehouse — blank at product level */}
          <td style={{ ...cellBase, width: 140, color: '#aaa', fontSize: 12 }}>
            {p.warehouseOrder.length} warehouse{p.warehouseOrder.length !== 1 ? 's' : ''}
          </td>
          {/* lot/brand/received — blank */}
          <td style={cellBase} />
          <td style={cellBase} />
          <td style={cellBase} />
          {/* expiry */}
          <td style={{ ...cellBase, width: 95, textAlign: 'center' }}>
            {p.minDays !== Infinity && (
              <span style={{
                background: ec.bg, color: ec.color, border: `1px solid ${ec.color}40`,
                fontSize: 11, fontWeight: 700, padding: '2px 8px', borderRadius: 10,
              }}>
                {p.minDays < 0 ? 'Expired' : `≤${p.minDays}d`}
              </span>
            )}
          </td>
          {/* qty */}
          <td style={{ ...cellBase, textAlign: 'right', fontWeight: 600, color: '#b71c1c', width: 85 }}>
            {fmtQty(p.totalQty)}
          </td>
        </tr>
      );

      if (!isProdExp) return;

      // Level 2 — Warehouse rows
      p.warehouseOrder.forEach((wk, wi) => {
        const w = p.warehouses[wk];
        const isWhExp = expandedWarehouses[wk];
        const wec = w.minDays !== Infinity ? expiryColor(w.minDays) : { color: '#555', bg: 'transparent', label: '' };

        tableRows.push(
          <tr
            key={wk}
            style={{ background: '#f0f4ff', cursor: 'pointer' }}
            onClick={(e) => { e.stopPropagation(); this.toggleWarehouse(wk); }}
          >
            <td style={{ ...cellBase, borderLeft: '3px solid #3f51b5', width: 28 }} />
            <td style={{ ...cellBase, paddingLeft: 24, color: '#888', fontSize: 12, width: 110 }}>
              {isWhExp ? '▲' : '▼'}
            </td>
            <td colSpan={4} style={{ ...cellBase, fontWeight: 600, color: '#3f51b5', fontSize: 13 }}>
              🏭 {w.warehouseName || '—'}
            </td>
            {/* warehouse column */}
            <td style={{ ...cellBase, width: 140, color: '#aaa', fontSize: 12 }}>
              {w.batches.length} batch{w.batches.length !== 1 ? 'es' : ''}
            </td>
            <td style={cellBase} />
            <td style={cellBase} />
            <td style={cellBase} />
            <td style={{ ...cellBase, width: 95, textAlign: 'center' }}>
              {w.minDays !== Infinity && (
                <span style={{
                  background: wec.bg, color: wec.color, border: `1px solid ${wec.color}40`,
                  fontSize: 11, fontWeight: 700, padding: '2px 8px', borderRadius: 10,
                }}>
                  {w.minDays < 0 ? 'Expired' : `≤${w.minDays}d`}
                </span>
              )}
            </td>
            <td style={{ ...cellBase, textAlign: 'right', fontWeight: 600, color: '#3f51b5', width: 85 }}>
              {fmtQty(w.totalQty)}
            </td>
          </tr>
        );

        if (!isWhExp) return;

        // Level 3 — Batch rows
        w.batches.forEach((b, bi) => {
          const bec = b.daysUntilExpiry != null ? expiryColor(b.daysUntilExpiry) : { color: '#555', bg: '#fff', label: '' };
          tableRows.push(
            <tr key={`${wk}_b${bi}`} style={{ background: '#fff8e1' }}>
              <td style={{ ...cellBase, borderLeft: '3px solid #ffc107', width: 28 }} />
              <td style={{ ...cellBase, width: 110, paddingLeft: 28, color: '#aaa', fontSize: 12 }}>
                Batch {bi + 1}
              </td>
              {/* product / code / unit / category blank at batch level */}
              <td style={{ ...cellBase, width: 180 }} />
              <td style={{ ...cellBase, width: 75 }} />
              <td style={{ ...cellBase, width: 55 }} />
              <td style={{ ...cellBase, width: 100 }} />
              {/* warehouse blank */}
              <td style={{ ...cellBase, width: 140 }} />
              {/* lot */}
              <td style={{ ...cellBase, fontSize: 12, color: '#555' }}>{b.lotNumber || <span style={{ color: '#ccc' }}>—</span>}</td>
              {/* brand */}
              <td style={{ ...cellBase, fontSize: 12, color: '#555' }}>{b.brand || <span style={{ color: '#ccc' }}>—</span>}</td>
              {/* received */}
              <td style={{ ...cellBase, fontSize: 12, color: '#555' }}>{b.receivedDate || <span style={{ color: '#ccc' }}>—</span>}</td>
              {/* expiry date + badge */}
              <td style={{ ...cellBase, width: 95, textAlign: 'center' }}>
                <span style={{
                  background: bec.bg, color: bec.color, border: `1px solid ${bec.color}40`,
                  fontSize: 11, fontWeight: 700, padding: '2px 8px', borderRadius: 10,
                  display: 'inline-block',
                }}>
                  {b.expiryDate || '—'}
                </span>
              </td>
              {/* qty */}
              <td style={{ ...cellBase, textAlign: 'right', width: 85, color: '#555', fontWeight: 600 }}>
                {fmtQty(b.qtyRemaining)}
              </td>
            </tr>
          );
        });
      });
    });

    return (
      <div style={{ overflowX: 'auto', width: '100%', border: '1px solid #e8e8e8', borderRadius: 6 }}>
        <table style={{ tableLayout: 'fixed', width: '100%', minWidth: 900, borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={{ ...thBase, width: 28 }} />
              <th style={{ ...thBase, width: 110 }}>Project</th>
              <th style={{ ...thBase, width: 180 }}>Product</th>
              <th style={{ ...thBase, width: 75 }}>Code</th>
              <th style={{ ...thBase, width: 55, textAlign: 'center' }}>Unit</th>
              <th style={{ ...thBase, width: 100 }}>Category</th>
              <th style={{ ...thBase, width: 140 }}>Warehouse</th>
              <th style={{ ...thBase, width: 100 }}>Lot #</th>
              <th style={{ ...thBase, width: 100 }}>Brand</th>
              <th style={{ ...thBase, width: 95 }}>Recv. Date</th>
              <th style={{ ...thBase, width: 95, textAlign: 'center' }}>Expiry</th>
              <th style={{ ...thBase, width: 85, textAlign: 'right' }}>Qty</th>
            </tr>
          </thead>
          <tbody>{tableRows}</tbody>
        </table>
      </div>
    );
  }
}

export default ExpiredStockTable;
