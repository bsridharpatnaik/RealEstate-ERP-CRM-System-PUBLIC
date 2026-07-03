package com.ec.application.repository;

import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.data.ProductStockSumDTO;
import com.ec.application.data.StockSummaryAggregatedDTO;
import com.ec.application.data.StockSummaryTilesDTO;
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
       MAIN QUERY
       ========================= */
        CriteriaQuery<StockSummaryAggregatedDTO> cq =
                cb.createQuery(StockSummaryAggregatedDTO.class);

        Root<StockSummary> root = cq.from(StockSummary.class);

        List<Predicate> wherePredicates = new ArrayList<>();
        List<Predicate> havingPredicates = new ArrayList<>();

    /* =========================
       AGGREGATE EXPRESSIONS
       ========================= */
        Expression<Double> totalStockExpr =
                cb.sum(root.get("quantityInHand"));

        Expression<Double> deadStockExpr =
                cb.sum(
                        cb.<Double>selectCase()
                                .when(
                                        cb.equal(root.get("warehouseName"), "Dead Stock Warehouse"),
                                        root.get("quantityInHand")
                                )
                                .otherwise(0.0)
                );

        Expression<Double> reorderLevelExpr =
                cb.max(root.<Double>get("reorderLevel"));

    /* =========================
       FILTER HANDLING
       ========================= */
        if (filters != null && filters.getFilterData() != null) {
            for (FilterAttributeData fad : filters.getFilterData()) {

                String attr = fad.getAttrName();
                List<String> values = fad.getAttrValue();

                if (values == null || values.isEmpty()) {
                    continue;
                }

                switch (attr) {

                    case "tenants":
                        wherePredicates.add(
                                root.get("tenantSchema").in(values)
                        );
                        break;

                    case "productId":
                        wherePredicates.add(
                                root.get("productId").in(
                                        values.stream()
                                                .map(Long::valueOf)
                                                .collect(Collectors.toList())
                                )
                        );
                        break;

                    case "productCodes":
                        wherePredicates.add(
                                root.get("productCode").in(values)
                        );
                        break;

                    case "productNames":
                        wherePredicates.add(
                                cb.lower(root.get("productName"))
                                        .in(values.stream()
                                                .map(String::toLowerCase)
                                                .collect(Collectors.toList()))
                        );
                        break;

                    case "categoryNames":
                        wherePredicates.add(
                                root.get("categoryName").in(values)
                        );
                        break;

                    case "globalSearch":
                        List<Predicate> orPredicates = new ArrayList<>();

                        for (String term : values) {
                            String pattern = "%" + term.toLowerCase() + "%";

                            orPredicates.add(
                                    cb.like(cb.lower(root.get("tenantSchema")), pattern)
                            );
                            orPredicates.add(
                                    cb.like(cb.lower(root.get("productCode")), pattern)
                            );
                            orPredicates.add(
                                    cb.like(cb.lower(root.get("productName")), pattern)
                            );
                        }

                        wherePredicates.add(
                                cb.or(orPredicates.toArray(new Predicate[0]))
                        );
                        break;

                    case "deadStockPresent":
                        boolean present = Boolean.parseBoolean(values.get(0));

                        if (present) {
                            havingPredicates.add(
                                    cb.greaterThan(deadStockExpr, 0.0)
                            );
                        } else {
                            havingPredicates.add(
                                    cb.equal(deadStockExpr, 0.0)
                            );
                        }
                        break;

                    case "lowStock":
                        boolean lowStock = Boolean.parseBoolean(values.get(0));
                        if (lowStock) {
                            // Only rows where reorder_level is set AND total qty <= reorder_level
                            havingPredicates.add(cb.isNotNull(reorderLevelExpr));
                            havingPredicates.add(
                                    cb.lessThanOrEqualTo(totalStockExpr, reorderLevelExpr)
                            );
                        }
                        break;
                }
            }
        }

    /* =========================
       SELECT
       ========================= */
        cq.select(cb.construct(
                StockSummaryAggregatedDTO.class,
                root.get("tenantSchema"),
                root.get("productId"),
                root.get("productCode"),
                root.get("productName"),
                totalStockExpr,                        // total stock
                deadStockExpr,                         // dead stock
                root.get("measurementUnit"),
                cb.greatest(root.<Date>get("syncedAt")), // latest sync
                reorderLevelExpr,                      // effective reorder level (MAX across warehouse rows)
                root.get("categoryName")
        ));

    /* =========================
       WHERE / GROUP BY / HAVING
       ========================= */
        if (!wherePredicates.isEmpty()) {
            cq.where(cb.and(wherePredicates.toArray(new Predicate[0])));
        }

        cq.groupBy(
                root.get("tenantSchema"),
                root.get("productId"),
                root.get("productCode"),
                root.get("productName"),
                root.get("measurementUnit"),
                root.get("categoryName")
        );

        if (!havingPredicates.isEmpty()) {
            cq.having(cb.and(havingPredicates.toArray(new Predicate[0])));
        }

    /* =========================
       SORTING
       ========================= */
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();

            for (Sort.Order sort : pageable.getSort()) {
                switch (sort.getProperty()) {

                    case "productId":
                        orders.add(sort.isAscending()
                                ? cb.asc(root.get("productId"))
                                : cb.desc(root.get("productId")));
                        break;

                    case "quantityInHand":
                        orders.add(sort.isAscending()
                                ? cb.asc(totalStockExpr)
                                : cb.desc(totalStockExpr));
                        break;

                    case "deadStock":
                        orders.add(sort.isAscending()
                                ? cb.asc(deadStockExpr)
                                : cb.desc(deadStockExpr));
                        break;
                }
            }
            cq.orderBy(orders);
        }

        TypedQuery<StockSummaryAggregatedDTO> query =
                em.createQuery(cq);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<StockSummaryAggregatedDTO> content =
                query.getResultList();

    /* =========================
       COUNT QUERY (for pagination)
       ========================= */
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<StockSummary> countRoot = countCq.from(StockSummary.class);

        countCq.select(cb.count(countRoot));

        List<Predicate> countPredicates = new ArrayList<>();

// reuse SAME where predicates (but rebuilt on countRoot!)
        if (filters != null && filters.getFilterData() != null) {
            for (FilterAttributeData fad : filters.getFilterData()) {

                String attr = fad.getAttrName();
                List<String> values = fad.getAttrValue();

                if (values == null || values.isEmpty()) {
                    continue;
                }

                switch (attr) {
                    case "tenants":
                        countPredicates.add(countRoot.get("tenantSchema").in(values));
                        break;
                    case "productId":
                        countPredicates.add(
                                countRoot.get("productId").in(
                                        values.stream().map(Long::valueOf).collect(Collectors.toList())
                                )
                        );
                        break;
                    case "productCodes":
                        countPredicates.add(countRoot.get("productCode").in(values));
                        break;
                    case "productNames":
                        countPredicates.add(
                                cb.lower(countRoot.get("productName"))
                                        .in(values.stream().map(String::toLowerCase).collect(Collectors.toList()))
                        );
                        break;
                    case "categoryNames":
                        countPredicates.add(countRoot.get("categoryName").in(values));
                        break;
                    case "globalSearch":
                        List<Predicate> ors = new ArrayList<>();
                        for (String term : values) {
                            String pattern = "%" + term.toLowerCase() + "%";
                            ors.add(cb.like(cb.lower(countRoot.get("tenantSchema")), pattern));
                            ors.add(cb.like(cb.lower(countRoot.get("productCode")), pattern));
                            ors.add(cb.like(cb.lower(countRoot.get("productName")), pattern));
                        }
                        countPredicates.add(cb.or(ors.toArray(new Predicate[0])));
                        break;
                }
            }
        }

        if (!countPredicates.isEmpty()) {
            countCq.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

/*
 IMPORTANT:
 We must GROUP BY the same fields as the main query
*/
        countCq.groupBy(
                countRoot.get("tenantSchema"),
                countRoot.get("productId"),
                countRoot.get("productCode"),
                countRoot.get("productName"),
                countRoot.get("measurementUnit"),
                countRoot.get("categoryName")
        );

        // Rebuild aggregate expressions on countRoot for HAVING conditions
        Expression<Double> countDeadStockExpr = cb.sum(
                cb.<Double>selectCase()
                        .when(cb.equal(countRoot.get("warehouseName"), "Dead Stock Warehouse"),
                                countRoot.<Double>get("quantityInHand"))
                        .otherwise(0.0)
        );
        Expression<Double> countTotalStockExpr = cb.sum(countRoot.<Double>get("quantityInHand"));
        Expression<Double> countReorderLevelExpr = cb.max(countRoot.<Double>get("reorderLevel"));

        List<Predicate> countHavingPredicates = new ArrayList<>();
        if (filters != null && filters.getFilterData() != null) {
            for (FilterAttributeData fad : filters.getFilterData()) {
                String havingAttr = fad.getAttrName();
                List<String> havingValues = fad.getAttrValue();
                if (havingValues == null || havingValues.isEmpty()) continue;
                switch (havingAttr) {
                    case "deadStockPresent":
                        if (Boolean.parseBoolean(havingValues.get(0))) {
                            countHavingPredicates.add(cb.greaterThan(countDeadStockExpr, 0.0));
                        }
                        break;
                    case "lowStock":
                        if (Boolean.parseBoolean(havingValues.get(0))) {
                            countHavingPredicates.add(cb.isNotNull(countReorderLevelExpr));
                            countHavingPredicates.add(
                                    cb.lessThanOrEqualTo(countTotalStockExpr, countReorderLevelExpr));
                        }
                        break;
                }
            }
        }
        if (!countHavingPredicates.isEmpty()) {
            countCq.having(cb.and(countHavingPredicates.toArray(new Predicate[0])));
        }

        Long total = (long) em.createQuery(countCq).getResultList().size();
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

    @Override
    public StockSummaryTilesDTO getTileCounts(FilterDataList filters) {
        StockSummaryTilesDTO dto = new StockSummaryTilesDTO();

        // Build optional tenant WHERE clause from filter
        List<String> tenants = new ArrayList<>();
        if (filters != null && filters.getFilterData() != null) {
            for (FilterAttributeData fad : filters.getFilterData()) {
                if ("tenants".equals(fad.getAttrName()) && fad.getAttrValue() != null) {
                    tenants.addAll(fad.getAttrValue());
                }
            }
        }
        String tenantWhere = tenants.isEmpty() ? "" :
                " AND tenantSchema IN (" + tenants.stream().map(t -> "'" + t.replace("'", "''") + "'").collect(Collectors.joining(",")) + ")";

        List<?> lowStockRows = em.createNativeQuery(
                "SELECT COUNT(*) FROM (" +
                "  SELECT tenantSchema, productId FROM stock_summary" +
                "  WHERE is_deleted = 0" + tenantWhere +
                "  GROUP BY tenantSchema, productId" +
                "  HAVING MAX(reorder_level) IS NOT NULL AND SUM(quantityInHand) <= MAX(reorder_level)" +
                ") t"
        ).getResultList();
        dto.setLowStockCount(lowStockRows.isEmpty() ? 0L : ((Number) lowStockRows.get(0)).longValue());

        List<?> deadStockRows = em.createNativeQuery(
                "SELECT COUNT(*) FROM (" +
                "  SELECT tenantSchema, productId FROM stock_summary" +
                "  WHERE is_deleted = 0" + tenantWhere +
                "  GROUP BY tenantSchema, productId" +
                "  HAVING SUM(CASE WHEN warehouseName = 'Dead Stock Warehouse' THEN quantityInHand ELSE 0 END) > 0" +
                ") t"
        ).getResultList();
        dto.setDeadStockCount(deadStockRows.isEmpty() ? 0L : ((Number) deadStockRows.get(0)).longValue());

        return dto;
    }
}
