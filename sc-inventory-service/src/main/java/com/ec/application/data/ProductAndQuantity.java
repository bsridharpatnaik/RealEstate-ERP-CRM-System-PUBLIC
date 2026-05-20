package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ProductAndQuantity {
    Long productId;
    Double quantity;

    // Batch fields for expirable products — required when qty increases
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date expiryDate;

    String brand;

    // Multiple splits — if set, overrides single expiryDate
    List<InwardBatchSplit> batchSplits;
}
