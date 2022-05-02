package com.ec.crm.Enums;

import com.ec.crm.ReusableClasses.DynamicAuthorizationEnumJsonSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonSerialize(using= DynamicAuthorizationEnumJsonSerializer.class)
public enum PaymentReceivedFromEnum {
    Customer,
    Bank;

    public PaymentReceivedFromEnum setFromString(String name) {
        return PaymentReceivedFromEnum.valueOf(name);
    }

    public static List<String> getValidPaymentReceivedTypeEnumValues() {
        List<String> validPaymentReceivedTypeEnums = new ArrayList<String>();
        EnumSet.allOf(PaymentReceivedFromEnum.class).forEach(type -> validPaymentReceivedTypeEnums.add(type.toString()));
        return validPaymentReceivedTypeEnums;
    }
}
