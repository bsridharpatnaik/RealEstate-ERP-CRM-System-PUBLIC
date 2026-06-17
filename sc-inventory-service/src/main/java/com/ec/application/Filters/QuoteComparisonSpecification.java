package com.ec.application.Filters;

import com.ec.application.data.QuoteComparisonFilter;
import com.ec.application.model.QuoteComparison;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuoteComparisonSpecification {

    public static Specification<QuoteComparison> getSpec(QuoteComparisonFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) return cb.conjunction();

            if (filter.getQcId() != null && !filter.getQcId().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("qcId")),
                        "%" + filter.getQcId().toLowerCase() + "%"));
            }

            if (filter.getProject() != null && !filter.getProject().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("project"), filter.getProject()));
            }

            if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getCreatedBy() != null && !filter.getCreatedBy().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("createdByUser")),
                        "%" + filter.getCreatedBy().toLowerCase() + "%"));
            }

            if (filter.getIndentId() != null && !filter.getIndentId().trim().isEmpty()) {
                // Check in the indent refs collection
                Join<Object, Object> indentRefs = root.join("indentIds", JoinType.LEFT);
                predicates.add(cb.equal(indentRefs, filter.getIndentId()));
                query.distinct(true);
            }

            if (filter.getSupplierName() != null && !filter.getSupplierName().trim().isEmpty()) {
                Join<Object, Object> supplierQuotes = root.join("supplierQuotes", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(supplierQuotes.get("supplierName")),
                        "%" + filter.getSupplierName().toLowerCase() + "%"));
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
