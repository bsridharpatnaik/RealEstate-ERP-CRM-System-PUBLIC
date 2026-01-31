package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
public class StockSummaryAggregatedDTO {

    private String tenantSchema;
    private Long productId;
    private String productCode;
    private String productName;
    private Double quantityInHand;   // SUM
    private String measurementUnit;

    private Date syncedAt;            // MAX
}
