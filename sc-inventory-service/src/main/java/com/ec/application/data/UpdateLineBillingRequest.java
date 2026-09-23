package com.ec.application.data;

import lombok.Data;

/**
 * Edits the billing unit conversion of a single PO line. Rate is per billing unit when
 * billingUnit is set, otherwise per base unit. netRate/totalAmount are recomputed server-side.
 */
@Data
public class UpdateLineBillingRequest {
    private Double rate;
    private String billingUnit;             // null clears conversion → bill in base unit
    private Double billingQuantity;         // qty in billing unit (baseQty / conversionFactor)
    private Double billingConversionFactor; // stored for display/PDF
}
