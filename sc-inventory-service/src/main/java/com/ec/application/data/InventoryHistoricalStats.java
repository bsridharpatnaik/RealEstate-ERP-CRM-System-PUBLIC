package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHistoricalStats {
    String productName;
    String measurementUnit;
    TimelyProductStatsForDashboard inward;
    TimelyProductStatsForDashboard outward;
}
