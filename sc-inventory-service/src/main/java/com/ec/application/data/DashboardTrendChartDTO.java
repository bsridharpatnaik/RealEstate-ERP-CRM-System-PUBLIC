package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTrendChartDTO {
    private String title;                 // "Indent Lifecycle Trend"
    private List<String> periods;         // ["01 Sep", "02 Sep", ...]
    private List<DashboardTrendSeriesDTO> series;
}
