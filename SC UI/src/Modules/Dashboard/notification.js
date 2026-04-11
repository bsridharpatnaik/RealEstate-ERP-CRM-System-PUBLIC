import React, { Component } from "react";
import { messages } from "./../../messages";
import Skeleton from "@material-ui/lab/Skeleton";
import CancelIcon from "@material-ui/icons/Cancel";
import CloseIcon from "@material-ui/icons/Close";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { IconButton } from "@material-ui/core";
import "./style.scss";
const height = 35;

class notification extends Component {
  renderLoader() {
    return (
      <React.Fragment>
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
      </React.Fragment>
    );
  }
  async deleteNotification(id) {
    const response = await API.DELETE(apiEndpoints.deleteNotification + id);
    if (response.success) {
      this.props.refreshNotification();
    }
  }
  renderNotification() {
    const data = this.props.data;
    return (
      <div className="notification-content">
        {data.map((item, index) => {
          return (
            <div
              className={index === data.length - 1 ? "item last" : "item"}
              key={index}
            >
              <div className="date">
                {item.updated}
                {item.canBeDeleted && (
                  <span
                    className="cancel"
                    onClick={() => this.deleteNotification(item.notificationId)}
                  >
                    <CancelIcon />
                  </span>
                )}
              </div>
              <div className="notification-text">{item.message}</div>
            </div>
          );
        })}
      </div>
    );
  }
  render() {
    return (
      <div className="notification notification-drawer">
        <div className="dashboard-heading notification-header">
          <span>{messages.common.notification}</span>
          <IconButton
          aria-label="back"
          onClick={this.props.onClose}
          className="close-icon"
        >
          <CloseIcon />
        </IconButton>
        </div>
        {this.props.isLoaded ? this.renderNotification() : this.renderLoader()}
      </div>
    );
  }
}

export default notification;
