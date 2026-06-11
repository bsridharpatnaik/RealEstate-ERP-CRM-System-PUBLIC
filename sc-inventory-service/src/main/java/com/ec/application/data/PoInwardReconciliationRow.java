package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PoInwardReconciliationRow {

    private String project;
    private String purchaseOrderId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    private Date poDate;

    private String poStatus;
    private String supplier;
    private String productName;
    private String productCode;
    private String unit;

    private Double orderedQty;
    private Double receivedQty;
    private Double balanceQty;
    private Double percentReceived;

    /** Derived status for quick filtering. */
    private String reconciliationStatus; // COMPLETE, PARTIAL, NOT_STARTED, EXCESS

    /** Effective lead time in calendar days (product-level → category-level → null). */
    private Integer leadTimeDays;

    /** Positive = days past deadline; null when no lead time or PO is terminal. */
    private Integer daysOverdue;

    /** True when daysOverdue > 0 (lead time exceeded and PO still open). */
    private Boolean isOverdue;
}
