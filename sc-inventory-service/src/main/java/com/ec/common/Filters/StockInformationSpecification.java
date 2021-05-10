package com.ec.common.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class StockInformationSpecification {
    static SpecificationsBuilder<StockInformationFromView> specbldr = new SpecificationsBuilder<StockInformationFromView>();

    public static Specification<StockInformationFromView> getSpecification(FilterDataList filterDataList) {
        List<String> products = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "products");
        List<String> categories = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categories");
        List<String> warehouses = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouses");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String>  stockStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "stockStatus");
        Specification<StockInformationFromView> finalSpec = null;

        if (products != null && products.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains(StockInformationFromView_.PRODUCT_NAME, products));

        if (categories != null && categories.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(StockInformationFromView_.CATEGORY_NAME, categories));

        if (warehouses != null && warehouses.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains(StockInformationFromView_.DETAILED_STOCK, warehouses));

        if (stockStatus != null && stockStatus.size() > 0) {
            if(!stockStatus.contains("All")) {
                finalSpec = specbldr.specAndCondition(finalSpec,
                        specbldr.whereDirectFieldContains(StockInformationFromView_.STOCK_STATUS, stockStatus));
            }
        }
        if (globalSearch != null && globalSearch.size() > 0) {
            Specification<StockInformationFromView> internalSpec = null;
            internalSpec = specbldr.specOrCondition(internalSpec,
                    specbldr.whereDirectFieldContains(StockInformationFromView_.PRODUCT_NAME, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec,
                    specbldr.whereDirectFieldContains(StockInformationFromView_.DETAILED_STOCK, globalSearch));
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }
}
