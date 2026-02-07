package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LowStockItem {

    private Long productId;
    private Double availableStock;
    private Double requestedQuantity;
}