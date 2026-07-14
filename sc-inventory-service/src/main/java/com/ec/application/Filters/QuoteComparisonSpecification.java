package com.ec.application.Filters;

import com.ec.application.data.QuoteComparisonFilter;
import com.ec.application.model.QuoteComparison;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class QuoteComparisonSpecification {

    public static Specification<QuoteComparison> getSpec(QuoteComparisonFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) return cb.conjunction();

            if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
                String search = filter.getSearch().trim().toLowerCase();
                Join<Object, Object> indentRefs = root.join("indentIds", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("qcId")), "%" + search + "%"),
                        cb.like(cb.lower(indentRefs.as(String.class)), "%" + search + "%")
                ));
                query.distinct(true);
            }

            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(root.get("status").in(filter.getStatus()));
            }

            if (filter.getCreatedBy() != null && !filter.getCreatedBy().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("createdByUser")),
                        "%" + filter.getCreatedBy().toLowerCase() + "%"));
            }

            if (filter.getSupplierName() != null && !filter.getSupplierName().isEmpty()) {
                Join<Object, Object> supplierQuotes = root.join("supplierQuotes", JoinType.LEFT);
                List<String> lowered = filter.getSupplierName().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                predicates.add(cb.lower(supplierQuotes.get("supplierName")).in(lowered));
                query.distinct(true);
            }

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                if (filter.getDateFrom() != null && !filter.getDateFrom().trim().isEmpty()) {
                    Date from = sdf.parse(filter.getDateFrom());
                    predicates.add(cb.greaterThanOrEqualTo(root.get("comparisonDate"), from));
                }
                if (filter.getDateTo() != null && !filter.getDateTo().trim().isEmpty()) {
                    Date to = sdf.parse(filter.getDateTo());
                    predicates.add(cb.lessThanOrEqualTo(root.get("comparisonDate"), to));
                }
            } catch (Exception ignored) {
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
