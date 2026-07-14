package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.Product_;
import com.ec.application.model.Stock;
import com.ec.application.model.Stock_;
import com.ec.application.model.Warehouse_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class StockSpecification {

    private static final SpecificationsBuilder<Stock> specbldr = new SpecificationsBuilder<>();

    public static Specification<Stock> getSpecification(FilterDataList filterDataList) {
        List<String> products     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "products");
        List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> categories   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categories");
        List<String> warehouses   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouses");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<Stock> spec = null;

        if (notEmpty(products))
            spec = and(spec, specbldr.whereChildFieldContains(Stock_.PRODUCT, Product_.PRODUCT_NAME, products));

        if (notEmpty(productCodes))
            spec = and(spec, specbldr.whereChildFieldContains(Stock_.PRODUCT, Product_.PRODUCT_CODE, productCodes));

        if (notEmpty(categories))
            spec = and(spec, specbldr.whereGrandChildFieldContains(
                    Stock_.PRODUCT, Product_.CATEGORY, Category_.CATEGORY_NAME, categories));

        if (notEmpty(warehouses))
            spec = and(spec, specbldr.whereChildFieldContains(Stock_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, warehouses));

        if (notEmpty(globalSearch)) {
            Specification<Stock> gs = null;
            gs = or(gs, specbldr.whereChildFieldContains(Stock_.PRODUCT, Product_.PRODUCT_NAME, globalSearch));
            gs = or(gs, specbldr.whereChildFieldContains(Stock_.PRODUCT, Product_.PRODUCT_CODE, globalSearch));
            gs = or(gs, specbldr.whereChildFieldContains(Stock_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, globalSearch));
            spec = and(spec, gs);
        }

        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static <T> Specification<T> or(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.or(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
