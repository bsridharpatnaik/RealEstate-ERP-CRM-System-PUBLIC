package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.ServiceOrder;
import com.ec.application.model.ServiceOrderLine;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.text.ParseException;
import java.util.List;

public final class ServiceOrderSpecification {

    private static final SpecificationsBuilder<ServiceOrder> specbldr = new SpecificationsBuilder<>();

    public static Specification<ServiceOrder> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> statusList   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "status");
        List<String> vendors      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "vendors");
        List<String> projectNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "projectNames");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> descriptions = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "descriptions");
        List<String> nextServiceFrom = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "nextServiceFrom");
        List<String> nextServiceTo   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "nextServiceTo");
        List<String> nextServiceOverdue = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "nextServiceOverdue");

        Specification<ServiceOrder> spec = null;

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan("serviceDate", startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan("serviceDate", endDates));

        if (notEmpty(statusList))
            spec = and(spec, specbldr.whereDirectFieldEquals("status", statusList));

        if (notEmpty(vendors))
            spec = and(spec, specbldr.whereChildFieldEquals("vendor", "name", vendors));

        if (notEmpty(projectNames))
            spec = and(spec, specbldr.whereDirectFieldEquals("projectName", projectNames));

        if (notEmpty(descriptions))
            spec = and(spec, soLineExistsWithDescriptionIn(descriptions));

        if (notEmpty(nextServiceFrom))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan("nextServiceDate", nextServiceFrom));

        if (notEmpty(nextServiceTo))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan("nextServiceDate", nextServiceTo));

        if (notEmpty(nextServiceOverdue) && Boolean.parseBoolean(nextServiceOverdue.get(0))) {
            spec = and(spec, (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("nextServiceDate")),
                cb.lessThan(root.get("nextServiceDate"), new java.util.Date())
            ));
        }

        if (notEmpty(globalSearch)) {
            Specification<ServiceOrder> gs = null;
            gs = or(gs, specbldr.whereDirectFieldContains("serviceOrderId", globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains("status", globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains("subject", globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains("notes", globalSearch));
            gs = or(gs, specbldr.whereChildFieldContains("vendor", "name", globalSearch));
            gs = or(gs, specbldr.whereChildFieldContains("firm", "firmName", globalSearch));
            spec = and(spec, gs);
        }

        return spec;
    }

    private static Specification<ServiceOrder> soLineExistsWithDescriptionIn(List<String> descriptions) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ServiceOrderLine> line = sub.from(ServiceOrderLine.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(line.get("serviceOrder").get("serviceOrderId"), root.get("serviceOrderId")),
                line.get("description").in(descriptions)
            ));
            return cb.exists(sub);
        };
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
