package com.ec.application.data;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class PoLineItemForInward {

    private String lineItemCode;
    private String indentId;

    private Long productId;
    private String productName;
    private String productCode;
    private String measurementUnit;

    private Double orderedQuantity;
    private Double totalInwardQuantity;
    private Double pendingQuantity;

    private String remarks;

    private String specification;
}