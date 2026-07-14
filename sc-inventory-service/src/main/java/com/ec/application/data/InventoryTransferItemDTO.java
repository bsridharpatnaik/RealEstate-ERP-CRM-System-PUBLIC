package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class InventoryTransferItemDTO {

    private Long productId;
    private String productName;
    private String productCode;
    private String measurementUnit;
    private Double quantity;
    private Double sourceClosingStock;
    private Double targetClosingStock;

    // Optional: if user wants to override FIFO batch selection for this item
    private List<BatchOverrideEntry> overrideBatches;
    private String overrideComment;
}

