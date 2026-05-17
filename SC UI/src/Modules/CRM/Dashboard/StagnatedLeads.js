import React, { Component } from "react";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import Skeleton from "@material-ui/lab/Skeleton";

const height = 35;
class StagnatedLeads extends Component {

    state = {isStagnatedLeadsLoaded: false};
    data = {};
    async componentDidMount() {
        this.setState({isStagnatedLeadsLoaded: false})
        const response = await API.GET(apiEndpoints.getstagnantDetailsDB);
        if(response.success){
            this.data = response.data;
            this.setState({isStagnatedLeadsLoaded: true})
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
                  <th>Name</th>
                  <th>10 - 20 Days</th>
                  <th>20 - 30 Days</th>
                  <th>{">"}30 Days</th>
                </tr>
              </thead>
              <tbody>
                {this.data.map((item) => {
                  return (
                    <tr>
                      <td data-label='Name'>{item.assigneename}</td>
                      <td data-label='10 - 20 Days'>{item.tenTo20Days}</td>
                      <td data-label='20 - 30 Days'>{item.twentyTo30Days}</td>
                      <td data-label='{">"}30 Days'>{item.greaterThan30Days}</td>
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
                <div className="dashboard-heading stagnant">
                    Stagnated Leads
                </div>
                {this.state.isStagnatedLeadsLoaded ? this.renderData() : this.renderLoader()}
            </div>
        )
    }
}

export default StagnatedLeads;