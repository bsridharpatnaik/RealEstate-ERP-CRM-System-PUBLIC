package com.ec.application.data;

import lombok.Data;

import java.util.List;

@Data
public class UpdateServiceOrderLineRequest {
    private Long lineId;
    private String description;
    private Double quantity;
    private Double rate;
    private Double discountPercent;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;
    private String serviceType;
    private String assetTag;
    /** Full replacement list — sent as-is by the UI on every edit. */
    private List<CustomFieldRequest> customFields;
}
