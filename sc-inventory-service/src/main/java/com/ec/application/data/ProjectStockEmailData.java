package com.ec.application.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ProjectStockEmailData {
    private String projectName;
    private List<ProductStockRow> stockRows;   // products with total qty > 0
    private List<String> zeroStockItems;        // product names with total qty == 0
}
