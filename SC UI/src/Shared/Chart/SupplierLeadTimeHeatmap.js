import React, { Component } from "react";
import * as am4core from "@amcharts/amcharts4/core";
import * as am4charts from "@amcharts/amcharts4/charts";
import am4themes_animated from "@amcharts/amcharts4/themes/animated";

am4core.useTheme(am4themes_animated);

const BUCKET_ORDER = [
  "WITHIN_1_DAY",
  "TWO_TO_FOUR_DAYS",
  "FOUR_TO_TEN_DAYS",
  "TEN_PLUS_DAYS",
];

const BUCKET_LABELS = {
  WITHIN_1_DAY: "Within 1 Day",
  TWO_TO_FOUR_DAYS: "2-4 Days",
  FOUR_TO_TEN_DAYS: "4-10 Days",
  TEN_PLUS_DAYS: "10+ Days",
};

const BUCKET_COLORS = {
  WITHIN_1_DAY: "#44c4a1",
  TWO_TO_FOUR_DAYS: "#85CDCA",
  FOUR_TO_TEN_DAYS: "#f9b571",
  TEN_PLUS_DAYS: "#FF808B",
};

/**
 * Build a lookup map: key = "supplierName|bucketLabel" -> { avgLeadTimeDays, bucketLabel }
 * so the in-tile label can show avgLeadTimeDays even when the chart doesn't pass custom fields.
 */
function buildTooltipLookup(items = []) {
  const lookup = {};

  items.forEach((item) => {
    const name = item.supplierName || `Supplier ${item.supplierId}`;
    const bucket = item.leadTimeBucket || "TEN_PLUS_DAYS";
    const bucketLabel = BUCKET_LABELS[bucket] || bucket;
    const key = `${name}|${bucketLabel}`;
    const avg = Number(item.avgLeadTimeDays);
    const avgLeadTimeDays = !Number.isNaN(avg) ? avg.toFixed(1) : "-";

    lookup[key] = {
      avgLeadTimeDays,
      bucketLabel,
    };
  });

  return lookup;
}

/**
 * Transform API response into treemap data.
 * API: [{ supplierId, supplierName, avgLeadTimeDays, leadTimeBucket }, ...]
 * Output: [{ name, value, color, children: [{ name: supplierName, value: 1 }, ...] }, ...]
 */
function buildTreemapData(items = []) {
  const byBucket = {};
  BUCKET_ORDER.forEach((b) => {
    byBucket[b] = [];
  });
  items.forEach((item) => {
    const bucket = item.leadTimeBucket || "TEN_PLUS_DAYS";
    if (!byBucket[bucket]) byBucket[bucket] = [];
    byBucket[bucket].push(item);
  });

  const bucketColor = (b) => am4core.color(BUCKET_COLORS[b] || "#a2a2c3");
  return BUCKET_ORDER.filter((b) => byBucket[b].length > 0).map((bucket) => {
    const list = byBucket[bucket];
    const color = bucketColor(bucket);
    const children = list.map((item) => {
      const avgLeadTimeDays = Number(item.avgLeadTimeDays);
      return {
        name: item.supplierName || `Supplier ${item.supplierId}`,
        value: Math.max(1, Math.ceil(avgLeadTimeDays || 1)),
        avgLeadTimeDays: avgLeadTimeDays,
        avgDays: avgLeadTimeDays != null && !Number.isNaN(avgLeadTimeDays)
          ? avgLeadTimeDays.toFixed(1)
          : "-",
        bucketLabel: BUCKET_LABELS[bucket] || bucket,
        color,
      };
    });
    const value = children.reduce((sum, c) => sum + c.value, 0);
    return {
      name: BUCKET_LABELS[bucket] || bucket,
      value,
      color,
      children,
    };
  });
}

class SupplierLeadTimeHeatmap extends Component {
  static defaultProps = {
    chartId: "supplierLeadTimeHeatmapDiv",
  };

  componentDidMount() {
    const { data, chartId } = this.props;
    const treemapData = buildTreemapData(data);
    if (!treemapData.length) return;

    const tooltipLookup = buildTooltipLookup(data || []);

    const id = chartId || "supplierLeadTimeHeatmapDiv";
    const chart = am4core.create(id, am4charts.TreeMap);
    chart.responsive.enabled = true;
    chart.data = treemapData;
    chart.dataFields.value = "value";
    chart.dataFields.name = "name";
    chart.dataFields.children = "children";
    chart.dataFields.color = "color";
    chart.zoomable = false;
    chart.maxLevels = 2;
    chart.colors.step = 2;

    chart.legend = new am4charts.Legend();
    chart.legend.position = "top";
    chart.legend.contentAlign = "left";
    chart.legend.marginBottom = 8;
    chart.legend.itemContainers.template.paddingTop = 4;
    chart.legend.itemContainers.template.paddingBottom = 4;
    chart.legend.dataFields.name = "name";
    chart.legend.markers.template.propertyFields.fill = "fill";
    chart.legend.markers.template.width = 14;
    chart.legend.markers.template.height = 14;
    const bucketLegendData = BUCKET_ORDER.map((bucket) => ({
      name: BUCKET_LABELS[bucket],
      fill: am4core.color(BUCKET_COLORS[bucket]),
    }));
    chart.legend.data = bucketLegendData;
    chart.feedLegend = function () {
      if (this.legend) {
        this.legend.dataFields.name = "name";
        this.legend.markers.template.propertyFields.fill = "fill";
        this.legend.data = bucketLegendData;
      }
    };

    chart.legend.itemContainers.template.interactionsEnabled = false;
    chart.legend.itemContainers.template.cursorOverStyle = am4core.MouseCursorStyle.default;

    const level0 = chart.seriesTemplates.create("0");
    level0.columns.template.column.cornerRadius(8, 8, 8, 8);
    level0.columns.template.fillOpacity = 0;
    level0.columns.template.strokeWidth = 2;
    level0.columns.template.strokeOpacity = 0.3;
    level0.columns.template.tooltipText = "{name}";

    const level1 = chart.seriesTemplates.create("1");
    level1.columns.template.column.cornerRadius(6, 6, 6, 6);
    level1.columns.template.fillOpacity = 1;
    level1.columns.template.strokeWidth = 2;
    level1.columns.template.stroke = am4core.color("#ffffff");
    level1.columns.template.tooltipText = "{parentName}\nAvg lead time: {avgDays} days";
    const bullet = level1.bullets.push(new am4charts.LabelBullet());
    bullet.locationY = 0.5;
    bullet.locationX = 0.5;
    bullet.label.fill = am4core.color("#ffffff");
    bullet.label.fontSize = 11;
    bullet.label.truncate = true;
    bullet.label.maxWidth = 120;
    bullet.label.wrap = true;
    bullet.label.textAlign = "middle";
    bullet.label.adapter.add("text", (text, target) => {
      let dataItem = null;
      let el = target.parent;
      while (el) {
        if (el.dataItem) {
          dataItem = el.dataItem;
          break;
        }
        el = el.parent;
      }
      const treeMapDataItem = dataItem?.dataContext || dataItem?.treeMapDataItem;
      if (!treeMapDataItem) return "";
      const name = treeMapDataItem.name || "-";
      const bucketLabel = treeMapDataItem.parent ? treeMapDataItem.parent.name : "-";
      const key = `${name}|${bucketLabel}`;
      const lookup = tooltipLookup[key];
      const avgDays = lookup && lookup.avgLeadTimeDays != null ? lookup.avgLeadTimeDays : "-";
      // return `${name}\n${avgDays} days`;
      return `${name}`;
    });

    this.chart = chart;
  }

  componentWillUnmount() {
    if (this.chart) {
      this.chart.dispose();
    }
  }

  render() {
    const { chartId } = this.props;
    const id = chartId || "supplierLeadTimeHeatmapDiv";
    return (
      <div
        id={id}
        style={{ width: "100%", height: "100%" }}
      />
    );
  }
}

export default SupplierLeadTimeHeatmap;
export { buildTreemapData, buildTooltipLookup, BUCKET_LABELS, BUCKET_COLORS };
