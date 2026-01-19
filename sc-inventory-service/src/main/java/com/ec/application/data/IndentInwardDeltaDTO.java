package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndentInwardDeltaDTO {

    private String lineItemCode;

    /**
     * +ve → inward added / increased
     * -ve → inward reduced / deleted
     */
    private Double finalQuantity;
}