package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeadStockDTO {

    private String tenantSchema;
    private String tenantCode;

    private Long productId;
    private String productName;
    private String productCode;   // ✅ NEW

    private Long warehouseId;
    private String warehouseName;

    private Double quantityInHand;
}
