package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.model.AllInventoryTransactions_;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class AllInventorySpecification {

    private static final SpecificationsBuilder<AllInventoryTransactions> specbldr = new SpecificationsBuilder<>();

    public static Specification<AllInventoryTransactions> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> type           = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "type");
        List<String> productNames   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "products");
        List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouses");
        List<String> startDates     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates       = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "EndDate");
        List<String> categoryNames  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categories");
        List<String> globalSearch   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<AllInventoryTransactions> spec = null;

        if (notEmpty(type))
            spec = and(spec, specbldr.whereDirectFieldContains(AllInventoryTransactions_.TYPE, type));

        if (notEmpty(productNames))
            spec = and(spec, specbldr.whereDirectFieldContains(AllInventoryTransactions_.PRODUCT_NAME, productNames));

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereDirectFieldContains(AllInventoryTransactions_.CATEGORY_NAME, categoryNames));

        if (notEmpty(warehouseNames))
            spec = and(spec, specbldr.whereDirectFieldContains(AllInventoryTransactions_.WAREHOUSE_NAME, warehouseNames));

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan(AllInventoryTransactions_.DATE, startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan(AllInventoryTransactions_.DATE, endDates));

        if (notEmpty(globalSearch)) {
            Specification<AllInventoryTransactions> gs = null;
            gs = or(gs, specbldr.whereDirectFieldContains(AllInventoryTransactions_.PRODUCT_NAME, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(AllInventoryTransactions_.NAME, globalSearch));
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
