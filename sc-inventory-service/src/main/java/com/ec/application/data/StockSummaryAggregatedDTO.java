package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double quantityInHand;   // SUM
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double deadStock;
    private String measurementUnit;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd MMM yyyy, hh:mm a",
            timezone = "Asia/Kolkata"
    )
    private Date syncedAt;            // MAX
}
