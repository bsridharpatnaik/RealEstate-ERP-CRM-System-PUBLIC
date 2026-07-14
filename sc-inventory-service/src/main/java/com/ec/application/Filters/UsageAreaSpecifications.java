package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.UsageArea;
import com.ec.application.model.UsageArea_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class UsageAreaSpecifications {

    private static final SpecificationsBuilder<UsageArea> specbldr = new SpecificationsBuilder<>();

    public static Specification<UsageArea> getSpecification(FilterDataList filterDataList) {
        List<String> names = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");

        Specification<UsageArea> spec = null;
        if (notEmpty(names))
            spec = and(spec, specbldr.whereDirectFieldContains(UsageArea_.USAGE_AREA_NAME, names));
        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
