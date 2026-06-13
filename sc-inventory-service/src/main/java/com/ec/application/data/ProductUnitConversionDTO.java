package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductUnitConversionDTO {
    private Long id;
    private Long productId;
    private String unitName;
    private Double conversionFactor;
    private boolean usedInPo;
}
