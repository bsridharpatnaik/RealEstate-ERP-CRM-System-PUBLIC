package com.ec.application.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class LineItemForInwardThroughPODTO {

    @NonNull
    String lineItemCode;

    Double quantityReceived;

    Long warehouseId;
}

