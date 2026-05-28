package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockTilesDTO {

    // Expiry
    private long expiring30Days;
    private long expiring60Days;
    private long expiredCount;

    // Stock status
    private long lowStockCount;
    private long highStockCount;

    // Material aging (days since last inward, product still has stock)
    private long aging30Days;
    private long aging60Days;
    private long aging90Days;

    // Batch-tracked products with stock not fully covered by inventory_batch records
    private long untrackedCount;
}
