package com.ec.application.data;

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

    private Double totalStock;   // sum across all tenants

    private List<TenantStockDTO> tenantWiseStock; // for pie chart
}

