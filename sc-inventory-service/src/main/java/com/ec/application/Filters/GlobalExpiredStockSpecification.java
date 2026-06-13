package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.GlobalExpiredStockReport;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class GlobalExpiredStockSpecification {

    private static final SpecificationsBuilder<GlobalExpiredStockReport> specbldr = new SpecificationsBuilder<>();

    /**
     * Supported filter keys:
     *   tenantSchema — exact match (project code)
     *   category     — exact match
     *   expiryFilter — special keyword tile filter:
     *                    "expired"  → daysUntilExpiry < 0
     *                    "within1"  → daysUntilExpiry <= 0   (expired + today)
     *                    "within10" → daysUntilExpiry <= 10
     *                    "within30" → daysUntilExpiry <= 30
     *                    "within90" → daysUntilExpiry <= 90
     */
    public static Specification<GlobalExpiredStockReport> getSpecification(FilterDataList filterDataList) {
        List<String> tenantSchema  = specbldr.fetchValueFromFilterList(filterDataList, "tenantSchema");
        List<String> category      = specbldr.fetchValueFromFilterList(filterDataList, "category");
        List<String> expiryFilter  = specbldr.fetchValueFromFilterList(filterDataList, "expiryFilter");

        Specification<GlobalExpiredStockReport> finalSpec = null;

        if (tenantSchema != null && !tenantSchema.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("tenantSchema", tenantSchema));

        if (category != null && !category.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("category", category));

        if (expiryFilter != null && !expiryFilter.isEmpty()) {
            String ef = expiryFilter.get(0);
            Specification<GlobalExpiredStockReport> expirySpec = buildExpiryFilterSpec(ef);
            if (expirySpec != null)
                finalSpec = specbldr.specAndCondition(finalSpec, expirySpec);
        }

        return finalSpec;
    }

    private static Specification<GlobalExpiredStockReport> buildExpiryFilterSpec(String ef) {
        switch (ef) {
            case "expired":
                return (root, query, cb) -> cb.lessThan(root.get("daysUntilExpiry"), 0);
            case "within1":
                return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("daysUntilExpiry"), 1);
            case "within10":
                return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("daysUntilExpiry"), 10);
            case "within30":
                return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("daysUntilExpiry"), 30);
            case "within90":
                return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("daysUntilExpiry"), 90);
            default:
                return null;
        }
    }
}
