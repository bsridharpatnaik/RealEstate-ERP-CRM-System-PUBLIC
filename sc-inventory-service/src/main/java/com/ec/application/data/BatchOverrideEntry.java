package com.ec.application.data;

import lombok.Data;

@Data
public class BatchOverrideEntry {
    private Long batchId;
    private Double qty;
    // Populated server-side when serializing to batchEntriesJson
    private String brand;
    private String lotNumber;
    private String expiryDate;
}
