package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class InwardBatchSplit {

    private Double qty;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date expiryDate;

    private String brand;

    private String lotNumber;

    /**
     * Only set on the reduce path (inward edit, qty decrease, multi-batch).
     * Identifies which existing InventoryBatch to reduce from.
     * Null on the increase/create path — new batches are matched by metadata.
     */
    private Long batchId;
}
