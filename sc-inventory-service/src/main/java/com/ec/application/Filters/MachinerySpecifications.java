package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Machinery;
import com.ec.application.model.Machinery_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class MachinerySpecifications {

    private static final SpecificationsBuilder<Machinery> specbldr = new SpecificationsBuilder<>();

    public static Specification<Machinery> getSpecification(FilterDataList filterDataList) {
        List<String> names = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");

        Specification<Machinery> spec = null;
        if (notEmpty(names))
            spec = and(spec, specbldr.whereDirectFieldContains(Machinery_.MACHINERY_NAME, names));
        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
