package com.ec.application.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PriceScatterPointDTO {

    private String x; // date (yyyy-MM-dd)
    private Double y; // rate

    private String supplierName;
    private String purchaseOrderId;

    public PriceScatterPointDTO(
            String x,
            Double y,
            String supplierName,
            String purchaseOrderId
    ) {
        this.x = x;
        this.y = y;
        this.supplierName = supplierName;
        this.purchaseOrderId = purchaseOrderId;
    }

    // getters
}