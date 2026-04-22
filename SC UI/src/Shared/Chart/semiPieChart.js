import React, { Component } from "react";
import * as am4core from "@amcharts/amcharts4/core";
import * as am4charts from "@amcharts/amcharts4/charts";
import am4themes_animated from "@amcharts/amcharts4/themes/animated";
import am4themes_kelly from "@amcharts/amcharts4/themes/kelly";
am4core.useTheme(am4themes_kelly);
am4core.useTheme(am4themes_animated);

const colors = {
  green: "#44c4a1",
  blue: "#4D4CAC",
  red: "#FF808B",
};

class Chart extends Component {
  componentDidMount() {
    var chart = am4core.create("semiPieChart", am4charts.PieChart);
    chart.hiddenState.properties.opacity = 0; // this creates initial fade-in

    chart.data = this.props.data;
    chart.radius = am4core.percent(70);
    chart.innerRadius = am4core.percent(40);
    chart.startAngle = 180;
    chart.endAngle = 360;
    chart.labelsEnabled=false;
    var series = chart.series.push(new am4charts.PieSeries());
    series.dataFields.value = "value";
    series.dataFields.category = "leadStatus";

    series.slices.template.cornerRadius = 3;
    series.slices.template.innerCornerRadius = 3;
    series.alignLabels = false;
    series.slices.template.strokeWidth = 1;
    series.slices.template.strokeOpacity = 1;
    series.slices.template.propertyFields.fill = "color"
    series.hiddenState.properties.startAngle = 90;
    series.hiddenState.properties.endAngle = 90;
    series.labels.template.maxWidth = 100;
    series.labels.template.wrap = true;
    series.labels.template.fontSize = 9;
    series.labels.template.text = "{leadStatus}";
    this.chart = chart;
  }

  componentWillUnmount() {
    if (this.chart) {
      this.chart.dispose();
    }
  }

  render() {
    return (
      <div id="semiPieChart" style={{ width: "100%", height: "100%" }}></div>
    );
  }
}

export default Chart;
