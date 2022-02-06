package com.ec.application.comparators;

import com.ec.application.data.InventoryReportByDate;

import java.util.Comparator;

public class MonthlyReportComparator implements Comparator<InventoryReportByDate> {

    @Override
    public int compare(InventoryReportByDate o1, InventoryReportByDate o2) {

        if(o1.getMonth().compareTo(o2.getMonth()) !=0 )
            return o1.getMonth().compareTo(o2.getMonth());

        if(o1.getCategory_name().compareTo(o2.getCategory_name()) !=0 )
                return o1.getCategory_name().compareTo(o2.getCategory_name());

        if(o1.getProduct_name().compareTo(o2.getProduct_name()) !=0 )
            return o1.getProduct_name().compareTo(o2.getProduct_name());

        return o1.getWarehousename().compareTo(o2.getWarehousename());
    }
}
