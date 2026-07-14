package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.BOQStatus;
import com.ec.application.model.BOQStatus_;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

public final class BOQStatusSpecification {

    private static final SpecificationsBuilder<BOQStatus> specbldr = new SpecificationsBuilder<>();

    public static Specification<BOQStatus> getSpecification(Long id, FilterDataList filterDataList) throws Exception {
        List<String> inventoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "inventoryName");
        List<String> boqStatus      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "consumedPercent");

        Specification<BOQStatus> spec =
            specbldr.whereDirectFieldLongFieldContains(BOQStatus_.LOCATION_ID,
                Collections.singletonList(id.toString()));

        if (notEmpty(inventoryNames))
            spec = and(spec, specbldr.whereDirectFieldContains(BOQStatus_.PRODUCT_NAME, inventoryNames));

        if (notEmpty(boqStatus)) {
            try {
                spec = and(spec, specbldr.whereDirectFieldDoubleGreaterThan(
                        BOQStatus_.CONSUMED_PERCENT, Double.parseDouble(boqStatus.get(0))));
            } catch (NumberFormatException e) {
                throw new Exception("Unable to parse number value from string");
            }
        }
        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
