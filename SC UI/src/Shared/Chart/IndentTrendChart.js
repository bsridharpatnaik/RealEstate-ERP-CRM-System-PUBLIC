import React, { Component } from "react";
import * as am4core from "@amcharts/amcharts4/core";
import * as am4charts from "@amcharts/amcharts4/charts";
import am4themes_animated from "@amcharts/amcharts4/themes/animated";

am4core.useTheme(am4themes_animated);

const LINE_COLORS = [
  "#5e81f4",
  "#44c4a1",
  "#FF808B",
  "#f9b571",
  "#4D4CAC",
];

/**
 * Build chart data from API response: { periods: ["W-0","W-1",...], series: [{ label, data }, ...] }
 * Output: [{ category: "W-0", "Created": 0, "PO Partial": 3, ... }, ...]
 */
function buildChartData(periods = [], series = []) {
  if (!periods.length || !series.length) return [];
  return periods.map((period, i) => {
    const row = { category: period };
    series.forEach((s) => {
      row[s.label] = s.data[i] != null ? s.data[i] : 0;
    });
    return row;
  });
}

class IndentTrendChart extends Component {
  static defaultProps = {
    chartId: "indentTrendChartDiv",
  };

  componentDidMount() {
    const { title, periods, series, chartId } = this.props;
    const chartData = buildChartData(periods, series);
    if (!chartData.length) return;

    const id = chartId || "indentTrendChartDiv";
    const chart = am4core.create(id, am4charts.XYChart);
    chart.responsive.enabled = true;
    chart.data = chartData;

    const xAxis = chart.xAxes.push(new am4charts.CategoryAxis());
    xAxis.dataFields.category = "category";
    xAxis.renderer.grid.template.location = 0;
    xAxis.renderer.minGridDistance = 40;
    xAxis.renderer.grid.template.disabled = true;

    const valueAxis = chart.yAxes.push(new am4charts.ValueAxis());
    valueAxis.renderer.minWidth = 40;

    const colorList = LINE_COLORS.map((c) => am4core.color(c));

    series.forEach((s, idx) => {
      const lineSeries = chart.series.push(new am4charts.LineSeries());
      lineSeries.dataFields.valueY = s.label;
      lineSeries.dataFields.categoryX = "category";
      lineSeries.name = s.label;
      lineSeries.stroke = colorList[idx % colorList.length];
      lineSeries.strokeWidth = 2;
      lineSeries.tooltipText = "{name}: {valueY}";
      lineSeries.tensionX = 1;
      lineSeries.tensionY = 1;

      const bullet = lineSeries.bullets.push(new am4charts.CircleBullet());
      bullet.circle.strokeWidth = 2;
      bullet.circle.fill = am4core.color("#fff");
      bullet.circle.stroke = lineSeries.stroke;
      bullet.circle.radius = 4;

      const hoverState = bullet.states.create("hover");
      hoverState.properties.scale = 1.2;

      const segment = lineSeries.segments.template;
      segment.interactionsEnabled = true;
      const segmentHover = segment.states.create("hover");
      segmentHover.properties.strokeWidth = 3;
    });

    chart.legend = new am4charts.Legend();
    chart.legend.position = "top";
    chart.legend.contentAlign = "left";

    chart.cursor = new am4charts.XYCursor();
    chart.cursor.lineX.strokeOpacity = 0.2;
    chart.cursor.lineY.strokeOpacity = 0.2;

    this.chart = chart;

    chart.legend.itemContainers.template.events.on("over", (ev) => {
      const series = ev.target.dataItem?.dataContext;
      if (!series) return;
      this.chart.series.each((s) => {
        if (s === series) {
          s.strokeWidth = 3;
          s.opacity = 1;
        } else {
          s.strokeWidth = 1;
          s.opacity = 0.25;
        }
      });
    });

    chart.legend.itemContainers.template.events.on("out", () => {
      this.chart.series.each((s) => {
        s.strokeWidth = 2;
        s.opacity = 1;
      });
    });
  }

  componentWillUnmount() {
    if (this.chart) {
      this.chart.dispose();
    }
  }

  render() {
    const { chartId } = this.props;
    const id = chartId || "indentTrendChartDiv";
    return (
      <div
        id={id}
        style={{ width: "100%", height: "100%" }}
      />
    );
  }
}

export default IndentTrendChart;
export { buildChartData };
