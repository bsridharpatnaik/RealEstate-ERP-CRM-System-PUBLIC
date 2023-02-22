package com.ec.common.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class InventoryMonthUsageInformationSpecification {
    static SpecificationsBuilder<InventoryMonthUsageInformation> specbldr = new SpecificationsBuilder<InventoryMonthUsageInformation>();

    public static Specification<InventoryMonthUsageInformation> getSpecification(FilterDataList filterDataList) {
        List<String> locationId = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "locationId");
        List<String> dates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "dates");
        Specification<InventoryMonthUsageInformation> finalSpec = null;

        if (locationId != null && locationId.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(InventoryMonthUsageInformation_.LOCATION_ID, locationId));

        if (dates != null && dates.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(InventoryMonthUsageInformation_.YM, dates));
        return finalSpec;

    }
}
