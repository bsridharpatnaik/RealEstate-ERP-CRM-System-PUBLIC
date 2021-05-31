package com.ec.crm.Enums;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum CustomerStatusEnum {
    Booked,
    DocumentCollection,
    Agreement,
    Finance,
    Registry,
    Handover;
    public CustomerStatusEnum setFromString(String name)
    {
        return CustomerStatusEnum.valueOf(name);
    }

    public static List<String> getValidCustomerStatus()
    {
        List<String> customerStatuses = new ArrayList<String>();
        EnumSet.allOf(CustomerStatusEnum.class)
                .forEach(type -> {
                    customerStatuses.add(type.toString());
                });
        return customerStatuses;
    }
}
