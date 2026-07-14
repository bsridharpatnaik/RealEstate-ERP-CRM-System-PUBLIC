package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category;
import com.ec.application.model.Category_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class CategorySpecifications {

    private static final SpecificationsBuilder<Category> specbldr = new SpecificationsBuilder<>();

    public static Specification<Category> getSpecification(FilterDataList filterDataList) {
        List<String> names = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");

        Specification<Category> spec = null;
        if (notEmpty(names))
            spec = and(spec, specbldr.whereDirectFieldContains(Category_.CATEGORY_NAME, names));
        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
