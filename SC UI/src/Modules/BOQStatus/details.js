import React, { Component } from "react";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import { messages } from "./../../messages";
import "./style.scss";
class Details extends Component {
  state = { value: 0 };
  render() {
    const data = this.props.data;
    // console.log("Data in Details: ", data);
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
            <div className="label">{messages.common.inventory}</div>
            <div className="value">{data.product}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.category}</div>
            <div className="value">{data.category}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.boqQuantity}</div>
            <div className="value">{data.boqQuantity}</div>
          </div>
          <div className="detail-item">
            <div className="label">{messages.common.outwardQuantity}</div>
            <div className="value">{data.outwardQuantity}</div>
          </div>
          <table>
            <thead>
              <tr>
                <th>{messages.common.finalLocation}</th>
                <th>{messages.common.boqQuantity}</th>
                <th>{messages.common.outwardQuantity}</th>
              </tr>
            </thead>
            <tbody>
              {data.boqDetails.map(item => {
                return (
                  <tr>
                    <td data-label={messages.common.finalLocation}>{item.finalLocation}</td>
                    <td data-label={messages.common.boqQuantity}>{item.boqQuantity}</td>
                    <td data-label={messages.common.outwardQuantity}>{item.outwardQuantity}</td>
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
