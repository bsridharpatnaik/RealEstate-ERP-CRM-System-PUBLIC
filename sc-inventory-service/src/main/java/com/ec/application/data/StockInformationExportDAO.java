package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.model.Stock;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class StockInformationExportDAO {

    Long productId;
    String inventory;
    String category;
    String totalStock;
    String warehouse;
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double warehouseStock;
    String measurementUnit;
    Double reorderQuantity;
    String stockStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") // Adjusted to match DATE type
    Date lastInwardDate;

    public StockInformationExportDAO(SingleStockInfo ssi, Stock stock) {
        this.productId = ssi.getProductId();
        this.inventory = ssi.getProductName();
        this.category = ssi.getCategoryName();
        this.totalStock = ssi.getTotalQuantityInHand();
        this.warehouse = stock.getWarehouse().getWarehouseName();
        this.warehouseStock = stock.getQuantityInHand();
        this.measurementUnit = stock.getProduct().getMeasurementUnit();
        this.reorderQuantity = stock.getProduct().getReorderQuantity();
        boolean neverLowStock = this.reorderQuantity == null || this.reorderQuantity <= 0;
        this.stockStatus = neverLowStock || stock.getQuantityInHand() > this.reorderQuantity ? "High" : "Low";
        this.lastInwardDate = ssi.getLastInwardDate();
    }
}
