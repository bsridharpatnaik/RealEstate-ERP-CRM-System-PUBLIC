import React, { Component } from "react";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import ExpandMore from "@material-ui/icons/ExpandMore";
import ExpandLess from "@material-ui/icons/ExpandLess";
import { messages } from "./../../messages";
import "./style.scss";

class Details extends Component {
  state = { value: 0, expandedWarehouse: null, activeTab: 'details' };

  handleExpandClick = (warehouseName) => {
    this.setState({
      expandedWarehouse: this.state.expandedWarehouse === warehouseName ? null : warehouseName,
    });
  };

  handleTabChange = (tab) => {
    this.setState({ activeTab: tab });
  };

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
        </div>
      </div>
    );
  }
}

export default Details;
