package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.model.StockSummary;
import com.ec.application.model.StockSummary_;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StockSummarySpecification {
    static SpecificationsBuilder<StockSummary> specbldr = new SpecificationsBuilder<StockSummary>();

    public static Specification<StockSummary> getSpecification(FilterDataList filterDataList) {
        List<String> productNames = specbldr.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> tenants = specbldr.fetchValueFromFilterList(filterDataList, "tenants");
        List<String> productCodes = specbldr.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> globalSearch = specbldr.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<StockSummary> finalSpec = null;

        if (productNames != null && !productNames.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(StockSummary_.PRODUCT_NAME, productNames));

        if (tenants != null && !tenants.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(StockSummary_.TENANT_SCHEMA, tenants));

        if (productCodes != null && !productCodes.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(StockSummary_.PRODUCT_CODE, productCodes));

        if (globalSearch != null && !globalSearch.isEmpty()) {
            Specification<StockSummary> globalSpec = null;
            globalSpec = specbldr.specOrCondition(globalSpec, specbldr.whereDirectFieldContains(StockSummary_.PRODUCT_NAME, globalSearch));
            globalSpec = specbldr.specOrCondition(globalSpec, specbldr.whereDirectFieldContains(StockSummary_.PRODUCT_CODE, globalSearch));
            globalSpec = specbldr.specOrCondition(globalSpec, specbldr.whereDirectFieldContains(StockSummary_.TENANT_SCHEMA, globalSearch));
            finalSpec = specbldr.specAndCondition(finalSpec, globalSpec);
        }

        return finalSpec;
    }

    public static Specification<StockSummary> addWarehouseForDeadStockFilter(Specification<StockSummary> spec) {
        return specbldr.specAndCondition(spec, specbldr.whereDirectFieldEquals(StockSummary_.WAREHOUSE_NAME, new ArrayList<>(Collections.singletonList(ProjectConstants.deadStockWarehouseName))));
    }
}
