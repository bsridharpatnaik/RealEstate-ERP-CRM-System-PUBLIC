package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    Double price;
    Double totalQuantity;
    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    Double totalPrice;
}
