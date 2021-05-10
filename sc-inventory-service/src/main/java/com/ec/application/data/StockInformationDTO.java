package com.ec.application.data;

import com.google.gson.JsonObject;
import lombok.Data;

import javax.persistence.Column;
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
    List<SingleStockInformationDTO> detailedStock;
}
