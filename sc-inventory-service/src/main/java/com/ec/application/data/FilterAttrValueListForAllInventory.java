package com.ec.application.data;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
public class FilterAttrValueListForAllInventory {
    Date startDate;
    Date endDate;
    Set<String> productNames;
    Set<String> categoryNames;
    Set<String> warehouseNames;
}
