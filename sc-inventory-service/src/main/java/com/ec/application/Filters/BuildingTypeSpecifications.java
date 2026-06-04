package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.BuildingType;
import com.ec.application.model.BuildingType_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class BuildingTypeSpecifications {

    private static final SpecificationsBuilder<BuildingType> specbldr = new SpecificationsBuilder<>();

    public static Specification<BuildingType> getSpecification(FilterDataList filterDataList) {
        List<String> names = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");

        Specification<BuildingType> spec = null;
        if (notEmpty(names))
            spec = and(spec, specbldr.whereDirectFieldContains(BuildingType_.TYPE_NAME, names));
        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
