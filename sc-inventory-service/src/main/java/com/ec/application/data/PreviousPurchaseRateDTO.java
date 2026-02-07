package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class PreviousPurchaseRateDTO {

    private String purchaseOrderId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date poDate;
    private String supplierName;
    private Double rate;

    public PreviousPurchaseRateDTO(String purchaseOrderId, Date poDate, String supplierName, Double rate) {
        this.purchaseOrderId = purchaseOrderId;
        this.poDate = poDate;
        this.supplierName = supplierName;
        this.rate = rate;
    }
}
