import React, { Component } from "react";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import ExpandMore from "@material-ui/icons/ExpandMore";
import ExpandLess from "@material-ui/icons/ExpandLess";
import { messages } from "./../../messages";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import "./style.scss";

class Details extends Component {
  state = {
    value: 0,
    expandedWarehouse: null,
    activeTab: 'details',
    selectedWarehouseId: null,
    batches: [],
    batchesLoading: false,
    writeOffForm: null,
    writeOffSubmitting: false,
    splitFormOpen: false,
    splitEntries: [{ qty: '', expiryDate: '', brand: '' }],
    splitSubmitting: false,
    splitError: null,
  };

  handleExpandClick = (warehouseName) => {
    this.setState({
      expandedWarehouse: this.state.expandedWarehouse === warehouseName ? null : warehouseName,
    });
  };

  handleTabChange = (tab) => {
    this.setState({ activeTab: tab });
  };

  async loadBatches(productId, warehouseId) {
    this.setState({ batchesLoading: true, batches: [], splitFormOpen: false, splitError: null });
    const response = await API.GET(apiEndpoints.getBatchesForProduct(productId, warehouseId));
    if (response.success) {
      this.setState({ batches: response.data || [], batchesLoading: false });
    } else {
      this.setState({ batchesLoading: false });
    }
  }

  getUntrackedQty() {
    const { selectedWarehouseId, batches } = this.state;
    const data = this.props.data;
    if (!selectedWarehouseId) return 0;
    const warehouseStock = (data.detailedStock || []).find(
      s => s.warehouse?.warehouseId === selectedWarehouseId
    )?.quantityInHand || 0;
    const trackedQty = batches.reduce((sum, b) => sum + (b.qtyRemaining || 0), 0);
    return Math.max(parseFloat((warehouseStock - trackedQty).toFixed(4)), 0);
  }

  getSplitTotal() {
    return this.state.splitEntries.reduce((sum, e) => {
      const v = parseFloat(e.qty);
      return sum + (isNaN(v) ? 0 : v);
    }, 0);
  }

  updateSplitEntry(idx, field, value) {
    const entries = this.state.splitEntries.map((e, i) => i === idx ? { ...e, [field]: value } : e);
    this.setState({ splitEntries: entries, splitError: null });
  }

  addSplitEntry() {
    this.setState(prev => ({
      splitEntries: [...prev.splitEntries, { qty: '', expiryDate: '', brand: '' }],
      splitError: null,
    }));
  }

  removeSplitEntry(idx) {
    const entries = this.state.splitEntries.filter((_, i) => i !== idx);
    this.setState({ splitEntries: entries.length > 0 ? entries : [{ qty: '', expiryDate: '', brand: '' }], splitError: null });
  }

  fillRemaining(idx) {
    const untrackedQty = this.getUntrackedQty();
    const otherSum = this.state.splitEntries.reduce((sum, e, i) => {
      if (i === idx) return sum;
      const v = parseFloat(e.qty);
      return sum + (isNaN(v) ? 0 : v);
    }, 0);
    const remaining = parseFloat((untrackedQty - otherSum).toFixed(4));
    if (remaining > 0) this.updateSplitEntry(idx, 'qty', String(remaining));
  }

  async submitSplit() {
    const { selectedWarehouseId, splitEntries } = this.state;
    const data = this.props.data;
    const untrackedQty = this.getUntrackedQty();
    const total = this.getSplitTotal();

    if (Math.abs(total - untrackedQty) > 0.001) {
      this.setState({ splitError: `Total allocated (${total}) must equal untracked stock (${untrackedQty}).` });
      return;
    }
    for (const e of splitEntries) {
      if (!e.qty || parseFloat(e.qty) <= 0) {
        this.setState({ splitError: 'All batch quantities must be greater than zero.' });
        return;
      }
      if (!e.expiryDate) {
        this.setState({ splitError: 'Expiry date is required for every batch.' });
        return;
      }
    }

    this.setState({ splitSubmitting: true, splitError: null });
    const payload = {
      warehouseId: selectedWarehouseId,
      batches: splitEntries.map(e => ({
        qty: parseFloat(e.qty),
        expiryDate: e.expiryDate ? e.expiryDate.split('-').reverse().join('-') : null,
        brand: e.brand || null,
      })),
    };
    const response = await API.POST(apiEndpoints.splitExistingStock(data.productId), payload);
    this.setState({ splitSubmitting: false });
    if (response.success) {
      this.setState({ splitFormOpen: false, splitEntries: [{ qty: '', expiryDate: '', brand: '' }] });
      this.loadBatches(data.productId, selectedWarehouseId);
    } else {
      this.setState({ splitError: response.errorMessage || 'Split failed. Please try again.' });
    }
  }

  renderSplitPanel(untrackedQty) {
    const { splitFormOpen, splitEntries, splitSubmitting, splitError } = this.state;
    const total = this.getSplitTotal();
    const remaining = parseFloat((untrackedQty - total).toFixed(4));
    const isExact = Math.abs(remaining) <= 0.001;
    const isOver = remaining < -0.001;

    const progressPct = Math.min((total / untrackedQty) * 100, 100);
    const progressColor = isOver ? '#c62828' : isExact ? '#2e7d32' : '#1565c0';

    if (!splitFormOpen) {
      return (
        <div style={{
          background: '#fff8e1', border: '1px solid #ffe082', borderRadius: '6px',
          padding: '10px 14px', marginBottom: '12px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px',
        }}>
          <div style={{ fontSize: '12px', color: '#5d4037' }}>
            <strong>⚠ {untrackedQty} units untracked</strong>
            <span style={{ marginLeft: '8px', color: '#795548' }}>
              Outward is blocked until all stock is split into batches with expiry dates.
            </span>
          </div>
          <button
            style={{
              padding: '5px 14px', fontSize: '12px', fontWeight: 600,
              background: '#e65100', color: 'white', border: 'none',
              borderRadius: '4px', cursor: 'pointer', whiteSpace: 'nowrap',
            }}
            onClick={() => this.setState({ splitFormOpen: true, splitError: null, splitEntries: [{ qty: '', expiryDate: '', brand: '' }] })}
          >
            Split Stock →
          </button>
        </div>
      );
    }

    return (
      <div style={{
        background: '#fff', border: '2px solid #1565c0', borderRadius: '8px',
        marginBottom: '16px', overflow: 'hidden',
      }}>
        {/* Header */}
        <div style={{ background: '#1565c0', padding: '10px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ color: 'white', fontWeight: 600, fontSize: '13px' }}>
            Split Existing Stock — {this.props.data.productName}
          </div>
          <button
            style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer', fontSize: '16px', lineHeight: 1 }}
            onClick={() => this.setState({ splitFormOpen: false, splitError: null })}
          >×</button>
        </div>

        {/* Stats row */}
        <div style={{ display: 'flex', gap: '1px', background: '#e3f2fd', borderBottom: '1px solid #bbdefb' }}>
          {[
            { label: 'Total Stock', value: (this.getUntrackedQty() + this.state.batches.reduce((s, b) => s + (b.qtyRemaining || 0), 0)).toFixed(2) },
            { label: 'Already in Batches', value: this.state.batches.reduce((s, b) => s + (b.qtyRemaining || 0), 0).toFixed(2) },
            { label: 'To Split', value: untrackedQty.toFixed(2), highlight: true },
          ].map(({ label, value, highlight }) => (
            <div key={label} style={{ flex: 1, padding: '8px 12px', background: highlight ? '#fff3e0' : 'white', textAlign: 'center' }}>
              <div style={{ fontSize: '11px', color: '#666', marginBottom: '2px' }}>{label}</div>
              <div style={{ fontSize: '16px', fontWeight: 700, color: highlight ? '#e65100' : '#333' }}>{value}</div>
            </div>
          ))}
        </div>

        <div style={{ padding: '12px 14px' }}>
          {/* Allocation progress */}
          <div style={{ marginBottom: '10px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', marginBottom: '4px' }}>
              <span style={{ color: '#555' }}>Allocated: <strong style={{ color: progressColor }}>{total.toFixed(2)}</strong> of {untrackedQty}</span>
              <span style={{ color: isOver ? '#c62828' : isExact ? '#2e7d32' : '#888', fontWeight: 600 }}>
                {isOver ? `Over by ${Math.abs(remaining).toFixed(2)}` : isExact ? '✓ Fully allocated' : `${remaining.toFixed(2)} remaining`}
              </span>
            </div>
            <div style={{ height: '6px', background: '#e0e0e0', borderRadius: '3px', overflow: 'hidden' }}>
              <div style={{ height: '100%', width: `${progressPct}%`, background: progressColor, borderRadius: '3px', transition: 'width 0.2s, background 0.2s' }} />
            </div>
          </div>

          {/* Batch entry table */}
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px', marginBottom: '8px' }}>
            <thead>
              <tr style={{ background: '#f5f5f5' }}>
                <th style={{ padding: '5px 6px', border: '1px solid #ddd', width: '28px', textAlign: 'center' }}>#</th>
                <th style={{ padding: '5px 6px', border: '1px solid #ddd', width: '90px' }}>Qty *</th>
                <th style={{ padding: '5px 6px', border: '1px solid #ddd', width: '140px' }}>Expiry Date *</th>
                <th style={{ padding: '5px 6px', border: '1px solid #ddd' }}>Brand (optional)</th>
                <th style={{ padding: '5px 6px', border: '1px solid #ddd', width: '60px', textAlign: 'center' }}>Fill</th>
                <th style={{ padding: '5px 6px', border: '1px solid #ddd', width: '30px' }}></th>
              </tr>
            </thead>
            <tbody>
              {splitEntries.map((entry, idx) => (
                <tr key={idx}>
                  <td style={{ padding: '4px 6px', border: '1px solid #ddd', textAlign: 'center', color: '#999', fontSize: '11px' }}>{idx + 1}</td>
                  <td style={{ padding: '4px 6px', border: '1px solid #ddd' }}>
                    <input
                      type="number" min="0" step="any"
                      style={{ width: '100%', padding: '3px 5px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                      value={entry.qty}
                      onChange={e => this.updateSplitEntry(idx, 'qty', e.target.value)}
                      placeholder="0"
                    />
                  </td>
                  <td style={{ padding: '4px 6px', border: '1px solid #ddd' }}>
                    <input
                      type="date"
                      style={{ width: '100%', padding: '3px 5px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                      value={entry.expiryDate}
                      onChange={e => this.updateSplitEntry(idx, 'expiryDate', e.target.value)}
                    />
                  </td>
                  <td style={{ padding: '4px 6px', border: '1px solid #ddd' }}>
                    <input
                      type="text"
                      style={{ width: '100%', padding: '3px 5px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                      value={entry.brand}
                      onChange={e => this.updateSplitEntry(idx, 'brand', e.target.value)}
                      placeholder="e.g. ABC Corp"
                    />
                  </td>
                  <td style={{ padding: '4px 6px', border: '1px solid #ddd', textAlign: 'center' }}>
                    <button
                      title="Fill with remaining unallocated qty"
                      style={{ padding: '2px 8px', fontSize: '11px', cursor: 'pointer', background: '#e3f2fd', color: '#1565c0', border: '1px solid #90caf9', borderRadius: '3px' }}
                      onClick={() => this.fillRemaining(idx)}
                    >Fill</button>
                  </td>
                  <td style={{ padding: '4px 6px', border: '1px solid #ddd', textAlign: 'center' }}>
                    <button
                      style={{ padding: '2px 6px', fontSize: '13px', cursor: 'pointer', background: 'transparent', color: '#999', border: 'none', lineHeight: 1 }}
                      onClick={() => this.removeSplitEntry(idx)}
                      title="Remove row"
                    >×</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <button
            style={{ fontSize: '12px', color: '#1565c0', background: 'transparent', border: '1px dashed #90caf9', borderRadius: '4px', padding: '4px 10px', cursor: 'pointer', marginBottom: '10px' }}
            onClick={() => this.addSplitEntry()}
          >+ Add batch</button>

          {splitError && (
            <div style={{ background: '#ffebee', border: '1px solid #ef9a9a', borderRadius: '4px', padding: '6px 10px', fontSize: '12px', color: '#c62828', marginBottom: '8px' }}>
              {splitError}
            </div>
          )}

          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
            <button
              style={{ padding: '6px 16px', fontSize: '12px', background: '#757575', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
              onClick={() => this.setState({ splitFormOpen: false, splitError: null })}
            >Cancel</button>
            <button
              disabled={!isExact || splitSubmitting}
              style={{
                padding: '6px 20px', fontSize: '12px', fontWeight: 600,
                background: isExact ? '#2e7d32' : '#bdbdbd',
                color: 'white', border: 'none', borderRadius: '4px',
                cursor: isExact ? 'pointer' : 'not-allowed',
                transition: 'background 0.2s',
              }}
              onClick={() => this.submitSplit()}
            >
              {splitSubmitting ? 'Saving...' : 'Save Batches'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  async submitWriteOff(batchId) {
    const { writeOffForm } = this.state;
    if (!writeOffForm || !writeOffForm.quantity || !writeOffForm.reason || !writeOffForm.writeOffDate) {
      alert("Quantity, reason, and write-off date are required.");
      return;
    }
    this.setState({ writeOffSubmitting: true });
    const response = await API.POST(apiEndpoints.writeOffBatch(batchId), {
      quantity: parseFloat(writeOffForm.quantity),
      reason: writeOffForm.reason,
      writeOffDate: writeOffForm.writeOffDate,
    });
    this.setState({ writeOffSubmitting: false });
    if (response.success) {
      this.setState({ writeOffForm: null });
      const { selectedWarehouseId } = this.state;
      const productId = this.props.data.productId;
      if (productId && selectedWarehouseId) {
        this.loadBatches(productId, selectedWarehouseId);
      }
    } else {
      alert(response.errorMessage || "Write-off failed.");
    }
  }

  render() {
    const data = this.props.data;

    return (
      <div className="list-section detail-section stock">
        <div className="details-header">
          {messages.common.details}
          <div>
            <IconButton
              aria-label="back"
              onClick={() => this.props.close(data)}
              className="back-icon"
            >
              <Clear />
            </IconButton>
          </div>
        </div>
        <div className="details-tabs">
          <button 
            className={`tab-button ${this.state.activeTab === 'details' ? 'active' : ''}`}
            onClick={() => this.handleTabChange('details')}
          >
            Details
          </button>
          <button
            className={`tab-button ${this.state.activeTab === 'history' ? 'active' : ''}`}
            onClick={() => this.handleTabChange('history')}
          >
            History
          </button>
          <button
            className={`tab-button ${this.state.activeTab === 'batches' ? 'active' : ''}`}
            onClick={() => this.handleTabChange('batches')}
          >
            Batches
          </button>
        </div>
        <div className="content-container">
          {this.state.activeTab === 'details' && (
            <div className="details-main-content">
              {/* Existing details content */}
              <div className="detail-item">
                <div className="label">{messages.common.product}</div>
                <div className="value">{data.productName}</div>
              </div>
              <div className="detail-item">
                <div className="label">{messages.common.inventoryCode}</div>
                <div className="value">{data.productCode || "-"}</div>
              </div>
              <div className="detail-item">
                <div className="label">{messages.common.category}</div>
                <div className="value">{data.categoryName}</div>
              </div>
              <div className="detail-item">
                <div className="label">{messages.common.availableStock}</div>
                <div className="value">{data.totalQuantityInHand}</div>
              </div>
              <table>
                <thead>
                  <tr>
                    <th>{messages.common.warehouse}</th>
                    <th>{messages.common.warehouseStock}</th>
                    <th>{messages.common.unit}</th>
                  </tr>
                </thead>
                <tbody>
                  {data.detailedStock.map((item) => (
                    <React.Fragment key={item.warehouseName}>
                      <tr>
                        <td>
                          {item.stockAgingData ? (
                            <div className="accordion-header" onClick={() => this.handleExpandClick(item.warehouseName)}>
                              <span className="accordion-icon">
                                {this.state.expandedWarehouse === item.warehouseName ? "−" : "+"}
                              </span>
                              {item.warehouseName}
                            </div>
                          ) : (
                            item.warehouseName
                          )}
                        </td>
                        <td>{item.quantityInHand}</td>
                        <td>{item.measurementUnit}</td>
                      </tr>
                      {this.state.expandedWarehouse === item.warehouseName && item.stockAgingData && (
                        <tr>
                          <td colSpan="3">
                            <div className="stock-aging-section">
                              <div className="stock-aging-header">
                                <h3>Stock Aging</h3>
                              </div>
                              <table className="stock-aging-table">
                                <thead>
                                  <tr>
                                    <th>Quantity</th>
                                    <th>Inward Date</th>
                                    <th>Age</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {item.stockAgingData.map((agingItem, index) => (
                                    <tr key={index}>
                                      <td>{agingItem.quantity}</td>
                                      <td>{agingItem.date}</td>
                                      <td>{agingItem.age}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {this.state.activeTab === 'history' && (
            <div className="history-content">
              <table>
                <thead>
                  <tr>
                    <th>Warehouse</th>
                    <th>Type</th>
                    <th>Date</th>
                    <th>Quantity</th>
                  </tr>
                </thead>
                <tbody>
                  {data.inwardOutwardHistory.map((item, index) => (
                    <tr key={index}>
                      <td>{item.warehouseName}</td>
                      <td>{item.type}</td>
                      <td>{item.date}</td>
                      <td>{item.quantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {this.state.activeTab === 'batches' && (
            <div style={{ padding: '8px' }}>
              {/* Warehouse selector */}
              <div style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '13px', fontWeight: 'bold', marginRight: '8px' }}>Warehouse:</label>
                <select
                  style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #ccc', fontSize: '13px' }}
                  value={this.state.selectedWarehouseId || ''}
                  onChange={(e) => {
                    const warehouseId = e.target.value ? parseInt(e.target.value) : null;
                    this.setState({ selectedWarehouseId: warehouseId, batches: [], writeOffForm: null, splitFormOpen: false, splitError: null, splitEntries: [{ qty: '', expiryDate: '', brand: '' }] });
                    if (warehouseId) {
                      this.loadBatches(data.productId, warehouseId);
                    }
                  }}
                >
                  <option value="">Select warehouse...</option>
                  {data.detailedStock.map((item, idx) => (
                    <option key={idx} value={item.warehouse?.warehouseId}>
                      {item.warehouse?.warehouseName || item.warehouseName || 'Unknown'}
                    </option>
                  ))}
                </select>
              </div>

              {this.state.batchesLoading && (
                <div style={{ padding: '20px', textAlign: 'center', color: '#888', fontSize: '13px' }}>Loading batches…</div>
              )}

              {!this.state.batchesLoading && this.state.selectedWarehouseId && (() => {
                const untrackedQty = this.getUntrackedQty();
                const isExpirable = data.isExpirable;
                return (
                  <>
                    {/* Split panel — only for expirable products with untracked stock */}
                    {isExpirable && untrackedQty > 0.001 && this.renderSplitPanel(untrackedQty)}

                    {this.state.batches.length === 0 ? (
                      <div style={{ color: '#888', fontSize: '13px', padding: '8px 0' }}>
                        No batch records found.
                        {!isExpirable && ' Batch tracking only applies to products with "Track Expiry Date" enabled.'}
                      </div>
                    ) : (
                      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                        <thead>
                          <tr style={{ backgroundColor: '#f5f5f5' }}>
                            <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Batch</th>
                            <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Brand</th>
                            <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Received</th>
                            <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Expiry</th>
                            <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'center' }}>Status</th>
                            <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'right' }}>Qty Remaining</th>
                            <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'center' }}>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {this.state.batches.map((batch) => {
                            const isExpired = batch.isExpired;
                            const isNearExpiry = !isExpired && batch.daysUntilExpiry != null && batch.daysUntilExpiry <= 30;
                            const rowStyle = isExpired
                              ? { backgroundColor: '#ffebee' }
                              : isNearExpiry
                              ? { backgroundColor: '#fff8e1' }
                              : {};
                            const isSplitOrigin = batch.inwardId === -1;
                            return (
                              <React.Fragment key={batch.batchId}>
                                <tr style={rowStyle}>
                                  <td style={{ padding: '6px', border: '1px solid #ddd' }}>
                                    <span style={{ fontSize: '11px', color: '#999' }}>#{batch.batchId}</span>
                                    {isSplitOrigin && (
                                      <span style={{ marginLeft: '4px', fontSize: '10px', background: '#e3f2fd', color: '#1565c0', borderRadius: '3px', padding: '1px 4px' }}>split</span>
                                    )}
                                  </td>
                                  <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.brand || <span style={{ color: '#bbb' }}>—</span>}</td>
                                  <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.receivedDate ? new Date(batch.receivedDate).toLocaleDateString('en-GB') : '—'}</td>
                                  <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.expiryDate ? new Date(batch.expiryDate).toLocaleDateString('en-GB') : <span style={{ color: '#bbb' }}>—</span>}</td>
                                  <td style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'center' }}>
                                    {batch.expiryDate ? (
                                      isExpired ? (
                                        <span style={{ background: '#ffcdd2', color: '#c62828', borderRadius: '3px', padding: '1px 6px', fontSize: '11px', fontWeight: 600 }}>Expired</span>
                                      ) : isNearExpiry ? (
                                        <span style={{ background: '#ffe0b2', color: '#e65100', borderRadius: '3px', padding: '1px 6px', fontSize: '11px' }}>{batch.daysUntilExpiry}d left</span>
                                      ) : (
                                        <span style={{ background: '#e8f5e9', color: '#2e7d32', borderRadius: '3px', padding: '1px 6px', fontSize: '11px' }}>{batch.daysUntilExpiry}d left</span>
                                      )
                                    ) : <span style={{ color: '#bbb', fontSize: '11px' }}>—</span>}
                                  </td>
                                  <td style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'right', fontWeight: 600 }}>{batch.qtyRemaining}</td>
                                  <td style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'center' }}>
                                    <button
                                      style={{ padding: '3px 8px', fontSize: '11px', cursor: 'pointer', backgroundColor: '#e53935', color: 'white', border: 'none', borderRadius: '3px' }}
                                      onClick={() => this.setState({ writeOffForm: { batchId: batch.batchId, quantity: '', reason: '', writeOffDate: '' } })}
                                    >
                                      Write Off
                                    </button>
                                  </td>
                                </tr>
                                {this.state.writeOffForm && this.state.writeOffForm.batchId === batch.batchId && (
                                  <tr>
                                    <td colSpan="7" style={{ padding: '8px', backgroundColor: '#fafafa', border: '1px solid #ddd' }}>
                                      <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                                        <div>
                                          <div style={{ fontSize: '11px', marginBottom: '2px' }}>Quantity *</div>
                                          <input type="number" style={{ padding: '4px', width: '80px', border: '1px solid #ccc', borderRadius: '3px' }}
                                            value={this.state.writeOffForm.quantity}
                                            onChange={e => this.setState({ writeOffForm: { ...this.state.writeOffForm, quantity: e.target.value } })}
                                            max={batch.qtyRemaining} min="0" step="any"
                                          />
                                        </div>
                                        <div>
                                          <div style={{ fontSize: '11px', marginBottom: '2px' }}>Date *</div>
                                          <input type="date" style={{ padding: '4px', border: '1px solid #ccc', borderRadius: '3px' }}
                                            value={this.state.writeOffForm.writeOffDate}
                                            onChange={e => this.setState({ writeOffForm: { ...this.state.writeOffForm, writeOffDate: e.target.value } })}
                                          />
                                        </div>
                                        <div style={{ flex: 1, minWidth: '160px' }}>
                                          <div style={{ fontSize: '11px', marginBottom: '2px' }}>Reason *</div>
                                          <input type="text" style={{ padding: '4px', width: '100%', border: '1px solid #ccc', borderRadius: '3px' }}
                                            value={this.state.writeOffForm.reason}
                                            onChange={e => this.setState({ writeOffForm: { ...this.state.writeOffForm, reason: e.target.value } })}
                                            placeholder="Reason for write-off"
                                          />
                                        </div>
                                        <button
                                          style={{ padding: '4px 12px', backgroundColor: '#1565c0', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer', fontSize: '12px' }}
                                          disabled={this.state.writeOffSubmitting}
                                          onClick={() => this.submitWriteOff(batch.batchId)}
                                        >
                                          {this.state.writeOffSubmitting ? 'Saving…' : 'Confirm'}
                                        </button>
                                        <button
                                          style={{ padding: '4px 12px', backgroundColor: '#757575', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer', fontSize: '12px' }}
                                          onClick={() => this.setState({ writeOffForm: null })}
                                        >
                                          Cancel
                                        </button>
                                      </div>
                                    </td>
                                  </tr>
                                )}
                              </React.Fragment>
                            );
                          })}
                        </tbody>
                      </table>
                    )}
                  </>
                );
              })()}
            </div>
          )}
        </div>
      </div>
    );
  }
}

export default Details;
