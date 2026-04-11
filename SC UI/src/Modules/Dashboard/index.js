import { IconButton } from "@material-ui/core";
import Drawer from '@material-ui/core/Drawer';
import NotificationsIcon from '@material-ui/icons/Notifications';
import { ToggleButton, ToggleButtonGroup } from "@material-ui/lab";
import moment from "moment";
import React, { Component } from "react";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";
import LineTrendChart from "./lineTrendChart";
import Notification from "./notification";
import SortTable from "./SortTable";
import Stock from "./stock";
import StockStatsChart from "./stockStatsChart";
import "./style.scss";

class Dashboard extends Component {
  state = {
    isIOStatsLoaded: false,
    isStatsChartLoaded: false,
    notificationOpen: false,
    notificationLoaded: false,
    isIOTrendLoaded: false,
    ioStatsData: [],
    statsChartData: [],
    notifications: [],
    IOTrendData: [],
    isInward: true
  };
  async componentDidMount() {
    this.getIOStatsData();
    this.getStatsChartData();
    this.refreshNotification();
    this.getIOTrendData();
  }
  async refreshNotification() {
    this.setState({ notificationLoaded: false });
    const response = await API.GET(apiEndpoints.getNotification);
    if (response.success) {
      let notifications = response.data.notifications;
      this.setState({ notificationLoaded: true, notifications });
    }
  }
  async getIOStatsData() {
    this.setState({ isIOStatsLoaded: false });
    const response = await API.GET(apiEndpoints.iostats);
    if (response.success) {
      this.setState({ isIOStatsLoaded: true, ioStatsData: response.data });
    }
  }
  async getStatsChartData() {
    this.setState({ isStatsChartLoaded: false });
    const response = await API.GET(apiEndpoints.stockstats);
    if (response.success) {
      this.setState({ isStatsChartLoaded: true, statsChartData: response.data });
    }
  }
  async getIOTrendData() {
    this.setState({ isIOTrendLoaded: false });
    const response = await API.GET(apiEndpoints.iotrend);
    if (response.success) {
      let { data = [] } = response;
      data.sort((a,b) => {
        let date1 = moment(a.date, "DD-MM-YYYY").unix();
        let date2 = moment(b.date, "DD-MM-YYYY").unix();
        return date1 - date2;
      })
      this.setState({ isIOTrendLoaded: true, IOTrendData: response.data });
    }
  }
  render() {
    return (
      <div className="dashboard-wrapper inventory-dashboard-v2">
        <div className="top">
          <div className="left">
            <Stock
              data={this.state.ioStatsData}
              isLoaded={this.state.isIOStatsLoaded}
            />
          </div>
          <div className="right">
            <StockStatsChart
              data={this.state.statsChartData}
              isLoaded={this.state.isStatsChartLoaded}
            />
          </div>
        </div>
        <div className="bottom">
          <div className="left">
            <LineTrendChart
              isLoaded={this.state.isIOTrendLoaded}
              data={this.state.IOTrendData}
            />
          </div>
          <div className="right">
            <div className="notification sort-table">
              <div className="dashboard-heading">
                <ToggleButtonGroup
                  value={this.state.isInward}
                  exclusive
                  onChange={(e, isInward) => this.setState({ isInward })}
                  size="small"
                >
                  <ToggleButton value={true}>
                    Supplier Stats
                  </ToggleButton>
                  <ToggleButton value={false}>
                    Contractor Stats
                  </ToggleButton>
                </ToggleButtonGroup>
              </div>
              {
                this.state.isInward
                &&
                <SortTable
                  apiUrl={apiEndpoints.inwardstats}
                  headings={["Supplier Name", "Average Lead Time", "Total Inwards", "Inward Rejects"]}
                  keys={["supplierName", "avgLeadTime", "inwardCount", "rejectCount"]}
                />
              }
              {
                !this.state.isInward
                &&
                <SortTable
                  apiUrl={apiEndpoints.outwardstats}
                  headings={["Contractor Name", "Total Outwards", "Total Rejects"]}
                  keys={["contractorName", "totalCount", "rejectCount"]}
                />
              }
            </div>
          </div>
        </div>
        <div className="notification-bell">
          <IconButton
            onClick={() => { this.setState({ notificationOpen: true }) }}
          >
            <NotificationsIcon />
          </IconButton>
        </div>
        <Drawer anchor="right" open={this.state.notificationOpen} onClose={() => { this.setState({ notificationOpen: false }) }}>
          <Notification
            data={this.state.notifications}
            isLoaded={this.state.notificationLoaded}
            refreshNotification={() => this.refreshNotification()}
            onClose={() => { this.setState({ notificationOpen: false }) }}
          />
        </Drawer>
      </div>
    );
  }
}

export default Dashboard;
