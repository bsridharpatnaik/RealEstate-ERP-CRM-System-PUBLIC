import React, { Component } from "react";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import { messages } from "./../../messages";
import "./style.scss";
class Details extends Component {
  state = { value: 0 };
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
        <div className="details-main-content">
          <div className="detail-item">
            <div className="label">{messages.common.product}</div>
            <div className="value">{data.productName}</div>
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
              {data.detailedStock.map((item) => {
                return (
                  <tr>
                    <td data-label={messages.common.warehouse}>{item.warehouseName}</td>
                    <td data-label={messages.common.warehouseStock}>{item.quantityInHand}</td>
                    <td data-label={messages.common.unit}>{item.measurementUnit}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    );
  }
}

export default Details;
