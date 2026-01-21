package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.DeadStockSummary;
import com.ec.application.model.DeadStockSummary_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class DeadStockSpecification {
    static SpecificationsBuilder<DeadStockSummary> specbldr = new SpecificationsBuilder<DeadStockSummary>();

    public static Specification<DeadStockSummary> getSpecification(FilterDataList filterDataList) {
        List<String> productNames = specbldr.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> tenants = specbldr.fetchValueFromFilterList(filterDataList, "tenants");
        List<String> productCodes = specbldr.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> globalSearch = specbldr.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<DeadStockSummary> finalSpec = null;

        if (productNames != null && productNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(DeadStockSummary_.PRODUCT_NAME, productNames));

        if (tenants != null && tenants.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(DeadStockSummary_.TENANT_SCHEMA, tenants));

        if (productCodes != null && productCodes.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(DeadStockSummary_.PRODUCT_CODE, productCodes));

        if (globalSearch != null && globalSearch.size() > 0) {
            Specification<DeadStockSummary> globalSpec = null;
            globalSpec = specbldr.specOrCondition(globalSpec, specbldr.whereDirectFieldContains(DeadStockSummary_.PRODUCT_NAME, globalSearch));
            globalSpec = specbldr.specOrCondition(globalSpec, specbldr.whereDirectFieldContains(DeadStockSummary_.PRODUCT_CODE, globalSearch));
            globalSpec = specbldr.specOrCondition(globalSpec, specbldr.whereDirectFieldContains(DeadStockSummary_.TENANT_SCHEMA, globalSearch));
            finalSpec = specbldr.specAndCondition(finalSpec, globalSpec);
        }

        return finalSpec;
    }
}
