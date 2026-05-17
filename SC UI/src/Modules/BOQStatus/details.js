import React, { Component } from "react";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import { messages } from "./../../messages";
import "./style.scss";

class Details extends Component {
  state = { value: 0 };

  getStatusClass(status) {
    if (status > 0)    return "exceeded";
    if (status >= -20) return "at-risk";
    return "on-track";
  }

  renderStatusBar(status) {
    const s = status !== null && status !== undefined ? status : 0;
    const consumed = s + 100;
    const barWidth = Math.min(Math.max(consumed, 0), 100);
    const cls = this.getStatusClass(s);
    return (
      <div className="boq-progress-cell">
        <div className="boq-progress-bar">
          <div className={`boq-progress-fill ${cls}`} style={{ width: `${barWidth}%` }} />
        </div>
        <span className={`boq-progress-label ${cls}`}>{Math.max(consumed, 0).toFixed(0)}%</span>
      </div>
    );
  }

  render() {
    const data = this.props.data;
    return (
      <div className="list-section detail-section stock">
        <div className="details-header">
          {messages.common.details}
          <div>
            <IconButton aria-label="back" onClick={() => this.props.close(data)} className="back-icon">
              <Clear />
            </IconButton>
          </div>
        </div>
        <div className="details-main-content">
          <div className="detail-item">
            <div className="label">{messages.common.buildingType}</div>
            <div className="value">{data.buildingType}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.location}</div>
            <div className="value">{data.buildingUnit}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.category}</div>
            <div className="value">{data.category}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.inventory}</div>
            <div className="value">{data.product}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.boqQuantity}</div>
            <div className="value">{Number(data.boqQuantity).toFixed(2)}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.outwardQuantity}</div>
            <div className="value">{Number(data.outwardQuantity).toFixed(2)}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.boqStatus}</div>
            <div className="value">{this.renderStatusBar(data.status)}</div>
          </div>
          <table>
            <thead>
              <tr>
                <th>{messages.common.finalLocation}</th>
                <th>{messages.common.boqQuantity}</th>
                <th>{messages.common.outwardQuantity}</th>
                <th>{messages.common.boqStatus}</th>
              </tr>
            </thead>
            <tbody>
              {data.boqDetails.map((item, idx) => (
                <tr key={idx}>
                  <td data-label={messages.common.finalLocation}>{item.finalLocation}</td>
                  <td data-label={messages.common.boqQuantity}>{Number(item.boqQuantity).toFixed(2)}</td>
                  <td data-label={messages.common.outwardQuantity}>{Number(item.outwardQuantity).toFixed(2)}</td>
                  <td data-label={messages.common.boqStatus}>{this.renderStatusBar(item.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }
}

export default Details;
