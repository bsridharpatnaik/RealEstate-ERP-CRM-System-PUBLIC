package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class PreviousPurchaseRateDTO {

    private String purchaseOrderId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date poDate;
    private String supplierName;
    private Double quantity;
    private Double rate;
    private Double discountPercent;
    private Double gstPercent;
    private Double netRate;
}
