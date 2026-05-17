import React, { Component } from "react";
import * as am4core from "@amcharts/amcharts4/core";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import Skeleton from "@material-ui/lab/Skeleton";
import SemiPieChart from "./../../../Shared/Chart/semiPieChart";
const height = 35;

class SalesFunnel extends Component {
  state = { isSalesFunnelLoaded: false, data: [] };
  async componentDidMount() {
    this.setState({ isSalesFunnelLoaded: false });
    const response = await API.GET(apiEndpoints.getSalesFunnel);
    if (response.success) {
      let data = response.data;
      data = data.map(item => {
        if(item.leadStatus === "Deal_Closed") {
          item.color = am4core.color("#44c4a1");
        }
        return item;
      });
      this.setState({ isSalesFunnelLoaded: true, data });
    }
  }

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

  renderChart() {
    return <SemiPieChart data={this.state.data} />;
  }

  render() {
    return (
      <div className="notification">
        <div className="dashboard-heading performer">Sales Snapshot</div>
        {this.state.isSalesFunnelLoaded
          ? this.renderChart()
          : this.renderLoader()}
      </div>
    );
  }
}

export default SalesFunnel;
