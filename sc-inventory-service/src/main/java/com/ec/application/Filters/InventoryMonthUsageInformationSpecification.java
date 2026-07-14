package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.InventoryMonthUsageInformation;
import com.ec.application.model.InventoryMonthUsageInformation_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class InventoryMonthUsageInformationSpecification {

    private static final SpecificationsBuilder<InventoryMonthUsageInformation> specbldr = new SpecificationsBuilder<>();

    public static Specification<InventoryMonthUsageInformation> getSpecification(FilterDataList filterDataList) {
        List<String> locationId = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "locationId");
        List<String> dates      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "dates");

        Specification<InventoryMonthUsageInformation> spec = null;

        if (notEmpty(locationId))
            spec = and(spec, specbldr.whereDirectFieldContains(InventoryMonthUsageInformation_.LOCATION_ID, locationId));

        if (notEmpty(dates))
            spec = and(spec, specbldr.whereDirectFieldContains(InventoryMonthUsageInformation_.YM, dates));

        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
