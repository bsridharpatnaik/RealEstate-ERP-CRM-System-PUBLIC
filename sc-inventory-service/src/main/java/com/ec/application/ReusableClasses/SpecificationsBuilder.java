package com.ec.application.ReusableClasses;


import javax.persistence.criteria.*;

import com.ec.application.constants.POStatusConstants;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.Filters.BOQStatusFilterDataList;
import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SpecificationsBuilder<T> {

    String dateFormat = ProjectConstants.dateFormat;
    // #######################################//
    // Level 0 //
    // #######################################//

    public Specification<T> whereDirectFieldContains(String key, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .like(root.get(key), "%" + name + "%");
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereDirectBoleanFieldEquals(String key, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(key), Boolean.parseBoolean(name));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereDirectFieldEquals(String key, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(key), name);
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereDirectFieldDateGreaterThan(String key, List<String> startDates) throws ParseException {
        Date startDate = ReusableMethods.atStartOfDay(new SimpleDateFormat(dateFormat).parse(startDates.get(0)));
        Specification<T> finalSpec = null;
        Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .greaterThanOrEqualTo(root.get(key), startDate);
        finalSpec = specOrCondition(finalSpec, internalSpec);
        return finalSpec;
    }

    public Specification<T> whereDirectFieldLongBetween(String key, Long lowerLimit, Long upperLimit)
            throws ParseException {

        Specification<T> finalSpec = null;
        Specification<T> internalSpec1 = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .lessThanOrEqualTo(root.get(key), lowerLimit);
        Specification<T> internalSpec2 = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .greaterThanOrEqualTo(root.get(key), upperLimit);
        finalSpec = specAndCondition(internalSpec1, internalSpec2);
        return finalSpec;
    }

//    public Specification<T> whereDirectFieldDoubleBetween(String key, Double lowerLimit, Double upperLimit)
//            throws ParseException {
//
//        Specification<T> finalSpec = null;
//        Specification<T> internalSpec1 = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
//                .lessThanOrEqualTo(root.get(key), upperLimit);
//        Specification<T> internalSpec2 = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
//                .greaterThanOrEqualTo(root.get(key), lowerLimit);
//        finalSpec = specAndCondition(internalSpec1, internalSpec2);
//        return finalSpec;
//    }
//    

    public Specification<T> whereDirectFieldDateLessThan(String key, List<String> endDates) throws ParseException {
        Date startDate = ReusableMethods.atEndOfDay(new SimpleDateFormat(dateFormat).parse(endDates.get(0)));
        Specification<T> finalSpec = null;
        Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .lessThanOrEqualTo(root.get(key), startDate);
        finalSpec = specOrCondition(finalSpec, internalSpec);
        return finalSpec;
    }

    public Specification<T> whereDirectFieldDoubleGreaterThan(String key, Double value) {
        Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .greaterThan(root.get(key), value);
        return internalSpec;
    }

    public Specification<T> whereDirectFieldLongFieldContains(String key, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(key), Long.parseLong(name));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    // #######################################//
    // Level 1 //
    // #######################################//

    public Specification<T> whereChildFieldContains(String childTable, String childFiledName, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .like(root.get(childTable).get(childFiledName), "%" + name + "%");
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereChildFieldEquals(String childTable, String childFiledName, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(childTable).get(childFiledName), name);
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    // #######################################//
    // Level 2 //
    // #######################################//

    public Specification<T> whereGrandChildFieldContains(String childTable, String grandChildTable,
                                                         String grandChildFiledName, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .like(root.get(childTable).get(grandChildTable).get(grandChildFiledName), "%" + name + "%");
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereChildFieldListContains(String childTableName, String gcTable, String fieldName,
                                                        List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .like(root.join(childTableName).join(gcTable).get(fieldName), "%" + name + "%");
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereCategoryContains(List<String> categoryNames, String joinTable) {

        return (root, query, cb) ->
        {

            Join<T, InwardOutwardList> ioList = root.join(joinTable);
            Join<InwardOutwardList, Product> productList = ioList.join(InwardOutwardList_.PRODUCT);
            Join<Product, Category> categoryList = productList.join(Product_.CATEGORY);
            query.distinct(true);
            Expression<String> parentExpression = categoryList.get(Category_.categoryName);
            Predicate parentPredicate = parentExpression.in(categoryNames);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public Specification<T> whereWarehouseContains(List<String> warehouseNames, String joinTable) {

        return (root, query, cb) ->
        {

            Join<T, InwardOutwardList> ioList = root.join(joinTable);
            Join<InwardOutwardList, Product> productList = ioList.join(InwardOutwardList_.PRODUCT);
            Join<Product, Category> categoryList = productList.join(Product_.CATEGORY);
            query.distinct(true);
            Expression<String> parentExpression = categoryList.get(Category_.categoryName);
            Predicate parentPredicate = parentExpression.in(warehouseNames);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public Specification<T> whereIndentCategoryContains(List<String> categoryNames, String joinTable) {

        return (root, query, cb) ->
        {

            Join<T, InwardOutwardList> ioList = root.join(joinTable);
            Join<InwardOutwardList, Product> productList = ioList.join(IndentInventoryList_.PRODUCT);
            Join<Product, Category> categoryList = productList.join(Product_.CATEGORY);
            query.distinct(true);
            Expression<String> parentExpression = categoryList.get(Category_.categoryName);
            Predicate parentPredicate = parentExpression.in(categoryNames);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public Specification<T> whereProductContains(List<String> productNames, String joinTable) {
        return (root, query, cb) ->
        {

            Join<T, InwardOutwardList> ioList = root.join(joinTable);
            Join<InwardOutwardList, Product> productList = ioList.join(InwardOutwardList_.PRODUCT);
            query.distinct(true);
            Expression<String> parentExpression = productList.get(Product_.PRODUCT_NAME);
            Predicate parentPredicate = parentExpression.in(productNames);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public Specification<T> wherePurchanseOrderContainsProductName(List<String> productNames, String joinTable) {
        return (root, query, cb) ->
        {

            Join<T, PurchaseOrderLine> ioList = root.join(joinTable);
            Join<PurchaseOrderLine, Product> productList = ioList.join(PurchaseOrderLine_.PRODUCT);
            query.distinct(true);
            Expression<String> parentExpression = productList.get(Product_.PRODUCT_NAME);
            Predicate parentPredicate = parentExpression.in(productNames);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public Specification<T> wherePurchanseOrderContainsProductCode(List<String> productNames, String joinTable) {
        return (root, query, cb) ->
        {

            Join<T, PurchaseOrderLine> ioList = root.join(joinTable);
            Join<PurchaseOrderLine, Product> productList = ioList.join(PurchaseOrderLine_.PRODUCT);
            query.distinct(true);
            Expression<String> parentExpression = productList.get(Product_.PRODUCT_CODE);
            Predicate parentPredicate = parentExpression.in(productNames);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public Specification<T> wherePurchaseOrderCategoryContains(List<String> categoryNames, String joinTable) {

        return (root, query, cb) ->
        {
            Join<T, PurchaseOrderLine> ioList = root.join(joinTable);
            Join<PurchaseOrderLine, Product> productList = ioList.join(PurchaseOrderLine_.PRODUCT);
            Join<Product, Category> categoryList = productList.join(Product_.CATEGORY);
            query.distinct(true);
            Expression<String> parentExpression = categoryList.get(Category_.categoryName);
            Predicate parentPredicate = parentExpression.in(categoryNames);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public Specification<T> whereIndentContainsProductName(List<String> productNames, String joinTable) {

        return (root, query, cb) ->
        {

            Join<T, InwardOutwardList> ioList = root.join(joinTable);
            Join<InwardOutwardList, Product> productList = ioList.join(IndentInventoryList_.PRODUCT);
            query.distinct(true);
            Expression<String> parentExpression = productList.get(Product_.PRODUCT_NAME);
            Predicate parentPredicate = parentExpression.in(productNames);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public Specification<T> whereIndentContainsProductCode(List<String> productCodes, String joinTable) {

        return (root, query, cb) ->
        {
            Join<T, InwardOutwardList> ioList = root.join(joinTable);
            Join<InwardOutwardList, Product> productList = ioList.join(IndentInventoryList_.PRODUCT);
            query.distinct(true);
            Expression<String> parentExpression = productList.get(Product_.PRODUCT_CODE);
            Predicate parentPredicate = parentExpression.in(productCodes);
            query.where(parentPredicate);
            return query.getRestriction();
        };
    }

    public <T> Specification<T> whereIndentContainsLineItemStatus(List<String> lineItemStatuses, String joinTable) {
        return (root, query, cb) -> {

            if (lineItemStatuses == null || lineItemStatuses.isEmpty()) {
                return cb.conjunction(); // no filtering
            }

            Join<T, IndentInventoryList> lineItemJoin = root.join(joinTable, JoinType.INNER);
            query.distinct(true);
            return lineItemJoin.get(IndentInventoryList_.lineItemStatus).in(lineItemStatuses);
        };
    }

    // #######################################//
    // Reusable Spec Setter for NULLs //
    // #######################################//

    public Specification<T> specAndCondition(Specification<T> finalSpec, Specification<T> internalSpec) {
        if (finalSpec == null)
            return internalSpec;
        else
            return finalSpec.and(internalSpec);
    }

    public Specification<T> specOrCondition(Specification<T> finalSpec, Specification<T> interalSpec) {
        if (finalSpec == null)
            return interalSpec;
        else
            return finalSpec.or(interalSpec);
    }

    // ############################################//
    // Reusable method to fetch filter Data //
    // ############################################//

    public static List<String> fetchValueFromFilterList(FilterDataList filterDataList, String field) {
        List<String> returnValue = null;
        for (FilterAttributeData filterData : filterDataList.getFilterData()) {
            if (filterData.getAttrName().equalsIgnoreCase(field))
                returnValue = filterData.getAttrValue();
        }
        return returnValue;
    }

    public static List<String> fetchValueFromBoqFilterList(BOQStatusFilterDataList filterDataList, String field) {
        List<String> returnValue = null;
        for (FilterAttributeData filterData : filterDataList.getFilterData()) {
            if (filterData.getAttrName().equalsIgnoreCase(field))
                returnValue = filterData.getAttrValue();
        }
        return returnValue;
    }

    public Specification<IndentInventory> whereIndentLastStatusUpdatedBefore(Date cutoffDate) {
        return (root, query, cb) ->
                cb.lessThan(root.get(IndentInventory_.LAST_STATUS_UPDATED_AT), ReusableMethods.atEndOfDay(cutoffDate));
    }

    public Specification<PurchaseOrder> wherePOLastStatusUpdatedBefore(Date cutoffDate) {
        return (root, query, cb) ->
                cb.lessThan(root.get(PurchaseOrder_.LAST_STATUS_UPDATED_AT), ReusableMethods.atEndOfDay(cutoffDate));
    }

    public Specification<IndentInventory> whereIndentStatusNotIn(List<String> statuses) {
        return (root, query, cb) ->
                cb.not(root.get(IndentInventory_.INDENT_STATUS).in(statuses));
    }

    public Specification<PurchaseOrder> wherePOStatusNotIn(List<String> statuses) {
        return (root, query, cb) ->
                cb.not(root.get(PurchaseOrder_.STATUS).in(statuses));
    }
}
