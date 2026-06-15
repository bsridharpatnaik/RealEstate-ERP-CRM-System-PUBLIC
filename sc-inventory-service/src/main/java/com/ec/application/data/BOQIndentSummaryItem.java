package com.ec.application.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BOQIndentSummaryItem {
    private String categoryName;
    private String productName;
    private String productCode;
    private String unit;
    private Double boqPlanned;
    private Double totalIndented;
    private Double balance;
    private Double coveragePct;
    private String bucket; // under, over, none (no BOQ)
}
