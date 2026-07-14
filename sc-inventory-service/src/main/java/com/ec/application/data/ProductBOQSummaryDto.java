package com.ec.application.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductBOQSummaryDto {
    private boolean hasBOQ;
    private Double totalPlanned;
    private Double totalConsumed;
    private Double remaining;
    private String bucket; // onTrack, atRisk, exceeded
}
