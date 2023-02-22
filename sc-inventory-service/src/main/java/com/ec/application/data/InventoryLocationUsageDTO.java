package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLocationUsageDTO {
    Long locationId;
    String locationName;
    Long categoryId;
    String categoryName;
    Long productId;
    String productName;
    Double price;
    Double totalQuantity;
    Double totalPrice;
}
