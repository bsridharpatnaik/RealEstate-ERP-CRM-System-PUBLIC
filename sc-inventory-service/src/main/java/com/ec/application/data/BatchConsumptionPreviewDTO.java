package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BatchConsumptionPreviewDTO {

    private Long productId;
    private List<BatchPreviewItem> batches;

    @Data
    public static class BatchPreviewItem {
        private Long batchId;
        private String brand;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
        private Date expiryDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
        private Date receivedDate;

        private Double qtyConsumed;
        private Double qtyAvailable;
        private boolean fifoOverridden;
    }
}
