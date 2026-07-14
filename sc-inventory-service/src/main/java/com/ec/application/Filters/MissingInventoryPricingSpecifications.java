package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.MissingInventoryPricing;
import com.ec.application.model.MissingInventoryPricing_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class MissingInventoryPricingSpecifications {

    private static final SpecificationsBuilder<MissingInventoryPricing> specbldr = new SpecificationsBuilder<>();

    public static Specification<MissingInventoryPricing> getSpecification(FilterDataList filterDataList) {
        List<String> productNames  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");

        Specification<MissingInventoryPricing> spec = null;

        if (notEmpty(productNames))
            spec = and(spec, specbldr.whereDirectFieldContains(MissingInventoryPricing_.PRODUCT_NAME, productNames));

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereDirectFieldContains(MissingInventoryPricing_.CATEGORY_NAME, categoryNames));

        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
