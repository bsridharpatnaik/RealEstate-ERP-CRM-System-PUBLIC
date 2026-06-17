package com.ec.application.data;

import lombok.Data;

@Data
public class LinkQuoteToPoRequest {
    private Long supplierQuoteLineId;
    private Long qcLineId;
    private String qcId;
    private String purchaseOrderId;
    private Long poLineId;
}
