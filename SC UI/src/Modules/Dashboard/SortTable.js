import Skeleton from "@material-ui/lab/Skeleton";
import React, { Component } from "react";
import ArrowDropUpIcon from "@material-ui/icons/ArrowDropUp";
import ArrowDropDownIcon from "@material-ui/icons/ArrowDropDown";
import { API } from "../../axios";
import "./style.scss";
const height = 35;

class SortTable extends Component {
  state = {
    data: [],
    sortKey: '',
    sortDirection: ''
  }
  componentDidMount() {
    this.getTableData();
  }
  async getTableData() {
    let { sortKey, sortDirection } = this.state;
    const { headings, keys } = this.props;
    this.setState({ isLoaded: false });
    let sort = '';
    if (!(sortKey && sortDirection)) {
      sortKey = keys[0];
      sortDirection = 'asc';
      this.setState({ sortKey, sortDirection });
    }
    sort = `${sortKey},${sortDirection}`;
    const response = await API.GET(`${this.props.apiUrl}?sort=${sort}`);
    if (response.success) {
      this.setState({ isLoaded: true, data: response?.data?.content });
    }
  }
  renderSortIcon(index) {
    const { keys } = this.props;
    const { sortKey, sortDirection } = this.state;
    const header = keys[index];
    if (header === sortKey && sortDirection === "desc") {
      return <ArrowDropDownIcon />;
    } else if (header === sortKey) {
      return <ArrowDropUpIcon />;
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
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
      </React.Fragment>
    );
  }
  sort(index) {
    let { keys } = this.props;
    let { sortKey, sortDirection } = this.state;
    const key = keys[index];
    if (key === sortKey) {
      if (sortDirection === "asc") {
        sortDirection = "desc";
      } else {
        sortDirection = "asc";
      }
    } else {
      sortKey = key;
      sortDirection = "asc";
    }
    this.setState({ sortKey, sortDirection }, () => this.getTableData())
  }
  renderData() {
    const { data } = this.state;
    const { headings, keys } = this.props;
    return (
      <div className="notification-content">
        <table className="sort-table">
          <thead>
            <tr>
              {
                headings.map((heading, i) => (
                  <th key={i} onClick={() => { this.sort(i); }} style={{ cursor: "pointer" }}>
                    <div style={{ display: "flex", alignItems: "center" }}>
                      {heading} {this.renderSortIcon(i)}
                    </div>
                  </th>
                ))
              }
            </tr>
          </thead>
          <tbody>
            {data.map((item, i) => (
              <tr>
                {
                  keys.map((key, j) => (
                    <td data-label={headings[j]} key={j}>{item[key]}</td>
                  ))
                }
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  render() {
    return this.state.isLoaded ? this.renderData() : this.renderLoader();
  }
}

export default SortTable;
