package com.ec.application.data;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentStockResponse {

    private String tenant;
    private Long productId;
    private Long warehouseId;
    private Double currentStock;
}