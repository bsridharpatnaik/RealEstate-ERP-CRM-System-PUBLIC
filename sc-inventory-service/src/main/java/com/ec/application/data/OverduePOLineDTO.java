package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single overdue PO line item for the dashboard widget.
 * Populated via native SQL in PurchaseOrderService.getOverdueLines().
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverduePOLineDTO {

    private String purchaseOrderId;
    private String poDate;          // dd-MM-yyyy
    private String projectName;
    private String supplierName;
    private String productName;
    private String productCode;
    private String measurementUnit;
    private Integer leadTimeDays;
    private Integer daysOverdue;    // positive = how many days past deadline
}
