package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.UsageLocation;
import com.ec.application.model.UsageLocation_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class LocationSpecifications {

    private static final SpecificationsBuilder<UsageLocation> specbldr = new SpecificationsBuilder<>();

    public static Specification<UsageLocation> getSpecification(FilterDataList filterDataList) {
        List<String> names = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");

        Specification<UsageLocation> spec = null;
        if (notEmpty(names))
            spec = and(spec, specbldr.whereDirectFieldContains(UsageLocation_.LOCATION_NAME, names));
        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
