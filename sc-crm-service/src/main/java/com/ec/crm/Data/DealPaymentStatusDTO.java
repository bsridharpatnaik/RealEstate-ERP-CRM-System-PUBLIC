package com.ec.crm.Data;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.crm.Model.DealStructure;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DealPaymentStatusDTO {

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double totalAmount;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double totalPaymentReceived;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double totalPaymentPending;

    public DealPaymentStatusDTO(DealStructure ds, Double totalPaymentReceived) {
        Double supplementAmount = ds.getSupplementAmount() != null ? ds.getSupplementAmount() : 0;
        this.totalAmount = ds.getDealAmount() + supplementAmount;
        this.totalPaymentReceived = totalPaymentReceived;
        this.totalPaymentPending = this.totalAmount - totalPaymentReceived;
    }
}
