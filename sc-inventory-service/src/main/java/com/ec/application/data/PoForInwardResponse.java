package com.ec.application.data;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class PoForInwardResponse {

    private String purchaseOrderNumber;
    private Date poDate;
    private String poStatus;
    private BigDecimal grandTotal;

    private Long supplierId;
    private String supplierName;

    private String tenant;

    private List<PoLineItemForInward> lineItems;
}
