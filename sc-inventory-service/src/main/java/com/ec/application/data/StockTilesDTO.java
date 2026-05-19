package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTilesDTO {

    private long expiring30Days;
    private long expiring60Days;
    private long expiredCount;
}
