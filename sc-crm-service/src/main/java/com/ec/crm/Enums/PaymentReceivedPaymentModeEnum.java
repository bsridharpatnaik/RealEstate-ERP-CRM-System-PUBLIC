package com.ec.crm.Enums;

import com.ec.crm.ReusableClasses.DynamicAuthorizationEnumJsonSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonSerialize(using= DynamicAuthorizationEnumJsonSerializer.class)
public enum PaymentReceivedPaymentModeEnum {
    Cheque,
    Cash,
    UPI,
    BankTransfer,
    Other;

    public PaymentReceivedPaymentModeEnum setFromString(String name) {
        return PaymentReceivedPaymentModeEnum.valueOf(name);
    }

    public static List<String> getValidPaymentReceivedTypeEnumValues() {
        List<String> validPaymentReceivedTypeEnums = new ArrayList<String>();
        EnumSet.allOf(PaymentReceivedPaymentModeEnum.class).forEach(type -> validPaymentReceivedTypeEnums.add(type.toString()));
        return validPaymentReceivedTypeEnums;
    }
}
