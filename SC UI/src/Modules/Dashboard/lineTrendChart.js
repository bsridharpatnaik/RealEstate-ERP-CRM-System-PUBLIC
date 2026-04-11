import { Skeleton } from "@material-ui/lab";
import React, { Component } from "react";
import BiLineChart from "../../Shared/Chart/BiLineChart";
import "./style.scss";

const height = 35;

class LineTrendChart extends Component {

  render() {
    return (
      <React.Fragment>
        <div className="dashboard-heading">Inward/Outward Trend</div>
        {this.props.isLoaded ? (
          <React.Fragment>
            <div className="chart">
              <div className="chart-wrapper">
                <BiLineChart
                  data={this.props.data}
                  xAxisField="date"
                  seriesOneField="inwardCount"
                  seriesTwoField="outwardCount"
                  seriesOneLabel="Inward"
                  seriesTwoLabel="Outward"
                />
              </div>
            </div>
          </React.Fragment>
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

export default LineTrendChart;
