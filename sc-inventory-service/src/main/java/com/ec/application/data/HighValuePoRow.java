package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/** One PO row in the high-value / filtered PO list. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HighValuePoRow {
    private String purchaseOrderId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    private Date poDate;

    private String project;
    private String supplierName;
    private String firmName;
    private String status;
    private double grandTotal;
    private boolean specialPo;
}
