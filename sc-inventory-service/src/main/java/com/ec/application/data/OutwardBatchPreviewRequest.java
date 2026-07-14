package com.ec.application.data;

import lombok.Data;
import java.util.List;

@Data
public class OutwardBatchPreviewRequest {
    private Long productId;
    private Long warehouseId;
    private Double quantity;
    private List<BatchOverrideEntry> overrideBatches; // null = FIFO preview
}
