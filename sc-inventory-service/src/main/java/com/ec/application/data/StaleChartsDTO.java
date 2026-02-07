package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StaleChartsDTO {
    List<IndentStaleBucketChartDTO> indentStaleBuckets;
    List<POStaleBucketChartDTO> poStaleBuckets;
}
