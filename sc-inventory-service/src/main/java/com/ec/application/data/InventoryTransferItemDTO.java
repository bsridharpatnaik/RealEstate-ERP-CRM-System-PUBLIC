package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class InventoryTransferItemDTO {

    private Long productId;
    private String productName;
    private String productCode;
    private String measurementUnit;
    private Double quantity;
    private Double sourceClosingStock;
    private Double targetClosingStock;
}

