package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.InventoryMonthPriceMapping;
import com.ec.application.model.InventoryMonthPriceMapping_;
import com.ec.application.model.Product_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class InventoryMonthPriceMappingSpecification {

    private static final SpecificationsBuilder<InventoryMonthPriceMapping> specbldr = new SpecificationsBuilder<>();

    public static Specification<InventoryMonthPriceMapping> getSpecification(FilterDataList filterDataList) {
        List<String> productNames  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");

        Specification<InventoryMonthPriceMapping> spec = null;

        if (notEmpty(productNames))
            spec = and(spec, specbldr.whereChildFieldContains(
                    InventoryMonthPriceMapping_.PRODUCT, Product_.PRODUCT_NAME, productNames));

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereGrandChildFieldContains(
                    InventoryMonthPriceMapping_.PRODUCT, Product_.CATEGORY, Category_.CATEGORY_NAME, categoryNames));

        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
