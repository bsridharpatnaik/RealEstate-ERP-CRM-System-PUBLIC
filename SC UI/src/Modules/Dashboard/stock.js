import { FormControl, FormControlLabel, Radio, RadioGroup } from "@material-ui/core";
import Skeleton from "@material-ui/lab/Skeleton";
import React, { Component } from "react";
import "./style.scss";

const height = 35;

class Stock extends Component {
  state = {
    view: 'daily'
  }
  renderLoader() {
    return (
      <React.Fragment>
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
      </React.Fragment>
    );
  }
  renderData() {
    const data = this.props.data;
    return (
      <div className="notification-content">
        <table className="ioStats">
          <thead>
            <tr>
              <th>Inventory Name</th>
              <th>Unit</th>
              <th style={{ textAlign: 'center' }}>Current Stock</th>
              <th style={{ textAlign: 'center', backgroundColor: '#f8f8f8', borderTopLeftRadius: '5px', borderTopRightRadius: '5px' }}>Inward</th>
              <th style={{ textAlign: 'center' }}>Outward</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item) => {
              return (
                <tr>
                  <td data-label="Inventory Name">{item?.productName}</td>
                  <td data-label="Unit">{item?.measurementUnit}</td>
                  <td data-label="Current Stock" style={{ textAlign: 'right' }}>{item?.currentStock}</td>
                  {this.state.view === 'daily' && <td data-label="Inward" style={{ textAlign: 'right', backgroundColor: '#f8f8f8' }}>{item?.inward?.daily}</td>}
                  {this.state.view === 'weekly' && <td data-label="Inward" style={{ textAlign: 'right', backgroundColor: '#f8f8f8' }}>{item?.inward?.weekly}</td>}
                  {this.state.view === 'monthly' && <td data-label="Inward" style={{ textAlign: 'right', backgroundColor: '#f8f8f8' }}>{item?.inward?.monthly}</td>}
                  {this.state.view === 'daily' && <td data-label="Outward" style={{ textAlign: 'right' }}>{item?.outward?.daily}</td>}
                  {this.state.view === 'weekly' && <td data-label="Outward" style={{ textAlign: 'right' }}>{item?.outward?.weekly}</td>}
                  {this.state.view === 'monthly' && <td data-label="Outward" style={{ textAlign: 'right' }}>{item?.outward?.monthly}</td>}
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
      <div className="notification iostats">
        <div className="dashboard-heading">
          {'Inward/Outward Status'}
          <FormControl component="fieldset">
            <RadioGroup className="radio-group" name="view" value={this.state.view} onChange={(event) => this.setState({ view: event.target.value })}>
              <FormControlLabel value="daily" control={<Radio color="primary" size="small" />} label="Daily" />
              <FormControlLabel value="weekly" control={<Radio color="primary" size="small" />} label="Weekly" />
              <FormControlLabel value="monthly" control={<Radio color="primary" size="small" />} label="Monthly" />
            </RadioGroup>
          </FormControl>
        </div>
        {this.props.isLoaded ? this.renderData() : this.renderLoader()}
      </div>
    );
  }
}

export default Stock;
