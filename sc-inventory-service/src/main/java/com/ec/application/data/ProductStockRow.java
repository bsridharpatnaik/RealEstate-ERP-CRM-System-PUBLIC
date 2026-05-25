package com.ec.application.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ProductStockRow {
    private String productName;
    private String category;
    private Double totalQty;
    private String unit;
    private List<WarehouseStockRow> warehouseBreakdown; // non-zero warehouses only
}
