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
    writeOffForm: null, // { batchId, quantity, reason, writeOffDate }
    writeOffSubmitting: false,
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
    this.setState({ batchesLoading: true, batches: [] });
    const response = await API.GET(apiEndpoints.getBatchesForProduct(productId, warehouseId));
    if (response.success) {
      this.setState({ batches: response.data || [], batchesLoading: false });
    } else {
      this.setState({ batchesLoading: false });
    }
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
                    this.setState({ selectedWarehouseId: warehouseId, batches: [], writeOffForm: null });
                    if (warehouseId) {
                      this.loadBatches(data.productId, warehouseId);
                    }
                  }}
                >
                  <option value="">Select warehouse...</option>
                  {data.detailedStock.map((item, idx) => (
                    <option key={idx} value={item.warehouseId}>
                      {item.warehouseName || 'Unknown'}
                    </option>
                  ))}
                </select>
              </div>

              {this.state.batchesLoading && <div>Loading batches...</div>}

              {!this.state.batchesLoading && this.state.selectedWarehouseId && (
                this.state.batches.length === 0 ? (
                  <div style={{ color: '#888', fontSize: '13px' }}>No batch records found. Batches are tracked from new inward entries after the feature is enabled.</div>
                ) : (
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                    <thead>
                      <tr style={{ backgroundColor: '#f5f5f5' }}>
                        <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Batch ID</th>
                        <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Brand</th>
                        <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Received Date</th>
                        <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Expiry Date</th>
                        <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'left' }}>Days Left</th>
                        <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'right' }}>Qty Remaining</th>
                        <th style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'center' }}>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {this.state.batches.map((batch) => {
                        const isExpired = batch.isExpired;
                        const rowStyle = isExpired ? { backgroundColor: '#ffebee' } : (batch.daysUntilExpiry != null && batch.daysUntilExpiry <= 30 ? { backgroundColor: '#fff8e1' } : {});
                        return (
                          <React.Fragment key={batch.batchId}>
                            <tr style={rowStyle}>
                              <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.batchId}</td>
                              <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.brand || '-'}</td>
                              <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.receivedDate ? new Date(batch.receivedDate).toLocaleDateString('en-GB') : '-'}</td>
                              <td style={{ padding: '6px', border: '1px solid #ddd' }}>{batch.expiryDate ? new Date(batch.expiryDate).toLocaleDateString('en-GB') : '-'}</td>
                              <td style={{ padding: '6px', border: '1px solid #ddd', color: isExpired ? '#c62828' : (batch.daysUntilExpiry != null && batch.daysUntilExpiry <= 30 ? '#e65100' : 'inherit') }}>
                                {batch.expiryDate ? (isExpired ? 'Expired' : `${batch.daysUntilExpiry}d`) : '-'}
                              </td>
                              <td style={{ padding: '6px', border: '1px solid #ddd', textAlign: 'right' }}>{batch.qtyRemaining}</td>
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
                                      {this.state.writeOffSubmitting ? 'Saving...' : 'Confirm'}
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
                )
              )}
            </div>
          )}
        </div>
      </div>
    );
  }
}

export default Details;
