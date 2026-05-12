package com.ec.application.Filters;

import com.ec.application.model.ActivityLog;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class ActivityLogSpecification {

    private static final SpecificationsBuilder<ActivityLog> specbldr = new SpecificationsBuilder<>();

    public static Specification<ActivityLog> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDate   = specbldr.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDate     = specbldr.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> action      = specbldr.fetchValueFromFilterList(filterDataList, "action");
        List<String> entityType  = specbldr.fetchValueFromFilterList(filterDataList, "entityType");
        List<String> entityId    = specbldr.fetchValueFromFilterList(filterDataList, "entityId");
        List<String> performedBy = specbldr.fetchValueFromFilterList(filterDataList, "performedBy");

        Specification<ActivityLog> finalSpec = null;

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

        return finalSpec;
    }
}
