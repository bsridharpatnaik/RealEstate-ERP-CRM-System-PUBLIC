package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockSummaryDTO {

    private String tenantSchema;
    private String tenantCode;

    private Long productId;
    private String productName;
    private String productCode;   // ✅ NEW
    private String categoryName;

    private Long warehouseId;
    private String warehouseName;
    private  String measurementUnit;
    private Double quantityInHand;
}
