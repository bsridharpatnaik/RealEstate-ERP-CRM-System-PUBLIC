package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.StockInformationFromView;
import com.ec.application.model.StockInformationFromView_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class StockInformationSpecification {

    private static final SpecificationsBuilder<StockInformationFromView> specbldr = new SpecificationsBuilder<>();

    public static Specification<StockInformationFromView> getSpecification(FilterDataList filterDataList) {
        List<String> products     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "products");
        List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> categories   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categories");
        List<String> warehouses   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouses");
        List<String> stockStatus  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "stockStatus");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<StockInformationFromView> spec = null;

        if (notEmpty(products))
            spec = and(spec, specbldr.whereDirectFieldContains(StockInformationFromView_.PRODUCT_NAME, products));

        if (notEmpty(productCodes))
            spec = and(spec, specbldr.whereDirectFieldContains(StockInformationFromView_.PRODUCT_CODE, productCodes));

        if (notEmpty(categories))
            spec = and(spec, specbldr.whereDirectFieldContains(StockInformationFromView_.CATEGORY_NAME, categories));

        if (notEmpty(warehouses))
            spec = and(spec, specbldr.whereDirectFieldContains(StockInformationFromView_.DETAILED_STOCK, warehouses));

        if (notEmpty(stockStatus) && !stockStatus.contains("All"))
            spec = and(spec, specbldr.whereDirectFieldContains(StockInformationFromView_.STOCK_STATUS, stockStatus));

        if (notEmpty(globalSearch)) {
            Specification<StockInformationFromView> gs = null;
            gs = or(gs, specbldr.whereDirectFieldContains(StockInformationFromView_.PRODUCT_NAME, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(StockInformationFromView_.PRODUCT_CODE, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(StockInformationFromView_.DETAILED_STOCK, globalSearch));
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
