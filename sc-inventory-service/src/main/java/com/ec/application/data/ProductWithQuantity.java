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
    // Legacy single-batch override (kept for backward compat)
    Long overrideBatchId;

    String overrideComment;

    // Multi-batch override: explicit list of {batchId, qty} — takes precedence over overrideBatchId when set
    List<BatchOverrideEntry> overrideBatches;

    // Return batch entries: explicit list of {batchId, qty} — required when multiple batches were used in the original outward
    List<BatchOverrideEntry> returnBatches;

    // Reject batch entries: explicit list of {batchId, qty} — required when multiple batches were used in the
    // original outward. Read-only attribution (reject does not move stock or batch qty), unlike returnBatches.
    List<BatchOverrideEntry> rejectBatches;

    public ProductWithQuantity() {}

    public ProductWithQuantity(Long productId, Double quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
