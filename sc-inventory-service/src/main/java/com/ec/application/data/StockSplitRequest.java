package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class StockSplitRequest {

    private Long warehouseId;
    private List<BatchEntry> batches;

    @Data
    public static class BatchEntry {
        private Double qty;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
        private Date expiryDate;

        private String brand;

        private String lotNumber;
    }
}
