package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class BulkCurrentStockResponse {

    private String tenant;
    private Long warehouseId;

    // productId -> currentStock
    private Map<Long, Double> productStockMap;
}