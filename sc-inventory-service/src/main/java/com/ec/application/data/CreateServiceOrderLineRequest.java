package com.ec.application.data;

import lombok.Data;

import java.util.List;

@Data
public class CreateServiceOrderLineRequest {
    private String description;
    private Double quantity;
    private Double rate;
    private Double discountPercent;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;
    private String serviceType;
    private String assetTag;
    /** Any number of scenario-specific key/value pairs, e.g. {label:"Odometer Reading", value:"45000 km"}. */
    private List<CustomFieldRequest> customFields;
}
