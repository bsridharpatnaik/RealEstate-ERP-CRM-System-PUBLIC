package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in the expiry-alert section of the daily stock email.
 * Populated per-tenant in StockEmailReportService and passed to the Freemarker template.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryAlertRow {
    private String projectName;
    private String productName;
    private String warehouseName;
    private String brand;
    private String lotNumber;
    private String expiryDate;   // dd-MM-yyyy (display as-is)
    private Double qtyRemaining;
    private String unit;
}
