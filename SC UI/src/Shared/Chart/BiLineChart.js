import React, { Component } from "react";
import * as am4core from "@amcharts/amcharts4/core";
import * as am4charts from "@amcharts/amcharts4/charts";
import am4themes_animated from "@amcharts/amcharts4/themes/animated";

am4core.useTheme(am4themes_animated);

class BiLineChart extends Component {
    componentDidMount() {
        const { data, xAxisField, seriesOneField, seriesTwoField, seriesOneLabel, seriesTwoLabel } = this.props;
        let chart = am4core.create("bilinechartdiv", am4charts.XYChart);

        chart.colors.step = 2;
        chart.maskBullets = false;
        chart.responsive.enabled = true;


        chart.data = data

        // Create axes
        let xAxis = chart.xAxes.push(new am4charts.CategoryAxis());
        xAxis.dataFields.category = xAxisField
        xAxis.renderer.grid.template.location = 0;
        xAxis.renderer.minGridDistance = 50;
        xAxis.renderer.inside = true;
        xAxis.renderer.grid.template.disabled = true;
        xAxis.renderer.fullWidthTooltip = true;

        let labelTemplate = xAxis.renderer.labels.template;
        labelTemplate.visible = false;
        labelTemplate.rotation = -45;
        labelTemplate.horizontalCenter = "left";
        labelTemplate.verticalCenter = "bottom";
        labelTemplate.truncate = true;
        labelTemplate.width = 180;
        labelTemplate.inside = false;

        let countAxis = chart.yAxes.push(new am4charts.ValueAxis());

        // Create series
        let firstSeries = chart.series.push(new am4charts.LineSeries());
        firstSeries.dataFields.valueY = seriesOneField;
        firstSeries.dataFields.categoryX = xAxisField;
        firstSeries.yAxis = countAxis;
        firstSeries.name = seriesOneLabel;
        firstSeries.strokeWidth = 2;
        firstSeries.stroke = "#006400"
        firstSeries.fill = "#006400"
        firstSeries.propertyFields.strokeDasharray = "dashLength";
        firstSeries.tooltipText = `${seriesOneLabel}: {valueY}`;
        firstSeries.showOnInit = true;

        let firstSeriesBullet = firstSeries.bullets.push(new am4charts.Bullet());
        let firstSeriesRectangle = firstSeriesBullet.createChild(am4core.Rectangle);
        firstSeriesBullet.horizontalCenter = "middle";
        firstSeriesBullet.verticalCenter = "middle";
        firstSeriesBullet.width = 7;
        firstSeriesBullet.height = 7;
        firstSeriesRectangle.width = 7;
        firstSeriesRectangle.height = 7;
        firstSeriesRectangle.stroke = "#006400"

        let firstSeriesState = firstSeriesBullet.states.create("hover");
        firstSeriesState.properties.scale = 1.2;

        let secondSeries = chart.series.push(new am4charts.LineSeries());
        secondSeries.dataFields.valueY = seriesTwoField;
        secondSeries.dataFields.categoryX = xAxisField;
        secondSeries.yAxis = countAxis;
        secondSeries.name = seriesTwoLabel;
        secondSeries.strokeWidth = 2;
        secondSeries.stroke = "#EE870D"
        secondSeries.fill = "#EE870D"
        secondSeries.propertyFields.strokeDasharray = "dashLength";
        secondSeries.tooltipText = `${seriesTwoLabel}: {valueY}`;
        secondSeries.showOnInit = true;

        let secondSeriesBullet = secondSeries.bullets.push(new am4charts.CircleBullet());
        secondSeriesBullet.circle.fill = am4core.color("#fff");
        secondSeriesBullet.circle.strokeWidth = 2;
        secondSeriesBullet.circle.stroke = "#EE870D";
        secondSeriesBullet.circle.propertyFields.radius = 5;

        let secondSeriesState = secondSeriesBullet.states.create("hover");
        secondSeriesState.properties.scale = 1.2;

        // Add legend
        chart.legend = new am4charts.Legend();

        // Add cursor
        chart.cursor = new am4charts.XYCursor();
        chart.cursor.fullWidthLineX = true;
        chart.cursor.xAxis = xAxis;
        chart.cursor.lineX.strokeOpacity = 0;
        chart.cursor.lineX.fill = am4core.color("#000");
        chart.cursor.lineX.fillOpacity = 0.1;
        this.chart = chart;
    }
    componentWillUnmount() {
        if (this.chart) {
            this.chart.dispose();
        }
    }

    render() {
        return <div id="bilinechartdiv" style={{ width: "100%", height: "100%" }}></div>;
    }
}

export default BiLineChart;