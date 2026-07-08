package com.ec.application.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BOQTrackerRow {
    private Long productId;
    private String productName;
    private String productCode;
    private String categoryName;
    private String unit;
    private Double boqPlanned;
    private Double totalIndented;
    private Double totalInward;
    private Double totalOutward;
    private Double boqBalance;    // boqPlanned - totalOutward
    private Double consumedPct;   // totalOutward / boqPlanned * 100; null when no BOQ
    private String bucket;        // on_track, at_risk, over, no_boq
}
