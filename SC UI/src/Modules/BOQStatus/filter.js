// import { options } from "@amcharts/amcharts4/core";
import React from "react";
import CommonFilter from "./../../Shared/Filter";
// import "./style.scss";
class filter extends CommonFilter {

  componentDidMount() {
    // this.filterData={};
    // this.setState({ reset: false }, () =>
    // this.setState({ reset: true }));
    // this.onReset();
    console.log('options data in filter', this.props.options);
    console.log('this.filterData at Filter.js: ',this.filterData);
    // this.getFilterList(); 
  }

 
  consumedPercent = [
    "0-10 %",
    "10-20 %",
    "20-30 %",
    "30-40 %",
    "40-50 %",
    "50-60 %",
    "60-70 %",
    "70-80 %",
    "80-90 %",
    "90-100 %",
    "above 100 %",
  ];

  renderFilter() {
    return (
      <div className="filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            {this.renderAutoComplete(
              "Category",
              this.props.options?.category,
              "category",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Inventory",
              this.props.options?.product,
              "product",
              (option) => {
                return option["name"];
              }
            )}
            {this.renderAutoComplete(
              "Consumed Percentage",
              this.consumedPercent,
              "consumedPercentage",
            )}
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
