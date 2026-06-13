package com.ec.application.data;

import lombok.Data;
import java.util.List;

@Data
public class MoveFromDeadStockRequest {
    private Long targetWarehouseId;
    private Double qty;
    /** Mandatory comment explaining the reason for removing from dead stock */
    private String comment;
    /** Optional: explicit batch allocation. If omitted, FIFO is used automatically. */
    private List<BatchOverrideEntry> overrideBatches;
}
