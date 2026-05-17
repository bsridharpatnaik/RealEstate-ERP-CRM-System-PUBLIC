import React, { Component } from "react";
import * as am4core from "@amcharts/amcharts4/core";
import * as am4charts from "@amcharts/amcharts4/charts";
import am4themes_animated from "@amcharts/amcharts4/themes/animated";
import { Skeleton } from "@material-ui/lab";
import "./style.scss";

am4core.useTheme(am4themes_animated);

const COLORS = {
  onTrack:  "#44c4a1",
  atRisk:   "#f9a825",
  exceeded: "#e57373",
};

class BOQStatusChart extends Component {
  componentDidMount() {
    if (this.props.isLoaded) this.buildChart();
  }

  componentDidUpdate(prev) {
    if (!prev.isLoaded && this.props.isLoaded) this.buildChart();
  }

  componentWillUnmount() {
    if (this.chart) this.chart.dispose();
  }

  buildChart() {
    if (this.chart) this.chart.dispose();

    const { items = [] } = this.props.data || {};
    if (items.length === 0) return;

    // AmCharts needs chart data reversed so highest value appears at top
    const chartData = [...items].reverse();

    let chart = am4core.create("boqStatusChartDiv", am4charts.XYChart);
    chart.data = chartData;
    chart.responsive.enabled = true;
    chart.paddingRight = 40;

    // Y axis — product names
    let categoryAxis = chart.yAxes.push(new am4charts.CategoryAxis());
    categoryAxis.dataFields.category = "productName";
    categoryAxis.renderer.grid.template.disabled = true;
    categoryAxis.renderer.labels.template.fontSize = 11;
    categoryAxis.renderer.labels.template.truncate = true;
    categoryAxis.renderer.labels.template.maxWidth = 140;
    categoryAxis.renderer.labels.template.tooltipText = "{category}";
    categoryAxis.renderer.minGridDistance = 10;

    // X axis — consumed %
    let valueAxis = chart.xAxes.push(new am4charts.ValueAxis());
    valueAxis.min = 0;
    valueAxis.strictMinMax = true;
    valueAxis.renderer.labels.template.fontSize = 10;
    valueAxis.title.text = "Consumed %";
    valueAxis.title.fontSize = 10;

    // 80% reference line
    let range80 = valueAxis.axisRanges.create();
    range80.value = 80;
    range80.grid.stroke = am4core.color("#f9a825");
    range80.grid.strokeDasharray = "4,4";
    range80.grid.strokeWidth = 1;
    range80.grid.strokeOpacity = 0.8;
    range80.label.text = "80%";
    range80.label.fontSize = 9;
    range80.label.fill = am4core.color("#f9a825");
    range80.label.dy = -6;

    // 100% reference line
    let range100 = valueAxis.axisRanges.create();
    range100.value = 100;
    range100.grid.stroke = am4core.color("#e57373");
    range100.grid.strokeDasharray = "4,4";
    range100.grid.strokeWidth = 1;
    range100.grid.strokeOpacity = 0.8;
    range100.label.text = "100%";
    range100.label.fontSize = 9;
    range100.label.fill = am4core.color("#e57373");
    range100.label.dy = -6;

    // Series
    let series = chart.series.push(new am4charts.ColumnSeries());
    series.dataFields.valueX = "consumedPercent";
    series.dataFields.categoryY = "productName";
    series.columns.template.height = am4core.percent(60);
    series.columns.template.tooltipText = "{categoryY}\n{consumedPercent}% consumed";
    series.columns.template.adapter.add("fill", (fill, target) => {
      const bucket = target.dataItem && target.dataItem.dataContext
        ? target.dataItem.dataContext.statusBucket
        : "onTrack";
      return am4core.color(COLORS[bucket] || COLORS.onTrack);
    });
    series.columns.template.adapter.add("stroke", (stroke, target) => {
      const bucket = target.dataItem && target.dataItem.dataContext
        ? target.dataItem.dataContext.statusBucket
        : "onTrack";
      return am4core.color(COLORS[bucket] || COLORS.onTrack);
    });

    // Value label at end of bar
    let bullet = series.bullets.push(new am4charts.LabelBullet());
    bullet.label.text = "{consumedPercent}%";
    bullet.label.fontSize = 10;
    bullet.label.dx = 4;
    bullet.label.horizontalCenter = "left";
    bullet.label.adapter.add("fill", (fill, target) => {
      const bucket = target.dataItem && target.dataItem.dataContext
        ? target.dataItem.dataContext.statusBucket
        : "onTrack";
      return am4core.color(COLORS[bucket] || COLORS.onTrack);
    });

    this.chart = chart;
  }

  render() {
    const { isLoaded, data = {} } = this.props;
    const { items = [], totalCount = 0, onTrackCount = 0, atRiskCount = 0, exceededCount = 0 } = data;

    const badgeStyle = (color) => ({
      display: 'inline-flex', alignItems: 'center', gap: '6px',
      padding: '3px 10px', borderRadius: '12px',
      background: color + '18', border: `1px solid ${color}`,
      fontSize: '11px', fontWeight: 600, color, marginRight: '8px',
    });

    return (
      <React.Fragment>
        <div className="dashboard-heading" style={{ height: 'auto', marginBottom: '6px' }}>
          BOQ Consumption Status
        </div>

        {!isLoaded ? (
          <React.Fragment>
            <Skeleton variant="rect" height={30} style={{ marginBottom: 6 }} />
            {[...Array(6)].map((_, i) => (
              <Skeleton key={i} variant="rect" height={22} style={{ marginBottom: 5 }} />
            ))}
          </React.Fragment>
        ) : (
          <React.Fragment>
            {/* Summary badges */}
            <div style={{ marginBottom: '10px', display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
              <span style={{ ...badgeStyle('#5c6bc0') }}>
                <span>{totalCount}</span> Total
              </span>
              <span style={{ ...badgeStyle(COLORS.onTrack) }}>
                <span>{onTrackCount}</span> On Track
              </span>
              <span style={{ ...badgeStyle(COLORS.atRisk) }}>
                <span>{atRiskCount}</span> At Risk
              </span>
              <span style={{ ...badgeStyle(COLORS.exceeded) }}>
                <span>{exceededCount}</span> Exceeded
              </span>
            </div>

            {items.length === 0 ? (
              <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                height: 'calc(100% - 60px)', flexDirection: 'column', color: '#44c4a1'
              }}>
                <span style={{ fontSize: '32px' }}>✓</span>
                <span style={{ fontSize: '13px', marginTop: '8px', color: '#888' }}>
                  All items are on track (&lt;70% consumed)
                </span>
              </div>
            ) : (
              <div className="chart" style={{ marginTop: '4px' }}>
                <div className="chart-wrapper">
                  <div
                    id="boqStatusChartDiv"
                    style={{ width: '100%', height: '100%', minHeight: `${Math.max(items.length * 30 + 40, 120)}px` }}
                  />
                </div>
              </div>
            )}

            {items.length > 0 && (
              <div style={{ fontSize: '10px', color: '#aaa', textAlign: 'right', marginTop: '4px' }}>
                Showing top {items.length} items ≥ 70% consumed
              </div>
            )}
          </React.Fragment>
        )}
      </React.Fragment>
    );
  }
}

export default BOQStatusChart;
