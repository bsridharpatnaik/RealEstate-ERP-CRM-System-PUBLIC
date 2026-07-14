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
    private Double totalIndented;      // = total requested (all non-cancelled/rejected lines, incl. short-closed)
    private Double totalReceived;      // SUM(quantity_received)
    private Double totalPending;       // requested - received - shortClosed
    private Double totalShortClosed;   // shortfall (requested - received) of SHORT CLOSED lines
    private Double balance;
    private Double coveragePct;
    private String bucket; // under, over, none (no BOQ)
}
