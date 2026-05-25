package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockRow {
    private String warehouseName;
    private Double quantity;
}
