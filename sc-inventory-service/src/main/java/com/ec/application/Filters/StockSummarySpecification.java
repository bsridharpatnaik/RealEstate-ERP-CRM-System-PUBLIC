package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.model.StockSummary;
import com.ec.application.model.StockSummary_;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

public final class StockSummarySpecification {

    private static final SpecificationsBuilder<StockSummary> specbldr = new SpecificationsBuilder<>();

    public static Specification<StockSummary> getSpecification(FilterDataList filterDataList) {
        List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> tenants      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "tenants");
        List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<StockSummary> spec = null;

        if (notEmpty(productNames))
            spec = and(spec, specbldr.whereDirectFieldContains(StockSummary_.PRODUCT_NAME, productNames));

        if (notEmpty(tenants))
            spec = and(spec, specbldr.whereDirectFieldContains(StockSummary_.TENANT_SCHEMA, tenants));

        if (notEmpty(productCodes))
            spec = and(spec, specbldr.whereDirectFieldContains(StockSummary_.PRODUCT_CODE, productCodes));

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereDirectFieldEquals(StockSummary_.CATEGORY_NAME, categoryNames));

        if (notEmpty(globalSearch)) {
            Specification<StockSummary> gs = null;
            gs = or(gs, specbldr.whereDirectFieldContains(StockSummary_.PRODUCT_NAME, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(StockSummary_.PRODUCT_CODE, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(StockSummary_.TENANT_SCHEMA, globalSearch));
            spec = and(spec, gs);
        }

        return spec;
    }

    public static Specification<StockSummary> addWarehouseForDeadStockFilter(Specification<StockSummary> spec) {
        return and(spec, specbldr.whereDirectFieldEquals(StockSummary_.WAREHOUSE_NAME,
                Collections.singletonList(ProjectConstants.deadStockWarehouseName)));
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
