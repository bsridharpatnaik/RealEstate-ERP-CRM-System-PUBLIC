import React, { Component } from "react";
import * as am4core from "@amcharts/amcharts4/core";
import * as am4charts from "@amcharts/amcharts4/charts";
import am4themes_animated from "@amcharts/amcharts4/themes/animated";

am4core.useTheme(am4themes_animated);

export const STALE_COLOR_PALETTE = [
  "#5e81f4",
  "#44c4a1",
  "#FF808B",
  "#f9b571",
  "#4D4CAC",
  "#a2a2c3",
  "#85CDCA",
  "#E8A87C",
  "#8C5C9E",
  "#F28482",
];

class StaleStackedColumnChart extends Component {
  static defaultProps = {
    chartId: "staleStackedChartDiv",
  };

  componentDidMount() {
    const { data, categoryField, seriesKeys } = this.props;
    if (!Array.isArray(data) || !data.length || !Array.isArray(seriesKeys) || !seriesKeys.length) {
      return;
    }

    const { chartId } = this.props;
    const id = chartId || "staleStackedChartDiv";

    const chart = am4core.create(id, am4charts.XYChart);
    chart.responsive.enabled = true;
    chart.data = data;
    chart.maskBullets = false;
    chart.paddingLeft = 0;

    const xAxis = chart.xAxes.push(new am4charts.CategoryAxis());
    xAxis.dataFields.category = categoryField;
    xAxis.renderer.grid.template.location = 0;
    xAxis.renderer.minGridDistance = 40;
    xAxis.renderer.grid.template.disabled = true;

    const labelTemplate = xAxis.renderer.labels.template;
    labelTemplate.rotation = -45;
    labelTemplate.horizontalCenter = "right";
    labelTemplate.verticalCenter = "middle";
    labelTemplate.fontSize = 11;
    labelTemplate.truncate = true;
    labelTemplate.maxWidth = 80;

    const yAxis = chart.yAxes.push(new am4charts.ValueAxis());
    yAxis.renderer.minWidth = 40;
    yAxis.min = 0;

    // Legend is rendered as HTML outside this component — no built-in legend needed.

    seriesKeys.forEach((field, index) => {
      const series = chart.series.push(new am4charts.ColumnSeries());
      series.dataFields.valueY = field;
      series.dataFields.categoryX = categoryField;
      series.name = field;
      series.stacked = true;
      series.columns.template.width = am4core.percent(80);
      series.columns.template.tooltipText = "{name}: {categoryX}: {valueY}";
      series.columns.template.strokeOpacity = 0;
      series.columns.template.fill = am4core.color(
        STALE_COLOR_PALETTE[index % STALE_COLOR_PALETTE.length]
      );

      const labelBullet = series.bullets.push(new am4charts.LabelBullet());
      labelBullet.label.text = "{valueY}";
      labelBullet.locationY = 0.5;
      labelBullet.locationX = 0.5;
      labelBullet.label.fill = am4core.color("#ffffff");
      labelBullet.label.fontSize = 11;
    });

    chart.cursor = new am4charts.XYCursor();
    chart.cursor.lineX.strokeOpacity = 0.2;
    chart.cursor.lineY.strokeOpacity = 0.2;

    chart.appear(1000, 100);

    this.chart = chart;
  }

  componentWillUnmount() {
    if (this.chart) {
      this.chart.dispose();
    }
  }

  render() {
    const { chartId } = this.props;
    const id = chartId || "staleStackedChartDiv";
    return (
      <div
        id={id}
        style={{ width: "100%", height: "100%" }}
      />
    );
  }
}

export default StaleStackedColumnChart;

