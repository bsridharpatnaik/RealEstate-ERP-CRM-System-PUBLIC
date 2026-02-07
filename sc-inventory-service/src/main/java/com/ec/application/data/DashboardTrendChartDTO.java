package com.ec.application.data;

import lombok.Data;

import java.util.List;

@Data
public class DashboardTrendChartDTO {
    private String title;                 // "Indent Lifecycle Trend"
    private List<String> periods;         // ["01 Sep", "02 Sep", ...]
    private List<DashboardTrendSeriesDTO> series;
}
