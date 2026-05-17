import React, { Component } from "react";
import * as am4core from "@amcharts/amcharts4/core";
import * as am4charts from "@amcharts/amcharts4/charts";
import am4themes_animated from "@amcharts/amcharts4/themes/animated";
import { LensTwoTone } from "@material-ui/icons";

am4core.useTheme(am4themes_animated);

const colors = {
  green: "#44c4a1",
  blue: "#4D4CAC",
  red: "#FF808B",
};

class Chart extends Component {
  componentDidMount() {
    let chart = am4core.create("chartdiv", am4charts.XYChart);

    chart.data = this.props.data;
    chart.responsive.enabled = true;

    let categoryAxis = chart.xAxes.push(new am4charts.CategoryAxis());
    categoryAxis.dataFields.category = this.props.dateX;

    // if(!this.props.dashboardChart) {
    categoryAxis.renderer.grid.template.location = 0;
    categoryAxis.renderer.minGridDistance = 20;
    categoryAxis.renderer.inside = true;
    categoryAxis.renderer.grid.template.disabled = true;
    let labelTemplate = categoryAxis.renderer.labels.template;
    labelTemplate.rotation = -90;
    labelTemplate.horizontalCenter = "left";
    labelTemplate.verticalCenter = "middle";
    labelTemplate.truncate = true;
    labelTemplate.width = 180;
    //labelTemplate.dy = 10; // moves it a bit down;
    labelTemplate.inside = false; // this is done to avoid settings which are not suitable when label is rotated
    // } else{
    //   categoryAxis.renderer.grid.template.location = 0;
    //   categoryAxis.renderer.minGridDistance = 30;
    //   let labelTemplate = categoryAxis.renderer.labels.template;
    //   labelTemplate.truncate = true;
    //   labelTemplate.maxWidth = 80;
    //   labelTemplate.tooltipText = "{category}";
    //   // categoryAxis.renderer.labels.template.adapter.add("dy", function(dy, target) {
    //   //   if (target.dataItem && target.dataItem.index & 2 == 2) {
    //   //     return dy + 25;
    //   //   }
    //   //   return dy;
    //   // });
    // }

    let valueAxis = chart.yAxes.push(new am4charts.ValueAxis());

    // Create series
    let series = chart.series.push(new am4charts.ColumnSeries());
    series.dataFields.valueY = this.props.valueY;
    series.dataFields.categoryX = this.props.dateX;
    if(this.props.XtraValue) {
      series.dataFields.XtraValue = this.props.XtraValue;
      series.columns.template.tooltipHTML = this.props.tooltipHTML;
    } else {
      series.columns.template.tooltipText = "{categoryX}: [bold]{valueY}[/]";
    }
    series.columns.template.fillOpacity = 0.8;
    if (this.props.dashboardChart) {
      series.columns.template.adapter.add("fill", function (fill, target) {
        if (target.dataItem.valueY < 80) {
          return am4core.color(colors.red);
        } else if (target.dataItem.valueY < 120) {
          return am4core.color(colors.blue);
        } else {
          return am4core.color(colors.green);
        }
      });
    } else {
      series.columns.template.fill = am4core.color("#44c4a1");
    }
    series.columns.template.width = am4core.percent(50);
    // if(this.props.dashboardChart)
    // {
    //   let bullet = series.bullets.push(new am4charts.LabelBullet())
    //   bullet.interactionsEnabled = false
    //   bullet.dy = -5;
    //   bullet.label.text = '{valueY}%'
    //   bullet.label.fontSize = 8;
    //   //bullet.label.fill = am4core.color('#ffffff')
    //   bullet.label.adapter.add("fill", function(fill, target) {
    //     if (target.dataItem.valueY < 80) {
    //       return am4core.color(colors.red);
    //     } else if (target.dataItem.valueY < 120) {
    //       return am4core.color(colors.blue);
    //     } else {
    //       return am4core.color(colors.green);
    //     }
    //   });
    // }
    this.chart = chart;
  }

  componentWillUnmount() {
    if (this.chart) {
      this.chart.dispose();
    }
  }

  render() {
    return <div id="chartdiv" style={{ width: "100%", height: "100%" }}></div>;
  }
}

export default Chart;
