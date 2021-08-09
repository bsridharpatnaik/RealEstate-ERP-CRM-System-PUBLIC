package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class StockPercentageForDashboard {
    String productName;
    Double currentStock;
    Double stockPercent;

    public StockPercentageForDashboard(String productName, Double currentStock, Double stockPercent) {
        this.productName = productName;
        this.currentStock = currentStock;
        this.stockPercent = stockPercent;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Double currentStock) {
        this.currentStock = currentStock==null?0:currentStock;
    }

    public Double getStockPercent() {
        return stockPercent;
    }

    public void setStockPercent(Double stockPercent) {
        this.stockPercent = stockPercent;
    }
}
