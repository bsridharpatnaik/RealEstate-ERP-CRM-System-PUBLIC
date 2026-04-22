import React, { Component } from "react";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import Skeleton from "@material-ui/lab/Skeleton";

const height = 35;
class CustomerPipeline extends Component {

  state = { isLoaded: false };
  data = {};
  async componentDidMount() {
    this.setState({ isLoaded: false })
    const response = await API.GET(apiEndpoints.getleadstagemapping);
    if (response.success) {
      this.data = response.data;
      this.setState({ isLoaded: true })
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


  renderData() {
    return (
      <div className="notification-content">
        <table>
          <thead>
            <tr>
              <th>Agent</th>
              <th>NL</th>
              <th>VS</th>
              <th>VC</th>
              <th>Neg.</th>
              <th>DL</th>
              <th>DC</th>
            </tr>
          </thead>
          <tbody>
            {this.data.map((item) => {
              return (
                <tr>
                  <td data-label='Agent'><b>{item.userName}</b></td>
                  <td data-label='NL'>{item.newLead}</td>
                  <td data-label='VS'>{item.visitScheduled}</td>
                  <td data-label='VC'>{item.visitCompleted}</td>
                  <td data-label='Neg.'>{item.negotiation}</td>
                  <td data-label='DL'>{item.dealLost}</td>
                  <td data-label='DC'>{item.dealClosed}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    );
  }


  render() {
    return (
      <div className="notification">
        <div className="dashboard-heading customer-pipeline">
          Customer Pipeline
        </div>
        {this.state.isLoaded ? this.renderData() : this.renderLoader()}
      </div>
    )
  }
}

export default CustomerPipeline;