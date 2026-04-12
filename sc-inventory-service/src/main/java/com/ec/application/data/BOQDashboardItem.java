package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BOQDashboardItem {
    private String productName;
    private double consumedPercent;
    private String statusBucket; // onTrack | atRisk | exceeded
}
