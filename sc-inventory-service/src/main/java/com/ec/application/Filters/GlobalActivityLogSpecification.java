package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.GlobalActivityLog;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class GlobalActivityLogSpecification {

    private static final SpecificationsBuilder<GlobalActivityLog> specbldr = new SpecificationsBuilder<>();

    public static Specification<GlobalActivityLog> getSpecification(FilterDataList filterDataList, List<String> allowedSchemas) throws ParseException {
        List<String> startDate    = specbldr.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDate      = specbldr.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> action       = specbldr.fetchValueFromFilterList(filterDataList, "action");
        List<String> entityType   = specbldr.fetchValueFromFilterList(filterDataList, "entityType");
        List<String> entityId     = specbldr.fetchValueFromFilterList(filterDataList, "entityId");
        List<String> performedBy  = specbldr.fetchValueFromFilterList(filterDataList, "performedBy");
        List<String> tenantSchema = specbldr.fetchValueFromFilterList(filterDataList, "tenantSchema");

        Specification<GlobalActivityLog> finalSpec = null;

        if (startDate != null && !startDate.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan("activityTime", startDate));

        if (endDate != null && !endDate.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan("activityTime", endDate));

        if (action != null && !action.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("action", action));

        if (entityType != null && !entityType.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("entityType", entityType));

        if (entityId != null && !entityId.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains("entityId", entityId));

        if (performedBy != null && !performedBy.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains("performedBy", performedBy));

        if (tenantSchema != null && !tenantSchema.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("tenantSchema", tenantSchema));

        if (allowedSchemas != null && !allowedSchemas.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("tenantSchema", allowedSchemas));

        return finalSpec;
    }
}
