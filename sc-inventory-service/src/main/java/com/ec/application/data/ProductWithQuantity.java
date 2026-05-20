package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.lang.NonNull;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ProductWithQuantity {
    @NonNull
    Long productId;

    @NonNull
    Double quantity;

    Long warehouseId;

    String remarks;

    // Inward batch fields (nullable — only set during inward, ignored elsewhere)
    String brand;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date expiryDate;

    // Multiple batch splits (expirable products) — if set, overrides single expiryDate
    List<InwardBatchSplit> batchSplits;

    // Outward FIFO override fields (nullable — only set during outward, ignored elsewhere)
    Long overrideBatchId;

    String overrideComment;

    public ProductWithQuantity() {}

    public ProductWithQuantity(Long productId, Double quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
