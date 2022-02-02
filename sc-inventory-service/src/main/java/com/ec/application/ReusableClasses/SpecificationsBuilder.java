package com.ec.application.ReusableClasses;


import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.ec.application.config.ProjectConstants;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;

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
        Date startDate = new SimpleDateFormat(dateFormat).parse(startDates.get(0));
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

    public Specification<T> whereDirectFieldDateLessThan(String key, List<String> endDates) throws ParseException {
        Date startDate = new SimpleDateFormat(dateFormat).parse(endDates.get(0));
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
}
