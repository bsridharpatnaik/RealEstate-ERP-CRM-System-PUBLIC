package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SingleStockInformationDTO {
    String warehouseName;
    Double quantityInHand;
    String measurementUnit;
}
