package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SplitLineItemRequest {
    private String lineItemCode;      // The item to split (e.g., "IND001/PROD-A")
    private Double splitQuantity;     // First part quantity (e.g., 80)

    // No need for specification, remarks etc. - they stay the same!
}