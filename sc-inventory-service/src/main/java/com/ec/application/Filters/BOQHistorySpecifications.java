package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.BOQHistory;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class BOQHistorySpecifications {

    private static final SpecificationsBuilder<BOQHistory> specbldr = new SpecificationsBuilder<>();

    public static Specification<BOQHistory> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDate    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDate      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> buildingType = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "buildingType");
        List<String> buildingUnit = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "buildingUnit");
        List<String> inventory    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "inventory");
        List<String> changeType   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "changeType");
        List<String> changedBy    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "changedBy");

        Specification<BOQHistory> spec = null;

        if (notEmpty(startDate))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan("changeDateTime", startDate));

        if (notEmpty(endDate))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan("changeDateTime", endDate));

        if (notEmpty(buildingType))
            spec = and(spec, specbldr.whereChildFieldContains("buildingType", "typeName", buildingType));

        if (notEmpty(buildingUnit))
            spec = and(spec, specbldr.whereChildFieldContains("usageLocation", "locationName", buildingUnit));

        if (notEmpty(inventory))
            spec = and(spec, specbldr.whereChildFieldContains("product", "productName", inventory));

        if (notEmpty(changeType))
            spec = and(spec, specbldr.whereDirectFieldEquals("changeType", changeType));

        if (notEmpty(changedBy))
            spec = and(spec, specbldr.whereDirectFieldContains("changedBy", changedBy));

        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
