package com.ec.application.data;

import lombok.Data;

import java.util.List;

@Data
public class TransferBatchPreviewRequest {

    private String sourceTenant;
    private Long productId;
    private Long sourceWarehouseId;
    private Double qty;

    // Optional: if provided, shows preview of these specific batches instead of FIFO
    private List<BatchOverrideEntry> overrideBatches;
}
