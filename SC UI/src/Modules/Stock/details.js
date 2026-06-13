import React, { Component } from "react";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import ExpandMore from "@material-ui/icons/ExpandMore";
import ExpandLess from "@material-ui/icons/ExpandLess";
import { withSnackbar } from "notistack";
import { messages } from "./../../messages";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import "./style.scss";

class Details extends Component {
  state = {
    value: 0,
    expandedWarehouse: null,
    activeTab: 'details',
    splitFormOpenWarehouseId: null,
    batches: [],
    batchesLoading: false,
    writeOffForm: null,
    writeOffSubmitting: false,
    editBatchForm: null,
    stockAdjustments: {},
    writeOffHistories: {},
    writeOffHistoryLoading: {},
    splitEntries: [{ qty: '', expiryDate: '', brand: '', lotNumber: '' }],
    splitSubmitting: false,
    splitError: null,
    // Comments
    comments: [],
    commentsLoading: false,
    newComment: '',
    commentSubmitting: false,
    // Mark as Dead Stock modal
    markDeadStockModal: null, // { warehouseId, warehouseName, qty: '', comment: '', submitting: false }
    // Move from Dead Stock modal
    moveFromDeadStockModal: null, // { qty: '', targetWarehouseId: '', comment: '', submitting: false }
    allWarehouses: [],
  };

  handleExpandClick = (warehouseName) => {
    this.setState({
      expandedWarehouse: this.state.expandedWarehouse === warehouseName ? null : warehouseName,
    });
  };

  handleTabChange = (tab) => {
    this.setState({ activeTab: tab });
    if (tab === 'batches' && this.state.batches.length === 0) {
      this.loadBatches(this.props.data.productId, null);
    }
    if (tab === 'comments' && this.state.comments.length === 0) {
      this.loadComments();
    }
  };

  async loadComments() {
    this.setState({ commentsLoading: true });
    const { productId } = this.props.data;
    const response = await API.GET(apiEndpoints.getStockComments(productId));
    if (response.success) {
      this.setState({ comments: response.data || [], commentsLoading: false });
    } else {
      this.setState({ commentsLoading: false });
    }
  }

  async submitComment() {
    const { newComment } = this.state;
    if (!newComment.trim()) return;
    this.setState({ commentSubmitting: true });
    const { productId } = this.props.data;
    const response = await API.POST(apiEndpoints.addStockComment(productId), { comment: newComment.trim() });
    if (response.success) {
      this.setState({ comments: [response.data, ...this.state.comments], newComment: '', commentSubmitting: false });
    } else {
      this.setState({ commentSubmitting: false });
    }
  }

  async loadAllWarehouses() {
    if (this.state.allWarehouses.length > 0) return;
    const response = await API.GET(apiEndpoints.getWarehouse);
    if (response.success) {
      const list = Array.isArray(response.data) ? response.data : (response.data?.content || []);
      this.setState({ allWarehouses: list });
    } else {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar('Failed to load warehouses', { variant: 'error' });
      this.setState({ moveFromDeadStockModal: null });
    }
  }

  async openMarkDeadStockModal(warehouseId, warehouseName, availableQty) {
    const isBatchTracked = (this.props.data.batchMode || 'NONE') !== 'NONE';
    this.setState({ markDeadStockModal: { warehouseId, warehouseName, availableQty, qty: '', comment: '', submitting: false, isBatchTracked, sourceBatches: [], batchQtys: {}, batchesLoading: isBatchTracked } });
    if (isBatchTracked) {
      const { productId } = this.props.data;
      const res = await API.GET(apiEndpoints.getBatchesForProduct(productId, warehouseId));
      const batches = (res.success ? res.data || [] : []).filter(b => b.qtyRemaining > 0);
      this.setState(prev => ({ markDeadStockModal: prev.markDeadStockModal ? { ...prev.markDeadStockModal, sourceBatches: batches, batchesLoading: false } : null }));
    }
  }

  async openMoveFromDeadStockModal(availableQty) {
    this.loadAllWarehouses();
    const isBatchTracked = (this.props.data.batchMode || 'NONE') !== 'NONE';
    // Dead Stock Warehouse ID — find from detailedStock
    const deadStockRow = (this.props.data.detailedStock || []).find(
      i => (i.warehouseName || '').toLowerCase() === 'dead stock warehouse'
    );
    const deadStockWarehouseId = deadStockRow ? deadStockRow.warehouseId : null;
    this.setState({ moveFromDeadStockModal: { availableQty, qty: '', targetWarehouseId: '', comment: '', submitting: false, isBatchTracked, sourceBatches: [], batchQtys: {}, batchesLoading: isBatchTracked, deadStockWarehouseId } });
    if (isBatchTracked && deadStockWarehouseId) {
      const { productId } = this.props.data;
      const res = await API.GET(apiEndpoints.getBatchesForProduct(productId, deadStockWarehouseId));
      const batches = (res.success ? res.data || [] : []).filter(b => b.qtyRemaining > 0);
      this.setState(prev => ({ moveFromDeadStockModal: prev.moveFromDeadStockModal ? { ...prev.moveFromDeadStockModal, sourceBatches: batches, batchesLoading: false } : null }));
    }
  }

  async submitMarkDeadStock() {
    const { markDeadStockModal } = this.state;
    const { isBatchTracked, batchQtys, sourceBatches } = markDeadStockModal;

    let totalQty, overrideBatches;
    if (isBatchTracked) {
      overrideBatches = sourceBatches
        .map(b => ({ batchId: b.batchId, qty: parseFloat(batchQtys[b.batchId] || 0) }))
        .filter(e => e.qty > 0);
      totalQty = overrideBatches.reduce((s, e) => s + e.qty, 0);
      if (totalQty <= 0) {
        this.props.enqueueSnackbar && this.props.enqueueSnackbar('Allocate quantity to at least one batch', { variant: 'error' });
        return;
      }
      // validate no batch exceeds its available qty
      for (const e of overrideBatches) {
        const batch = sourceBatches.find(b => b.batchId === e.batchId);
        if (batch && e.qty > batch.qtyRemaining) {
          this.props.enqueueSnackbar && this.props.enqueueSnackbar(`Qty for batch ${batch.lotNumber || batch.batchId} exceeds available (${batch.qtyRemaining})`, { variant: 'error' });
          return;
        }
      }
    } else {
      totalQty = parseFloat(markDeadStockModal.qty);
      if (!markDeadStockModal.qty || totalQty <= 0) {
        this.props.enqueueSnackbar && this.props.enqueueSnackbar('Enter a valid quantity', { variant: 'error' });
        return;
      }
      if (markDeadStockModal.availableQty != null && totalQty > markDeadStockModal.availableQty) {
        this.props.enqueueSnackbar && this.props.enqueueSnackbar(`Quantity cannot exceed available stock (${markDeadStockModal.availableQty})`, { variant: 'error' });
        return;
      }
    }
    if (!markDeadStockModal.comment.trim()) {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar('Comment is mandatory', { variant: 'error' });
      return;
    }
    this.setState({ markDeadStockModal: { ...markDeadStockModal, submitting: true } });
    const { productId } = this.props.data;
    const payload = {
      sourceWarehouseId: markDeadStockModal.warehouseId,
      qty: totalQty,
      comment: markDeadStockModal.comment.trim(),
    };
    if (isBatchTracked && overrideBatches && overrideBatches.length > 0) {
      payload.overrideBatches = overrideBatches;
    }
    const response = await API.POST(apiEndpoints.markAsDeadStock(productId), payload);
    if (response.success) {
      this.setState({ markDeadStockModal: null });
      this.props.enqueueSnackbar && this.props.enqueueSnackbar('Stock marked as dead stock successfully', { variant: 'success' });
      this.props.reloadData && this.props.reloadData();
    } else {
      this.setState({ markDeadStockModal: { ...markDeadStockModal, submitting: false } });
      this.props.enqueueSnackbar && this.props.enqueueSnackbar(response.errorMessage || 'Failed to mark dead stock', { variant: 'error' });
    }
  }

  async submitMoveFromDeadStock() {
    const { moveFromDeadStockModal } = this.state;
    const { isBatchTracked, batchQtys, sourceBatches } = moveFromDeadStockModal;

    let totalQty, overrideBatches;
    if (isBatchTracked) {
      overrideBatches = sourceBatches
        .map(b => ({ batchId: b.batchId, qty: parseFloat(batchQtys[b.batchId] || 0) }))
        .filter(e => e.qty > 0);
      totalQty = overrideBatches.reduce((s, e) => s + e.qty, 0);
      if (totalQty <= 0) {
        this.props.enqueueSnackbar && this.props.enqueueSnackbar('Allocate quantity to at least one batch', { variant: 'error' });
        return;
      }
      for (const e of overrideBatches) {
        const batch = sourceBatches.find(b => b.batchId === e.batchId);
        if (batch && e.qty > batch.qtyRemaining) {
          this.props.enqueueSnackbar && this.props.enqueueSnackbar(`Qty for batch ${batch.lotNumber || batch.batchId} exceeds available (${batch.qtyRemaining})`, { variant: 'error' });
          return;
        }
      }
    } else {
      totalQty = parseFloat(moveFromDeadStockModal.qty);
      if (!moveFromDeadStockModal.qty || totalQty <= 0) {
        this.props.enqueueSnackbar && this.props.enqueueSnackbar('Enter a valid quantity', { variant: 'error' });
        return;
      }
      if (moveFromDeadStockModal.availableQty != null && totalQty > moveFromDeadStockModal.availableQty) {
        this.props.enqueueSnackbar && this.props.enqueueSnackbar(`Quantity cannot exceed available stock (${moveFromDeadStockModal.availableQty})`, { variant: 'error' });
        return;
      }
    }
    if (!moveFromDeadStockModal.targetWarehouseId) {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar('Select a target warehouse', { variant: 'error' });
      return;
    }
    if (!moveFromDeadStockModal.comment.trim()) {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar('Comment is mandatory', { variant: 'error' });
      return;
    }
    this.setState({ moveFromDeadStockModal: { ...moveFromDeadStockModal, submitting: true } });
    const { productId } = this.props.data;
    const payload = {
      targetWarehouseId: parseInt(moveFromDeadStockModal.targetWarehouseId),
      qty: totalQty,
      comment: moveFromDeadStockModal.comment.trim(),
    };
    if (isBatchTracked && overrideBatches && overrideBatches.length > 0) {
      payload.overrideBatches = overrideBatches;
    }
    const response = await API.POST(apiEndpoints.moveFromDeadStock(productId), payload);
    if (response.success) {
      this.setState({ moveFromDeadStockModal: null });
      this.props.enqueueSnackbar && this.props.enqueueSnackbar('Stock moved from dead stock successfully', { variant: 'success' });
      this.props.reloadData && this.props.reloadData();
    } else {
      this.setState({ moveFromDeadStockModal: { ...moveFromDeadStockModal, submitting: false } });
      this.props.enqueueSnackbar && this.props.enqueueSnackbar(response.errorMessage || 'Failed to move from dead stock', { variant: 'error' });
    }
  }

  async loadBatches(productId, warehouseId) {
    this.setState({ batchesLoading: true, batches: [], splitFormOpenWarehouseId: null, splitError: null });
    // warehouseId null = load all warehouses
    const response = await API.GET(apiEndpoints.getBatchesForProduct(productId, warehouseId || null));
    if (response.success) {
      const batches = response.data || [];
      this.setState({ batches, batchesLoading: false });
      batches.forEach(b => this.loadWriteOffHistory(b.batchId));
    } else {
      this.setState({ batchesLoading: false });
    }
  }

  getUntrackedQtyForWarehouse(warehouseId) {
    const { batches, stockAdjustments } = this.state;
    const data = this.props.data;
    const baseStock = (data.detailedStock || []).find(
      s => s.warehouseId === warehouseId
    )?.quantityInHand || 0;
    const adjustment = stockAdjustments[warehouseId] || 0;
    const warehouseStock = baseStock - adjustment;
    const trackedQty = batches
      .filter(b => b.warehouse?.warehouseId === warehouseId || b.warehouseId === warehouseId)
      .reduce((sum, b) => sum + (b.qtyRemaining || 0), 0);
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
      splitEntries: [...prev.splitEntries, { qty: '', expiryDate: '', brand: '', lotNumber: '' }],
      splitError: null,
    }));
  }

  removeSplitEntry(idx) {
    const entries = this.state.splitEntries.filter((_, i) => i !== idx);
    this.setState({ splitEntries: entries.length > 0 ? entries : [{ qty: '', expiryDate: '', brand: '', lotNumber: '' }], splitError: null });
  }

  fillRemaining(idx) {
    const untrackedQty = this.getUntrackedQtyForWarehouse(this.state.splitFormOpenWarehouseId);
    const otherSum = this.state.splitEntries.reduce((sum, e, i) => {
      if (i === idx) return sum;
      const v = parseFloat(e.qty);
      return sum + (isNaN(v) ? 0 : v);
    }, 0);
    const remaining = parseFloat((untrackedQty - otherSum).toFixed(4));
    if (remaining > 0) this.updateSplitEntry(idx, 'qty', String(remaining));
  }

  async submitSplit() {
    const warehouseId = this.state.splitFormOpenWarehouseId;
    const { splitEntries } = this.state;
    const data = this.props.data;
    const batchMode = data.batchMode || 'NONE';
    const requiresExpiry = batchMode === 'BATCH_WITH_EXPIRY';
    const untrackedQty = this.getUntrackedQtyForWarehouse(warehouseId);
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
      if (requiresExpiry && !e.expiryDate) {
        this.setState({ splitError: 'Expiry date is required for every batch.' });
        return;
      }
    }

    this.setState({ splitSubmitting: true, splitError: null });
    const payload = {
      warehouseId: warehouseId,
      batches: splitEntries.map(e => ({
        qty: parseFloat(e.qty),
        expiryDate: e.expiryDate ? e.expiryDate.split('-').reverse().join('-') : null,
        brand: e.brand || null,
        lotNumber: e.lotNumber || null,
      })),
    };
    const response = await API.POST(apiEndpoints.splitExistingStock(data.productId), payload);
    this.setState({ splitSubmitting: false });
    if (response.success) {
      this.setState({ splitFormOpenWarehouseId: null, splitEntries: [{ qty: '', expiryDate: '', brand: '', lotNumber: '' }] });
      this.loadBatches(data.productId, null);
    } else {
      this.setState({ splitError: response.errorMessage || 'Split failed. Please try again.' });
    }
  }

  renderSplitPanel(untrackedQty, warehouseId) {
    const { splitEntries, splitSubmitting, splitError } = this.state;
    const splitFormOpen = this.state.splitFormOpenWarehouseId === warehouseId;
    const batchMode = this.props.data.batchMode || 'NONE';
    const requiresExpiry = batchMode === 'BATCH_WITH_EXPIRY';
    const total = this.getSplitTotal();
    const remaining = parseFloat((untrackedQty - total).toFixed(4));
    const isExact = Math.abs(remaining) <= 0.001;
    const isOver = remaining < -0.001;

    const progressPct = Math.min((total / untrackedQty) * 100, 100);
    const progressColor = isOver ? '#c62828' : isExact ? '#2e7d32' : '#1565c0';

    if (!splitFormOpen) {
      return (
        <div style={{
          background: '#fff8e1', border: '1px solid #ffe082',
          padding: '10px 14px', marginBottom: '0',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px',
        }}>
          <div style={{ fontSize: '12px', color: '#5d4037' }}>
            <strong>⚠ {untrackedQty} units untracked</strong>
            <span style={{ marginLeft: '8px', color: '#795548' }}>
              {requiresExpiry
                ? 'Outward is blocked until all stock is split into batches with expiry dates.'
                : 'Outward is blocked until all stock is assigned to tracked batches.'}
            </span>
          </div>
          <button
            style={{
              padding: '5px 14px', fontSize: '12px', fontWeight: 600,
              background: '#e65100', color: 'white', border: 'none',
              borderRadius: '4px', cursor: 'pointer', whiteSpace: 'nowrap',
            }}
            onClick={() => this.setState({ splitFormOpenWarehouseId: warehouseId, splitError: null, splitEntries: [{ qty: '', expiryDate: '', brand: '', lotNumber: '' }] })}
          >
            Split Stock →
          </button>
        </div>
      );
    }

    const warehouseBatchesTracked = this.state.batches
      .filter(b => b.warehouse?.warehouseId === warehouseId || b.warehouseId === warehouseId)
      .reduce((s, b) => s + (b.qtyRemaining || 0), 0);

    return (
      <div style={{
        background: '#fff', border: '2px solid #1565c0',
        marginBottom: '0', overflow: 'hidden',
      }}>
        {/* Header */}
        <div style={{ background: '#1565c0', padding: '10px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ color: 'white', fontWeight: 600, fontSize: '13px' }}>
            Split Existing Stock — {this.props.data.productName}
          </div>
          <button
            style={{ background: 'transparent', border: 'none', color: 'white', cursor: 'pointer', fontSize: '16px', lineHeight: 1 }}
            onClick={() => this.setState({ splitFormOpenWarehouseId: null, splitError: null })}
          >×</button>
        </div>

        {/* Stats row */}
        <div style={{ display: 'flex', gap: '1px', background: '#e3f2fd', borderBottom: '1px solid #bbdefb' }}>
          {[
            { label: 'Total Stock', value: (untrackedQty + warehouseBatchesTracked).toFixed(2) },
            { label: 'Already in Batches', value: warehouseBatchesTracked.toFixed(2) },
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
                {requiresExpiry && (
                  <th style={{ padding: '5px 6px', border: '1px solid #ddd', width: '140px' }}>Expiry Date *</th>
                )}
                <th style={{ padding: '5px 6px', border: '1px solid #ddd' }}>Identifier (optional)</th>
                <th style={{ padding: '5px 6px', border: '1px solid #ddd' }}>Lot / Batch No. (optional)</th>
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
                      onWheel={(e) => e.target.blur()}
                      onChange={e => this.updateSplitEntry(idx, 'qty', e.target.value)}
                      placeholder="0"
                    />
                  </td>
                  {requiresExpiry && (
                    <td style={{ padding: '4px 6px', border: '1px solid #ddd' }}>
                      <input
                        type="date"
                        style={{ width: '100%', padding: '3px 5px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                        value={entry.expiryDate}
                        onChange={e => this.updateSplitEntry(idx, 'expiryDate', e.target.value)}
                      />
                    </td>
                  )}
                  <td style={{ padding: '4px 6px', border: '1px solid #ddd' }}>
                    <input
                      type="text"
                      style={{ width: '100%', padding: '3px 5px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                      value={entry.brand}
                      onChange={e => this.updateSplitEntry(idx, 'brand', e.target.value)}
                      placeholder="e.g. Brand, Type, Grade"
                    />
                  </td>
                  <td style={{ padding: '4px 6px', border: '1px solid #ddd' }}>
                    <input
                      type="text"
                      style={{ width: '100%', padding: '3px 5px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                      value={entry.lotNumber || ''}
                      onChange={e => this.updateSplitEntry(idx, 'lotNumber', e.target.value)}
                      placeholder="e.g. LOT-2024-01"
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
              onClick={() => this.setState({ splitFormOpenWarehouseId: null, splitError: null })}
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

  // Convert "dd-MM-yyyy" (from backend) → "yyyy-MM-dd" (for <input type="date">)
  ddmmyyyyToInputDate(ddmmyyyy) {
    if (!ddmmyyyy) return '';
    const parts = ddmmyyyy.split('-');
    if (parts.length !== 3) return '';
    return `${parts[2]}-${parts[1]}-${parts[0]}`;
  }

  async submitEditBatch(batchId) {
    const { editBatchForm } = this.state;
    if (!editBatchForm || !editBatchForm.receivedDate) {
      alert('Received date is required.');
      return;
    }
    this.setState({ editBatchForm: { ...editBatchForm, submitting: true } });

    // Convert yyyy-MM-dd → dd-MM-yyyy for backend
    const toBackendDate = (isoDate) => isoDate ? isoDate.split('-').reverse().join('-') : null;

    const payload = {
      brand: editBatchForm.brand || null,
      lotNumber: editBatchForm.lotNumber || null,
      expiryDate: editBatchForm.expiryDate ? toBackendDate(editBatchForm.expiryDate) : null,
      receivedDate: toBackendDate(editBatchForm.receivedDate),
    };

    const response = await API.PUT(apiEndpoints.updateBatch(batchId), payload);
    if (response.success) {
      this.setState({ editBatchForm: null });
      this.loadBatches(this.props.data.productId, null);
    } else {
      this.setState({ editBatchForm: { ...editBatchForm, submitting: false } });
      alert(response.errorMessage || 'Update failed. Please try again.');
    }
  }

  async submitWriteOff(batchId) {
    const { writeOffForm } = this.state;
    if (!writeOffForm || !writeOffForm.quantity || !writeOffForm.reason) {
      alert("Quantity and reason are required.");
      return;
    }
    this.setState({ writeOffSubmitting: true });
    const response = await API.POST(apiEndpoints.writeOffBatch(batchId), {
      quantity: parseFloat(writeOffForm.quantity),
      reason: writeOffForm.reason,
    });
    this.setState({ writeOffSubmitting: false });
    if (response.success) {
      const { stockAdjustments } = this.state;
      const warehouseId = writeOffForm.warehouseId;
      const writtenQty = parseFloat(writeOffForm.quantity);
      this.setState({
        writeOffForm: null,
        stockAdjustments: {
          ...stockAdjustments,
          [warehouseId]: (stockAdjustments[warehouseId] || 0) + writtenQty,
        },
      });
      const productId = this.props.data.productId;
      this.loadBatches(productId, null);
      this.loadWriteOffHistory(batchId);
    } else {
      alert(response.errorMessage || "Write-off failed.");
    }
  }

  async loadWriteOffHistory(batchId) {
    this.setState(prev => ({ writeOffHistoryLoading: { ...prev.writeOffHistoryLoading, [batchId]: true } }));
    const response = await API.GET(apiEndpoints.getBatchWriteOffHistory(batchId));
    this.setState(prev => ({
      writeOffHistoryLoading: { ...prev.writeOffHistoryLoading, [batchId]: false },
      writeOffHistories: { ...prev.writeOffHistories, [batchId]: response.success ? (response.data || []) : [] },
    }));
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
          {data.batchMode && data.batchMode !== 'NONE' && (
            <button
              className={`tab-button ${this.state.activeTab === 'batches' ? 'active' : ''}`}
              onClick={() => this.handleTabChange('batches')}
            >
              Batches
            </button>
          )}
          <button
            className={`tab-button ${this.state.activeTab === 'comments' ? 'active' : ''}`}
            onClick={() => this.handleTabChange('comments')}
          >
            Comments
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
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.detailedStock.map((item) => {
                    const isDeadStockWarehouse = (item.warehouseName || '').toLowerCase() === 'dead stock warehouse';
                    return (
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
                        <td>
                          {!isDeadStockWarehouse && item.quantityInHand > 0 && (
                            <button
                              style={{ padding: '3px 8px', fontSize: '11px', backgroundColor: '#b71c1c', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}
                              onClick={() => this.openMarkDeadStockModal(item.warehouseId, item.warehouseName, item.quantityInHand)}
                            >
                              Mark Dead Stock
                            </button>
                          )}
                          {isDeadStockWarehouse && item.quantityInHand > 0 && (
                            <button
                              style={{ padding: '3px 8px', fontSize: '11px', backgroundColor: '#1565c0', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}
                              onClick={() => this.openMoveFromDeadStockModal(item.quantityInHand)}
                            >
                              Move from Dead Stock
                            </button>
                          )}
                        </td>
                      </tr>
                      {this.state.expandedWarehouse === item.warehouseName && item.stockAgingData && (
                        <tr>
                          <td colSpan="4">
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
                  ); })}
                </tbody>
              </table>
            </div>
          )}
          {this.state.activeTab === 'history' && (
            <div className="history-content">
              <div style={{ margin: '8px 0 10px', padding: '6px 10px', backgroundColor: '#fff8e1', border: '1px solid #ffe082', borderRadius: '4px', fontSize: '12px', color: '#795548' }}>
                ⚠ Data refreshed every 30 minutes. Recent transactions may not appear immediately.
              </div>
              <table>
                <thead>
                  <tr>
                    <th>Warehouse</th>
                    <th>Type</th>
                    <th>Date</th>
                    <th>Quantity</th>
                    <th>Closing Stock</th>
                  </tr>
                </thead>
                <tbody>
                  {data.inwardOutwardHistory.map((item, index) => {
                    const isOutward = ['OUTWARD', 'LOST-DAMAGED', 'WRITE-OFF', 'TRANSFER-OUT'].includes((item.type || '').toUpperCase());
                    return (
                      <tr key={index}>
                        <td>{item.warehouseName}</td>
                        <td style={{ color: isOutward ? '#c62828' : '#2e7d32', fontWeight: 500 }}>{item.type}</td>
                        <td>{item.date}</td>
                        <td style={{ color: isOutward ? '#c62828' : '#2e7d32' }}>{isOutward ? '-' : '+'}{item.quantity}</td>
                        <td>{item.closingStock != null ? item.closingStock : '—'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {this.state.activeTab === 'batches' && (
            <div style={{ padding: '8px' }}>
              {this.state.batchesLoading && (
                <div style={{ padding: '20px', textAlign: 'center', color: '#888', fontSize: '13px' }}>Loading batches…</div>
              )}

              {!this.state.batchesLoading && (() => {
                const batchMode = data.batchMode || 'NONE';
                const requiresExpiry = batchMode === 'BATCH_WITH_EXPIRY';

                // Top banner — list all warehouses with untracked stock
                const untrackedWarehouses = (data.detailedStock || []).filter(
                  w => this.getUntrackedQtyForWarehouse(w.warehouseId) > 0.001
                );

                return (
                  <>
                    {untrackedWarehouses.length > 0 && (
                      <div style={{
                        background: '#fff8e1', border: '1px solid #ffe082', borderRadius: '6px',
                        padding: '8px 12px', marginBottom: '12px', fontSize: '12px', color: '#5d4037',
                      }}>
                        <strong>⚠ Untracked goods present in: </strong>
                        {untrackedWarehouses.map(w => w.warehouseName).join(', ')}
                      </div>
                    )}

                    {/* Per-warehouse sections */}
                    <div style={{ maxHeight: '70vh', overflowY: 'auto', paddingRight: '2px' }}>
                      {(data.detailedStock || []).map(warehouse => {
                        const warehouseId = warehouse.warehouseId;
                        const untrackedQty = this.getUntrackedQtyForWarehouse(warehouseId);
                        const warehouseBatches = [...this.state.batches]
                          .filter(b => b.qtyRemaining > 0)
                          .filter(b => b.warehouse?.warehouseId === warehouseId || b.warehouseId === warehouseId)
                          .sort((a, b) => {
                            if (requiresExpiry) {
                              if (!a.expiryDate && !b.expiryDate) return 0;
                              if (!a.expiryDate) return 1;
                              if (!b.expiryDate) return -1;
                              return a.expiryDate.localeCompare(b.expiryDate);
                            } else {
                              if (!a.receivedDate && !b.receivedDate) return 0;
                              if (!a.receivedDate) return 1;
                              if (!b.receivedDate) return -1;
                              return a.receivedDate.localeCompare(b.receivedDate);
                            }
                          });

                        return (
                          <div key={warehouseId} style={{
                            marginBottom: '16px', border: '1px solid #e0e0e0',
                            borderRadius: '6px', overflow: 'hidden',
                          }}>
                            {/* Warehouse header */}
                            <div style={{
                              background: '#f5f5f5', padding: '8px 12px',
                              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                              borderBottom: '1px solid #e0e0e0',
                            }}>
                              <span style={{ fontWeight: 600, fontSize: '13px' }}>📦 {warehouse.warehouseName}</span>
                              <span style={{ color: '#666', fontSize: '12px' }}>
                                Stock: <strong>{warehouse.quantityInHand}</strong> {data.measurementUnit || ''}
                                {untrackedQty > 0.001 && (
                                  <span style={{ marginLeft: '8px', color: '#e65100', fontWeight: 600 }}>
                                    ({untrackedQty} untracked)
                                  </span>
                                )}
                              </span>
                            </div>

                            {/* Split panel (if untracked) */}
                            {batchMode !== 'NONE' && untrackedQty > 0.001 && this.renderSplitPanel(untrackedQty, warehouseId)}

                            {/* Batch table */}
                            {warehouseBatches.length === 0 ? (
                              <div style={{ padding: '12px', color: '#aaa', fontSize: '12px', textAlign: 'center' }}>
                                No tracked batches for this warehouse.
                              </div>
                            ) : (
                              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                                <thead>
                                  <tr style={{ backgroundColor: '#fafafa' }}>
                                    <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Batch</th>
                                    <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Identifier</th>
                                    <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Lot / Batch No.</th>
                                    <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Received</th>
                                    {requiresExpiry && <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Expiry</th>}
                                    {requiresExpiry && <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'center' }}>Status</th>}
                                    <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'right' }}>Qty Remaining</th>
                                    <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'center' }}>Action</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {warehouseBatches.map((batch) => {
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
                                              <span
                                                title="Batch created from untracked stock split — not linked to a specific inward receipt"
                                                style={{ marginLeft: '4px', fontSize: '10px', background: '#e3f2fd', color: '#1565c0', borderRadius: '3px', padding: '1px 4px', cursor: 'default' }}
                                              >split</span>
                                            )}
                                          </td>
                                          <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.brand || <span style={{ color: '#bbb', fontSize: '11px' }}>—</span>}</td>
                                          <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.lotNumber || <span style={{ color: '#bbb' }}>—</span>}</td>
                                          <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.receivedDate ? batch.receivedDate.replace(/-/g, '/') : '—'}</td>
                                          {requiresExpiry && (
                                            <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.expiryDate ? batch.expiryDate.replace(/-/g, '/') : <span style={{ color: '#bbb' }}>—</span>}</td>
                                          )}
                                          {requiresExpiry && (
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
                                          )}
                                          <td style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'right', fontWeight: 600 }}>{batch.qtyRemaining}</td>
                                          <td style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'center' }}>
                                            <button
                                              style={{ padding: '3px 8px', fontSize: '11px', cursor: 'pointer', backgroundColor: '#1565c0', color: 'white', border: 'none', borderRadius: '3px', marginRight: '4px' }}
                                              onClick={() => this.setState({
                                                editBatchForm: {
                                                  batchId: batch.batchId,
                                                  brand: batch.brand || '',
                                                  lotNumber: batch.lotNumber || '',
                                                  expiryDate: this.ddmmyyyyToInputDate(batch.expiryDate),
                                                  receivedDate: this.ddmmyyyyToInputDate(batch.receivedDate),
                                                  submitting: false,
                                                },
                                                writeOffForm: null,
                                              })}
                                            >
                                              Edit
                                            </button>
                                            {batch.qtyRemaining > 0 && (
                                              <button
                                                style={{ padding: '3px 8px', fontSize: '11px', cursor: 'pointer', backgroundColor: '#e53935', color: 'white', border: 'none', borderRadius: '3px' }}
                                                onClick={() => this.setState({ writeOffForm: { batchId: batch.batchId, warehouseId: batch.warehouse?.warehouseId, quantity: '', reason: '' }, editBatchForm: null })}
                                              >
                                                Write Off
                                              </button>
                                            )}
                                          </td>
                                        </tr>
                                        {this.state.editBatchForm && this.state.editBatchForm.batchId === batch.batchId && (() => {
                                          const ef = this.state.editBatchForm;
                                          return (
                                            <tr>
                                              <td colSpan={requiresExpiry ? 8 : 6} style={{ padding: '10px 12px', backgroundColor: '#e8f4fd', border: '1px solid #90caf9' }}>
                                                <div style={{ fontSize: '12px', fontWeight: 600, marginBottom: '8px', color: '#1565c0' }}>
                                                  Edit Batch #{batch.batchId}
                                                  <span style={{ fontWeight: 400, color: '#555', marginLeft: '8px', fontSize: '11px' }}>
                                                    Note: changing Received / Expiry date affects future FIFO/FEFO order
                                                  </span>
                                                </div>
                                                <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                                                  <div>
                                                    <div style={{ fontSize: '11px', marginBottom: '2px' }}>Identifier (Brand)</div>
                                                    <input type="text"
                                                      style={{ padding: '4px 6px', width: '130px', border: '1px solid #90caf9', borderRadius: '3px', fontSize: '12px' }}
                                                      value={ef.brand}
                                                      onChange={e => this.setState({ editBatchForm: { ...ef, brand: e.target.value } })}
                                                      placeholder="e.g. Brand / Grade"
                                                    />
                                                  </div>
                                                  <div>
                                                    <div style={{ fontSize: '11px', marginBottom: '2px' }}>Lot / Batch No.</div>
                                                    <input type="text"
                                                      style={{ padding: '4px 6px', width: '130px', border: '1px solid #90caf9', borderRadius: '3px', fontSize: '12px' }}
                                                      value={ef.lotNumber}
                                                      onChange={e => this.setState({ editBatchForm: { ...ef, lotNumber: e.target.value } })}
                                                      placeholder="e.g. LOT-2024-01"
                                                    />
                                                  </div>
                                                  <div>
                                                    <div style={{ fontSize: '11px', marginBottom: '2px' }}>Received Date *</div>
                                                    <input type="date"
                                                      style={{ padding: '4px 6px', border: '1px solid #90caf9', borderRadius: '3px', fontSize: '12px' }}
                                                      value={ef.receivedDate}
                                                      onChange={e => this.setState({ editBatchForm: { ...ef, receivedDate: e.target.value } })}
                                                    />
                                                  </div>
                                                  {requiresExpiry && (
                                                    <div>
                                                      <div style={{ fontSize: '11px', marginBottom: '2px' }}>Expiry Date</div>
                                                      <input type="date"
                                                        style={{ padding: '4px 6px', border: '1px solid #90caf9', borderRadius: '3px', fontSize: '12px' }}
                                                        value={ef.expiryDate}
                                                        onChange={e => this.setState({ editBatchForm: { ...ef, expiryDate: e.target.value } })}
                                                      />
                                                    </div>
                                                  )}
                                                  <button
                                                    style={{ padding: '5px 14px', fontSize: '12px', fontWeight: 600, backgroundColor: '#1565c0', color: 'white', border: 'none', borderRadius: '3px', cursor: ef.submitting ? 'not-allowed' : 'pointer', opacity: ef.submitting ? 0.7 : 1 }}
                                                    disabled={ef.submitting}
                                                    onClick={() => this.submitEditBatch(batch.batchId)}
                                                  >
                                                    {ef.submitting ? 'Saving…' : 'Save'}
                                                  </button>
                                                  <button
                                                    style={{ padding: '5px 12px', fontSize: '12px', backgroundColor: '#757575', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}
                                                    onClick={() => this.setState({ editBatchForm: null })}
                                                  >
                                                    Cancel
                                                  </button>
                                                </div>
                                              </td>
                                            </tr>
                                          );
                                        })()}
                                        {this.state.writeOffForm && this.state.writeOffForm.batchId === batch.batchId && (
                                          <tr>
                                            <td colSpan={requiresExpiry ? 8 : 6} style={{ padding: '8px', backgroundColor: '#fafafa', border: '1px solid #ddd' }}>
                                              <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                                                <div>
                                                  <div style={{ fontSize: '11px', marginBottom: '2px' }}>Quantity *</div>
                                                  <input type="number" style={{ padding: '4px', width: '80px', border: '1px solid #ccc', borderRadius: '3px' }}
                                                    value={this.state.writeOffForm.quantity}
                                                    onWheel={(e) => e.target.blur()}
                                                    onChange={e => this.setState({ writeOffForm: { ...this.state.writeOffForm, quantity: e.target.value } })}
                                                    max={batch.qtyRemaining} min="0" step="any"
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
                                        {/* Write-off history for this batch */}
                                        {this.state.writeOffHistories[batch.batchId] && this.state.writeOffHistories[batch.batchId].length > 0 && (
                                          <tr>
                                            <td colSpan={requiresExpiry ? 8 : 6} style={{ padding: '6px 8px', backgroundColor: '#fff8e1', border: '1px solid #ffe082' }}>
                                              <div style={{ fontSize: '11px', fontWeight: 600, marginBottom: '4px', color: '#e65100' }}>Write-off History</div>
                                              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '11px' }}>
                                                <thead>
                                                  <tr style={{ backgroundColor: '#fff3e0' }}>
                                                    <th style={{ padding: '3px 6px', border: '1px solid #ffe082', textAlign: 'left' }}>Date</th>
                                                    <th style={{ padding: '3px 6px', border: '1px solid #ffe082', textAlign: 'left' }}>Qty</th>
                                                    <th style={{ padding: '3px 6px', border: '1px solid #ffe082', textAlign: 'left' }}>Reason</th>
                                                    <th style={{ padding: '3px 6px', border: '1px solid #ffe082', textAlign: 'left' }}>By</th>
                                                  </tr>
                                                </thead>
                                                <tbody>
                                                  {this.state.writeOffHistories[batch.batchId].map((wo, i) => (
                                                    <tr key={i}>
                                                      <td style={{ padding: '3px 6px', border: '1px solid #ffe082' }}>{wo.writeOffDate ? wo.writeOffDate.replace(/-/g, '/') : '—'}</td>
                                                      <td style={{ padding: '3px 6px', border: '1px solid #ffe082' }}>{wo.quantity}</td>
                                                      <td style={{ padding: '3px 6px', border: '1px solid #ffe082' }}>{wo.reason}</td>
                                                      <td style={{ padding: '3px 6px', border: '1px solid #ffe082' }}>{wo.writtenOffBy || '—'}</td>
                                                    </tr>
                                                  ))}
                                                </tbody>
                                              </table>
                                            </td>
                                          </tr>
                                        )}
                                      </React.Fragment>
                                    );
                                  })}
                                </tbody>
                              </table>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </>
                );
              })()}
            </div>
          )}

          {/* ── Comments Tab ── */}
          {this.state.activeTab === 'comments' && (
            <div style={{ padding: '12px' }}>
              {/* Add comment box */}
              <div style={{ marginBottom: '16px', padding: '12px', border: '1px solid #e0e0e0', borderRadius: '4px', backgroundColor: '#fafafa' }}>
                <div style={{ fontSize: '12px', fontWeight: 600, marginBottom: '6px', color: '#333' }}>Add Comment</div>
                <textarea
                  rows={3}
                  style={{ width: '100%', padding: '8px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', resize: 'vertical', boxSizing: 'border-box' }}
                  placeholder="Write a comment..."
                  value={this.state.newComment}
                  onChange={e => this.setState({ newComment: e.target.value })}
                />
                <button
                  style={{ marginTop: '6px', padding: '5px 14px', backgroundColor: '#1565c0', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer', fontSize: '12px' }}
                  disabled={this.state.commentSubmitting || !this.state.newComment.trim()}
                  onClick={() => this.submitComment()}
                >
                  {this.state.commentSubmitting ? 'Saving…' : 'Post Comment'}
                </button>
              </div>
              {this.state.commentsLoading && (
                <div style={{ textAlign: 'center', color: '#888', fontSize: '13px', padding: '16px' }}>Loading comments…</div>
              )}
              {!this.state.commentsLoading && this.state.comments.length === 0 && (
                <div style={{ textAlign: 'center', color: '#aaa', fontSize: '13px', padding: '16px' }}>No comments yet.</div>
              )}
              {this.state.comments.map((c, idx) => {
                const typeColors = {
                  DEAD_STOCK_IN: { bg: '#fce4ec', border: '#f48fb1', badge: '#c62828', label: 'Dead Stock In' },
                  DEAD_STOCK_OUT: { bg: '#e8f5e9', border: '#a5d6a7', badge: '#2e7d32', label: 'Dead Stock Out' },
                  TRANSFER_AUTO: { bg: '#e3f2fd', border: '#90caf9', badge: '#1565c0', label: 'Transfer' },
                  MANUAL: { bg: '#f5f5f5', border: '#e0e0e0', badge: '#616161', label: 'Comment' },
                };
                const style = typeColors[c.commentType] || typeColors.MANUAL;
                return (
                  <div key={idx} style={{ marginBottom: '8px', padding: '10px 12px', backgroundColor: style.bg, border: `1px solid ${style.border}`, borderRadius: '4px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                      <span style={{ fontSize: '11px', fontWeight: 600, color: style.badge, padding: '1px 6px', backgroundColor: 'white', border: `1px solid ${style.border}`, borderRadius: '10px' }}>
                        {style.label}
                      </span>
                      <span style={{ fontSize: '11px', color: '#777' }}>{c.createdBy} · {c.createdAt}</span>
                    </div>
                    <div style={{ fontSize: '12px', color: '#333' }}>{c.comment}</div>
                    {c.linkedTransferId && (
                      <div style={{ fontSize: '10px', color: '#999', marginTop: '3px' }}>Transfer #{c.linkedTransferId}</div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* ── Mark as Dead Stock Modal ── */}
        {this.state.markDeadStockModal && (() => {
          const m = this.state.markDeadStockModal;
          const totalAllocated = m.isBatchTracked
            ? (m.sourceBatches || []).reduce((s, b) => s + parseFloat(m.batchQtys[b.batchId] || 0), 0)
            : 0;
          const confirmDisabled = m.submitting || !m.comment || !m.comment.trim() ||
            (m.isBatchTracked ? totalAllocated <= 0 : (!m.qty || parseFloat(m.qty) <= 0));
          return (
            <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <div style={{ backgroundColor: 'white', borderRadius: '6px', padding: '20px', width: m.isBatchTracked ? '520px' : '400px', maxWidth: '95vw', maxHeight: '90vh', overflowY: 'auto' }}>
                <h3 style={{ margin: '0 0 4px', fontSize: '15px', color: '#b71c1c' }}>Mark as Dead Stock</h3>
                <p style={{ margin: '0 0 14px', fontSize: '12px', color: '#777' }}>From: <strong>{m.warehouseName}</strong></p>

                {m.isBatchTracked ? (
                  <div style={{ marginBottom: '12px' }}>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '6px' }}>
                      Batch Allocation * <span style={{ color: '#888', fontWeight: 400 }}>(enter qty to move from each batch)</span>
                    </label>
                    {m.batchesLoading ? (
                      <div style={{ padding: '12px', textAlign: 'center', color: '#888', fontSize: '12px' }}>Loading batches…</div>
                    ) : m.sourceBatches.length === 0 ? (
                      <div style={{ padding: '10px', backgroundColor: '#fff3e0', borderRadius: '4px', fontSize: '12px', color: '#e65100' }}>
                        No active batches found in this warehouse.
                      </div>
                    ) : (
                      <>
                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '11px' }}>
                          <thead>
                            <tr style={{ backgroundColor: '#f5f5f5' }}>
                              <th style={{ padding: '5px 6px', textAlign: 'left', border: '1px solid #e0e0e0' }}>Lot #</th>
                              <th style={{ padding: '5px 6px', textAlign: 'left', border: '1px solid #e0e0e0' }}>Brand</th>
                              <th style={{ padding: '5px 6px', textAlign: 'right', border: '1px solid #e0e0e0' }}>Available</th>
                              <th style={{ padding: '5px 6px', textAlign: 'right', border: '1px solid #e0e0e0', width: '90px' }}>Qty to Move</th>
                            </tr>
                          </thead>
                          <tbody>
                            {m.sourceBatches.map(b => (
                              <tr key={b.batchId}>
                                <td style={{ padding: '4px 6px', border: '1px solid #e0e0e0' }}>{b.lotNumber || '—'}</td>
                                <td style={{ padding: '4px 6px', border: '1px solid #e0e0e0' }}>{b.brand || '—'}</td>
                                <td style={{ padding: '4px 6px', border: '1px solid #e0e0e0', textAlign: 'right' }}>{b.qtyRemaining}</td>
                                <td style={{ padding: '4px 6px', border: '1px solid #e0e0e0' }}>
                                  <input
                                    type="number" min="0" step="any" max={b.qtyRemaining}
                                    style={{ width: '100%', padding: '3px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '11px', textAlign: 'right', boxSizing: 'border-box' }}
                                    value={m.batchQtys[b.batchId] || ''}
                                    onWheel={e => e.target.blur()}
                                    onChange={e => {
                                      const val = e.target.value;
                                      if (val !== '' && parseFloat(val) > b.qtyRemaining) return;
                                      this.setState({ markDeadStockModal: { ...m, batchQtys: { ...m.batchQtys, [b.batchId]: val } } });
                                    }}
                                  />
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        <div style={{ marginTop: '6px', fontSize: '12px', color: totalAllocated > 0 ? '#2e7d32' : '#888', textAlign: 'right', fontWeight: 600 }}>
                          Total allocated: {totalAllocated} units
                        </div>
                      </>
                    )}
                  </div>
                ) : (
                  <div style={{ marginBottom: '10px' }}>
                    <label style={{ display: 'block', fontSize: '12px', marginBottom: '3px' }}>
                      Quantity * <span style={{ color: '#888', fontWeight: 400 }}>(max: {m.availableQty})</span>
                    </label>
                    <input
                      type="number" min="0" step="any" max={m.availableQty}
                      style={{ width: '100%', padding: '6px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                      value={m.qty}
                      onWheel={e => e.target.blur()}
                      onChange={e => {
                        const val = e.target.value;
                        if (val !== '' && m.availableQty != null && parseFloat(val) > m.availableQty) return;
                        this.setState({ markDeadStockModal: { ...m, qty: val } });
                      }}
                    />
                  </div>
                )}

                <div style={{ marginBottom: '14px' }}>
                  <label style={{ display: 'block', fontSize: '12px', marginBottom: '3px' }}>Reason / Comment * <span style={{ color: '#999', fontWeight: 400 }}>(mandatory)</span></label>
                  <textarea
                    rows={3}
                    style={{ width: '100%', padding: '6px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', resize: 'vertical', boxSizing: 'border-box' }}
                    placeholder="Why is this stock being marked as dead?"
                    value={m.comment}
                    onChange={e => this.setState({ markDeadStockModal: { ...m, comment: e.target.value } })}
                  />
                </div>
                <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                  <button
                    style={{ padding: '6px 16px', backgroundColor: '#757575', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer', fontSize: '12px' }}
                    onClick={() => this.setState({ markDeadStockModal: null })}
                    disabled={m.submitting}
                  >
                    Cancel
                  </button>
                  <button
                    style={{ padding: '6px 16px', backgroundColor: '#b71c1c', color: 'white', border: 'none', borderRadius: '3px', fontSize: '12px',
                      opacity: confirmDisabled ? 0.5 : 1,
                      cursor: confirmDisabled ? 'not-allowed' : 'pointer' }}
                    onClick={() => this.submitMarkDeadStock()}
                    disabled={confirmDisabled}
                  >
                    {m.submitting ? 'Processing…' : 'Confirm'}
                  </button>
                </div>
              </div>
            </div>
          );
        })()}

        {/* ── Move from Dead Stock Modal ── */}
        {this.state.moveFromDeadStockModal && (() => {
          const m = this.state.moveFromDeadStockModal;
          const nonDeadWarehouses = this.state.allWarehouses.filter(w => (w.name || '').toLowerCase() !== 'dead stock warehouse');
          const totalAllocated = m.isBatchTracked
            ? (m.sourceBatches || []).reduce((s, b) => s + parseFloat(m.batchQtys[b.batchId] || 0), 0)
            : 0;
          const confirmDisabled = m.submitting || !m.targetWarehouseId || !m.comment || !m.comment.trim() ||
            (m.isBatchTracked ? totalAllocated <= 0 : (!m.qty || parseFloat(m.qty) <= 0));
          return (
            <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <div style={{ backgroundColor: 'white', borderRadius: '6px', padding: '20px', width: m.isBatchTracked ? '520px' : '400px', maxWidth: '95vw', maxHeight: '90vh', overflowY: 'auto' }}>
                <h3 style={{ margin: '0 0 14px', fontSize: '15px', color: '#1565c0' }}>Move from Dead Stock</h3>
                <div style={{ marginBottom: '10px' }}>
                  <label style={{ display: 'block', fontSize: '12px', marginBottom: '3px' }}>Target Warehouse *</label>
                  <select
                    style={{ width: '100%', padding: '6px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                    value={m.targetWarehouseId}
                    onChange={e => this.setState({ moveFromDeadStockModal: { ...m, targetWarehouseId: e.target.value } })}
                  >
                    <option value="">Select warehouse</option>
                    {nonDeadWarehouses.map(w => (
                      <option key={w.id} value={w.id}>{w.name}</option>
                    ))}
                  </select>
                </div>

                {m.isBatchTracked ? (
                  <div style={{ marginBottom: '12px' }}>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '6px' }}>
                      Batch Allocation * <span style={{ color: '#888', fontWeight: 400 }}>(enter qty to move from each batch)</span>
                    </label>
                    {m.batchesLoading ? (
                      <div style={{ padding: '12px', textAlign: 'center', color: '#888', fontSize: '12px' }}>Loading batches…</div>
                    ) : m.sourceBatches.length === 0 ? (
                      <div style={{ padding: '10px', backgroundColor: '#fff3e0', borderRadius: '4px', fontSize: '12px', color: '#e65100' }}>
                        No active batches found in Dead Stock Warehouse.
                      </div>
                    ) : (
                      <>
                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '11px' }}>
                          <thead>
                            <tr style={{ backgroundColor: '#f5f5f5' }}>
                              <th style={{ padding: '5px 6px', textAlign: 'left', border: '1px solid #e0e0e0' }}>Lot #</th>
                              <th style={{ padding: '5px 6px', textAlign: 'left', border: '1px solid #e0e0e0' }}>Brand</th>
                              <th style={{ padding: '5px 6px', textAlign: 'right', border: '1px solid #e0e0e0' }}>Available</th>
                              <th style={{ padding: '5px 6px', textAlign: 'right', border: '1px solid #e0e0e0', width: '90px' }}>Qty to Move</th>
                            </tr>
                          </thead>
                          <tbody>
                            {m.sourceBatches.map(b => (
                              <tr key={b.batchId}>
                                <td style={{ padding: '4px 6px', border: '1px solid #e0e0e0' }}>{b.lotNumber || '—'}</td>
                                <td style={{ padding: '4px 6px', border: '1px solid #e0e0e0' }}>{b.brand || '—'}</td>
                                <td style={{ padding: '4px 6px', border: '1px solid #e0e0e0', textAlign: 'right' }}>{b.qtyRemaining}</td>
                                <td style={{ padding: '4px 6px', border: '1px solid #e0e0e0' }}>
                                  <input
                                    type="number" min="0" step="any" max={b.qtyRemaining}
                                    style={{ width: '100%', padding: '3px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '11px', textAlign: 'right', boxSizing: 'border-box' }}
                                    value={m.batchQtys[b.batchId] || ''}
                                    onWheel={e => e.target.blur()}
                                    onChange={e => {
                                      const val = e.target.value;
                                      if (val !== '' && parseFloat(val) > b.qtyRemaining) return;
                                      this.setState({ moveFromDeadStockModal: { ...m, batchQtys: { ...m.batchQtys, [b.batchId]: val } } });
                                    }}
                                  />
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        <div style={{ marginTop: '6px', fontSize: '12px', color: totalAllocated > 0 ? '#2e7d32' : '#888', textAlign: 'right', fontWeight: 600 }}>
                          Total allocated: {totalAllocated} units
                        </div>
                      </>
                    )}
                  </div>
                ) : (
                  <div style={{ marginBottom: '10px' }}>
                    <label style={{ display: 'block', fontSize: '12px', marginBottom: '3px' }}>
                      Quantity * <span style={{ color: '#888', fontWeight: 400 }}>(max: {m.availableQty})</span>
                    </label>
                    <input
                      type="number" min="0" step="any" max={m.availableQty}
                      style={{ width: '100%', padding: '6px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', boxSizing: 'border-box' }}
                      value={m.qty}
                      onWheel={e => e.target.blur()}
                      onChange={e => {
                        const val = e.target.value;
                        if (val !== '' && m.availableQty != null && parseFloat(val) > m.availableQty) return;
                        this.setState({ moveFromDeadStockModal: { ...m, qty: val } });
                      }}
                    />
                  </div>
                )}

                <div style={{ marginBottom: '14px' }}>
                  <label style={{ display: 'block', fontSize: '12px', marginBottom: '3px' }}>Reason / Comment * <span style={{ color: '#999', fontWeight: 400 }}>(mandatory)</span></label>
                  <textarea
                    rows={3}
                    style={{ width: '100%', padding: '6px', border: '1px solid #ccc', borderRadius: '3px', fontSize: '12px', resize: 'vertical', boxSizing: 'border-box' }}
                    placeholder="Why is this stock being moved?"
                    value={m.comment}
                    onChange={e => this.setState({ moveFromDeadStockModal: { ...m, comment: e.target.value } })}
                  />
                </div>
                <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                  <button
                    style={{ padding: '6px 16px', backgroundColor: '#757575', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer', fontSize: '12px' }}
                    onClick={() => this.setState({ moveFromDeadStockModal: null })}
                    disabled={m.submitting}
                  >
                    Cancel
                  </button>
                  <button
                    style={{ padding: '6px 16px', backgroundColor: '#1565c0', color: 'white', border: 'none', borderRadius: '3px', fontSize: '12px',
                      opacity: confirmDisabled ? 0.5 : 1,
                      cursor: confirmDisabled ? 'not-allowed' : 'pointer' }}
                    onClick={() => this.submitMoveFromDeadStock()}
                    disabled={confirmDisabled}
                  >
                    {m.submitting ? 'Processing…' : 'Move Stock'}
                  </button>
                </div>
              </div>
            </div>
          );
        })()}
      </div>
    );
  }
}

export default withSnackbar(Details);
