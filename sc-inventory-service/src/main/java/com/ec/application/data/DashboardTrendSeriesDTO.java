package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTrendSeriesDTO {
    private String label; // e.g. "Created", "PO Partial"
    private List<Long> data; // aligned with time buckets
}
