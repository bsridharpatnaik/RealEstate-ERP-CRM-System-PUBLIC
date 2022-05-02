package com.ec.crm.Data;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.crm.Enums.PaymentReceivedPaymentModeEnum;
import com.ec.crm.Enums.PaymentReceivedFromEnum;
import com.ec.crm.ReusableClasses.DynamicAuthorizationEnumJsonSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentReceivedCreateData {

    Long paymentId;

    @JsonSerialize(using= DynamicAuthorizationEnumJsonSerializer.class)
    PaymentReceivedFromEnum paymentBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date paymentReceivedDate;

    @JsonSerialize(using=DynamicAuthorizationEnumJsonSerializer.class)
    PaymentReceivedPaymentModeEnum paymentMode;

    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    Double amount;

    String bankName;
    String referenceNumber;
    Long dealStructureId;
}
