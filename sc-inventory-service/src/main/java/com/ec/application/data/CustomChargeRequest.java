package com.ec.application.data;

import lombok.Data;

@Data
public class CustomChargeRequest {
    private String chargeName;
    private Double chargeAmount;
    private Double chargeGstPercent;
    private Double totalChargeAmount;
}
