package com.ec.application.repository;

import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.ProductStockSumDTO;
import com.ec.application.data.StockSummaryAggregatedDTO;
import com.ec.application.model.StockSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class StockSummaryCustomRepoImpl implements StockSummaryCustomRepo {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<StockSummaryAggregatedDTO> fetchAggregatedStock(
            FilterDataList filters,
            Pageable pageable
    ) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        /* =========================
           MAIN QUERY (DATA)
           ========================= */
        CriteriaQuery<StockSummaryAggregatedDTO> cq =
                cb.createQuery(StockSummaryAggregatedDTO.class);

        Root<StockSummary> root = cq.from(StockSummary.class);
        List<Predicate> predicates = buildPredicates(cb, root, filters);

        cq.select(cb.construct(
                StockSummaryAggregatedDTO.class,
                root.get("tenantSchema"),
                root.get("productId"),
                root.get("productCode"),
                root.get("productName"),
                cb.sum(root.get("quantityInHand")),
                root.get("measurementUnit"),
                cb.greatest(root.<Date>get("syncedAt")) // ⭐ important
        ));


        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        cq.groupBy(
                root.get("tenantSchema"),
                root.get("productId"),
                root.get("productCode"),
                root.get("productName"),
                root.get("measurementUnit")
        );

        /* ===== Sorting ===== */
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                if ("productId".equals(sortOrder.getProperty())) {
                    orders.add(sortOrder.isAscending()
                            ? cb.asc(root.get("productId"))
                            : cb.desc(root.get("productId")));
                }
                if ("totalQuantity".equals(sortOrder.getProperty())) {
                    orders.add(sortOrder.isAscending()
                            ? cb.asc(cb.sum(root.get("quantityInHand")))
                            : cb.desc(cb.sum(root.get("quantityInHand"))));
                }
            }
            cq.orderBy(orders);
        }

        TypedQuery<StockSummaryAggregatedDTO> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<StockSummaryAggregatedDTO> content = query.getResultList();

        /* =========================
           COUNT QUERY (GROUP COUNT)
           ========================= */
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<StockSummary> countRoot = countQuery.from(StockSummary.class);

        List<Predicate> countPredicates =
                buildPredicates(cb, countRoot, filters);

        countQuery.select(cb.countDistinct(countRoot.get("productId")));

        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        Long total =
                em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    /* =========================
       Predicate Builder (Reusable)
       ========================= */
    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<StockSummary> root,
            FilterDataList filters
    ) {

        List<Predicate> predicates = new ArrayList<>();

        if (filters == null || filters.getFilterData() == null) {
            return predicates;
        }

        for (FilterAttributeData fad : filters.getFilterData()) {

            String attr = fad.getAttrName();
            List<String> values = fad.getAttrValue();

            if (values == null || values.isEmpty()) {
                continue;
            }

            switch (attr) {

                case "tenants":
                    predicates.add(root.get("tenantSchema").in(values));
                    break;

                case "productId":
                    predicates.add(
                            root.get("productId").in(
                                    values.stream()
                                            .map(Long::valueOf)
                                            .collect(Collectors.toList())
                            )
                    );
                    break;

                case "productCodes":
                    predicates.add(root.get("productCode").in(values));
                    break;

                case "productNames":
                    predicates.add(
                            cb.lower(root.get("productName"))
                                    .in(values.stream()
                                            .map(String::toLowerCase)
                                            .collect(Collectors.toList()))
                    );
                    break;
                case "globalSearch":
                    List<Predicate> orPredicates = new ArrayList<>();
                    for (String term : values) {
                        String pattern = "%" + term.toLowerCase() + "%";
                        orPredicates.add(cb.like(cb.lower(root.get("tenantSchema")), pattern));
                        orPredicates.add(cb.like(cb.lower(root.get("productCode")), pattern));
                        orPredicates.add(cb.like(cb.lower(root.get("productName")), pattern));
                    }
                    predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
                    break;
            }
        }
        return predicates;
    }
}
