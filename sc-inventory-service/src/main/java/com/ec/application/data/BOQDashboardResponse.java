package com.ec.application.data;

import lombok.Data;
import java.util.List;

@Data
public class BOQDashboardResponse {
    private List<BOQDashboardItem> items; // products >= 70% consumed, sorted desc, top 15
    private long totalCount;
    private long onTrackCount;
    private long atRiskCount;
    private long exceededCount;
}
