package com.ec.application.data;

import com.ec.application.model.AllInventoryTransactions;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.JsonObject;
import lombok.Data;

import javax.persistence.Column;
import java.util.Date;
import java.util.List;

@Data
public class StockInformationDTO {
    Long productId;
    String productName;
    Double reorderQuantity;
    String measurementUnit;
    String categoryName;
    Double totalQuantityInHand;
    String stockStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") // Adjusted to match DATE type
    Date lastInwardDate;
    List<SingleStockInformationDTO> detailedStock;
    List<AllInventoryTransactions> inwardOutwardHistory;

    public void updateDetailedStock(List<SingleStockInformationDTO> detailedStock, StockAgingData stockAgingDataMap) {
        for(SingleStockInformationDTO dStock : detailedStock){
            String warehouseName = dStock.getWarehouseName();
            dStock.setStockAgingData(stockAgingDataMap.getStockAge().get(warehouseName));
        }
    }
}
