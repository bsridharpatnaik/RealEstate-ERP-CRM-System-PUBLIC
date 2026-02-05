package com.ec.application.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierLeadTimeHeatmapDTO {

    private Long supplierId;
    private String supplierName;

    private Double avgLeadTimeDays;
    private String leadTimeBucket; // For color mapping

    public SupplierLeadTimeHeatmapDTO() {}

    public SupplierLeadTimeHeatmapDTO(
            Long supplierId,
            String supplierName,
            Double avgLeadTimeDays,
            String leadTimeBucket
    ) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.avgLeadTimeDays = avgLeadTimeDays;
        this.leadTimeBucket = leadTimeBucket;
    }
}
