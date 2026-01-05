package com.ec.application.data;


import lombok.Data;
import java.util.List;

@Data
public class BulkCurrentStockRequest {
    private String tenant;
    private Long warehouseId;
    private List<Long> productIds;
}