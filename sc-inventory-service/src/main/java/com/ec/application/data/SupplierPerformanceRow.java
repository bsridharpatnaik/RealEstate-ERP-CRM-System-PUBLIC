package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierPerformanceRow {
    private Long supplierId;
    private String supplierName;
    private Long totalPos;
    private Long completedPos;
    private Long pendingPos;
    private Long shortClosedPos;
    private Long cancelledPos;
    private Long overduePos;       // open POs past expected lead time
    private Double onTimeRate;
    private Double totalOrderValue;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private java.util.Date lastPoDate;
}
