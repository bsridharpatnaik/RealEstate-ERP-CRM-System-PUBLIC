package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Date;

@Data
@NoArgsConstructor
public class LineItemForInwardThroughPODTO {

    @NonNull
    String lineItemCode;

    Double quantityReceived;

    Long warehouseId;

    String brand;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date expiryDate;
}

