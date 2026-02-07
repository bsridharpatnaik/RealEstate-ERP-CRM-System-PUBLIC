package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductStockSumDTO {
    private Long productId;
    private Double totalQuantity;
}
