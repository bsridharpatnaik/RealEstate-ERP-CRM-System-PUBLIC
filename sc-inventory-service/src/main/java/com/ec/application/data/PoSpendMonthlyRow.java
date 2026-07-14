package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One month of the spend trend. month is 'yyyy-MM'. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PoSpendMonthlyRow {
    private String month;
    private double totalValue;
    private long poCount;
}
