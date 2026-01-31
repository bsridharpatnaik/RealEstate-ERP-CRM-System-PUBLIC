package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd MMM yyyy, hh:mm a",
            timezone = "Asia/Kolkata"
    )
    private Date syncedAt;            // MAX
}
