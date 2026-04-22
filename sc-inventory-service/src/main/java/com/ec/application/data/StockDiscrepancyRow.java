package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockDiscrepancyRow {
    private String tenant;
    private String productName;
    private double totalInward;
    private double totalTransferIn;
    private double totalOutward;
    private double totalLostDamaged;
    private double totalTransferOut;
    private double expectedStock;
    private double actualStock;
    private double discrepancy;
}
