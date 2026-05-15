package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllProductsStockSummaryDTO {
    private Long productId;
    private String productName;
    private String measurementUnit;
    private List<WarehouseStockEntry> warehouseStocks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseStockEntry {
        private Long warehouseId;
        private String warehouseName;
        @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
        private Double stock;
    }
}
