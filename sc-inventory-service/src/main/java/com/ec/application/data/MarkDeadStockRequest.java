package com.ec.application.data;

import lombok.Data;
import java.util.List;

@Data
public class MarkDeadStockRequest {
    private Long sourceWarehouseId;
    private Double qty;
    /** Mandatory comment explaining why stock is being marked as dead */
    private String comment;
    /** Optional: explicit batch allocation. If omitted, FIFO is used automatically. */
    private List<BatchOverrideEntry> overrideBatches;
}
