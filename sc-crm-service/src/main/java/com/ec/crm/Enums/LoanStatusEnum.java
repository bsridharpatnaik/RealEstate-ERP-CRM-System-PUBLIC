package com.ec.crm.Enums;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum LoanStatusEnum {
    DocumentCollection,
    ApplicationSubmitted,
    ApplicationProcessing,
    Approved,
    Disbursed;

    public LoanStatusEnum setFromString(String name)
    {
        return LoanStatusEnum.valueOf(name);
    }

    public static List<String> getValidLoanStatus()
    {
        List<String> loanStatuses = new ArrayList<String>();
        EnumSet.allOf(LoanStatusEnum.class)
                .forEach(type -> {
                    loanStatuses.add(type.toString());
                });
        return loanStatuses;
    }
}
