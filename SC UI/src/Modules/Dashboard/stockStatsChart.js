import { Skeleton } from "@material-ui/lab";
import React, { Component } from "react";
import Chart from '../../Shared/Chart';
import "./style.scss";

const height = 35;

class StockStatsChart extends Component {

  render() {
    return (
      <React.Fragment>
        <div className="dashboard-heading">Reorder Level Status</div>
        {this.props.isLoaded ? (
          <div className="chart">
            <div className="chart-wrapper">
              <Chart
                data={this.props.data}
                dashboardChart={false}
                dateX="productName"
                valueY="stockPercent"
                XtraValue="currentStock"
                tooltipHTML="{categoryX}<br/>Reorder Status - {valueY}%<br/>Current Stock - {XtraValue}"
              />
            </div>
          </div>
        ) : (
          <React.Fragment>
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
          </React.Fragment>
        )}
      </React.Fragment>
    );
  }
}

export default StockStatsChart;
