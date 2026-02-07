package com.ec.application.data;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class BulkCurrentStockRequest {
    private String tenant;
    private Long warehouseId;
    private List<Long> productIds;
}