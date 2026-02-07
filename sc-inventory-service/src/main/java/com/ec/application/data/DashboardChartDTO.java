package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DashboardChartDTO {
    private Long totalCount;
    private List<TenantCountDTO> tenantCounts;
}
