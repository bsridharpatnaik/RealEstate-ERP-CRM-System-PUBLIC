package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class OpeningStockInwardDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date inwardDate;

    private List<OpeningStockLineItem> lineItems;

    @Data
    public static class OpeningStockLineItem {
        private String productName;    // changed from productId
        private String warehouseName;  // changed from warehouseId
        private Double quantity;
    }
}