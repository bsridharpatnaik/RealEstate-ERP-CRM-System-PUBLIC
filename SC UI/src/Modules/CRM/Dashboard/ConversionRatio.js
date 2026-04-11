import React, { Component } from "react";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import Skeleton from "@material-ui/lab/Skeleton";

const height = 35;
class ConversionRatio extends Component {

    state = {isConversionRatioLoaded: false};
    data = {};
    async componentDidMount() {
        this.setState({isConversionRatioLoaded: false})
        const response = await API.GET(apiEndpoints.getconversionRatioDB);
        if(response.success){
            this.data = response.data;
            this.setState({isConversionRatioLoaded: true})
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
                  <th>Lead Generated</th>
                  <th>Deal Closed</th>
                  <th>Ratio</th>
                </tr>
              </thead>
              <tbody>
                {this.data.map((item) => {
                  return (
                    <tr>
                      <td data-label='Name' >{item.asigneeName}</td>
                      <td data-label='Lead Generated'>{item.totalcount}</td>
                      <td data-label='Deal Closed'>{item.convertedcount}</td>
                      <td data-label='Ratio'>{item.ratio}%</td>
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
                <div className="dashboard-heading conversion">
                    Conversion Ratio
                </div>
                {this.state.isConversionRatioLoaded ? this.renderData() : this.renderLoader()}
            </div>
        )
    }
}

export default ConversionRatio;