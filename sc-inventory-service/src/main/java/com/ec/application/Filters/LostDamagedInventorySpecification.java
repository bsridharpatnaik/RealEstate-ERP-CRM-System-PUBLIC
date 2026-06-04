package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.model.LostDamagedInventory_;
import com.ec.application.model.Product_;
import com.ec.application.model.Warehouse_;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class LostDamagedInventorySpecification {

    private static final SpecificationsBuilder<LostDamagedInventory> specbldr = new SpecificationsBuilder<>();

    public static Specification<LostDamagedInventory> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates       = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "EndDate");
        List<String> productNames   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "ProductNames");
        List<String> categoryNames  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "CategoryNames");
        List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouseNames");
        List<String> globalSearch   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<LostDamagedInventory> spec = null;

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan(LostDamagedInventory_.DATE, startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan(LostDamagedInventory_.DATE, endDates));

        if (notEmpty(productNames))
            spec = and(spec, specbldr.whereChildFieldContains(
                    LostDamagedInventory_.PRODUCT, Product_.PRODUCT_NAME, productNames));

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereGrandChildFieldContains(
                    LostDamagedInventory_.PRODUCT, Product_.CATEGORY, Category_.CATEGORY_NAME, categoryNames));

        if (notEmpty(warehouseNames))
            spec = and(spec, specbldr.whereChildFieldContains(
                    LostDamagedInventory_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, warehouseNames));

        if (notEmpty(globalSearch)) {
            Specification<LostDamagedInventory> gs = null;
            gs = or(gs, specbldr.whereChildFieldContains(
                    LostDamagedInventory_.PRODUCT, Product_.PRODUCT_NAME, globalSearch));
            gs = or(gs, specbldr.whereGrandChildFieldContains(
                    LostDamagedInventory_.PRODUCT, Product_.CATEGORY, Category_.CATEGORY_NAME, globalSearch));
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
