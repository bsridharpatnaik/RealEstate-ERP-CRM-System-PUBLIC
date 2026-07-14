package com.ec.application.data;

import lombok.Data;

@Data
public class FinalizeLineRequest {
    private Long qcLineId;
    private Long supplierQuoteLineId;
    private String remarks;
}
