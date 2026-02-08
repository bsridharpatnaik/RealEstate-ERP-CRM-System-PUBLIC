package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardProductStockDTO {

    private Long productId;
    private String productCode;
    private String productName;
    private String measurementUnit;

    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double totalStock;   // sum across all tenants

    private List<TenantStockDTO> tenantWiseStock; // for pie chart
}

