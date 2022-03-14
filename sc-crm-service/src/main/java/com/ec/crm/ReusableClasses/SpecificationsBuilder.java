package com.ec.crm.ReusableClasses;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.ec.crm.Filters.FilterAttributeData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Service.UserDetailsService;

@Component
public class SpecificationsBuilder<T> {
    @Autowired
    private WebApplicationContext context;

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

    public Specification<T> whereEnumFieldEquals(String key, List<String> names, Class claz) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            @SuppressWarnings("unchecked")
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(key), getEnum(name, claz));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereProperytyEnumFieldEquals(String key, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(key), PropertyTypeEnum.valueOf(name));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereSentimentEnumFieldEquals(String key, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(key), SentimentEnum.valueOf(name));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereDirectFieldDateGreaterThanOrEqual(String key, List<String> startDates)
            throws ParseException {
        Date startDate = new SimpleDateFormat("dd-MM-yyyy").parse(startDates.get(0));
        Specification<T> finalSpec = null;
        Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .greaterThanOrEqualTo(root.get(key), ReusableMethods.atStartOfDay(startDate));
        finalSpec = specOrCondition(finalSpec, internalSpec);
        return finalSpec;
    }

    public Specification<T> whereDirectFieldDateLessThanOrEqual(String key, List<String> endDates) throws ParseException {
        Date endDate = new SimpleDateFormat("dd-MM-yyyy").parse(endDates.get(0));
        Specification<T> finalSpec = null;
        Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .lessThanOrEqualTo(root.get(key), ReusableMethods.atEndOfDay(endDate));
        finalSpec = specOrCondition(finalSpec, internalSpec);
        return finalSpec;
    }

    public Specification<T> whereDirectLongFieldContains(String key, List<String> assignees) {
        Specification<T> finalSpec = null;
        for (String name : assignees) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(key), Long.parseLong(name));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereDirectIntFieldEquals(String key, List<String> isLatestFlagList) throws Exception {
        Specification<T> finalSpec = null;
        if (isLatestFlagList.size() != 1)
            throw new Exception("More than one value received for dropdown filter field");
        try {
            int isLatestFlag = isLatestFlagList.get(0).equals("true") ? 1 : 0;
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(key), isLatestFlag);
            finalSpec = specOrCondition(finalSpec, internalSpec);
        } catch (Exception e) {
            throw new Exception("Not able to parse ");
        }
        return finalSpec;

    }

    public Specification<T> whereDirectAssigneeContains(String key, List<String> assignees) throws Exception {
        UserDetailsService userDetailsService = BeanUtil.getBean(UserDetailsService.class);
        Specification<T> finalSpec = null;
        for (String name : assignees) {
            if (name.matches("\\d*")) {
                Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                        .equal(root.get(key), Long.parseLong(name));
                finalSpec = specOrCondition(finalSpec, internalSpec);
            } else {
                Long userid = userDetailsService.getIdFromUsername(name).getId();
                Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                        .equal(root.get(key), userid);
                finalSpec = specOrCondition(finalSpec, internalSpec);
            }
        }
        return finalSpec;
    }

    public Specification<T> whereDirectFieldIntBetween(String key, int lowerBound, int upperBound) throws ParseException {

        Specification<T> finalSpec = null;
        Specification<T> internalSpec1 = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .greaterThanOrEqualTo(root.get(key), lowerBound);
        Specification<T> internalSpec2 = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .lessThanOrEqualTo(root.get(key), upperBound);
        finalSpec = specAndCondition(internalSpec1, internalSpec2);
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

    public Specification<T> whereChildFieldDateGreaterThan(String childTable, String childFiledName,
                                                           List<String> startDates) throws ParseException {
        Date startDate = new SimpleDateFormat("dd-MM-yyyy").parse(startDates.get(0));
        Specification<T> finalSpec = null;
        Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .greaterThanOrEqualTo(root.get(childTable).get(childFiledName),
                        ReusableMethods.atStartOfDay(startDate));
        finalSpec = specOrCondition(finalSpec, internalSpec);
        return finalSpec;
    }

    public Specification<T> whereChildFieldDateLessThan(String childTable, String childFiledName, List<String> endDates)
            throws ParseException {
        Date endDate = new SimpleDateFormat("dd-MM-yyyy").parse(endDates.get(0));
        Specification<T> finalSpec = null;
        Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .lessThanOrEqualTo(root.get(childTable).get(childFiledName), ReusableMethods.atEndOfDay(endDate));
        finalSpec = specOrCondition(finalSpec, internalSpec);
        return finalSpec;
    }

    public Specification<T> whereChildLongFieldContains(String childTable, String childFiledName,
                                                        List<String> assignees) {
        Specification<T> finalSpec = null;
        for (String name : assignees) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(childTable).get(childFiledName), Long.parseLong(name));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereChildEnumFieldEquals(String childTable, String childFiledName, List<String> names,
                                                      Class claz) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            @SuppressWarnings("unchecked")
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(childTable).get(childFiledName), getEnum(name, claz));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereChildFieldIntBetween(String childTable, String childTableField, int lowerLimit,
                                                      int upperLimit) throws ParseException {

        Specification<T> finalSpec = null;
        Specification<T> internalSpec1 = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .greaterThanOrEqualTo(root.get(childTable).get(childTableField), lowerLimit);
        Specification<T> internalSpec2 = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .lessThanOrEqualTo(root.get(childTable).get(childTableField), upperLimit);
        finalSpec = specAndCondition(internalSpec1, internalSpec2);
        return finalSpec;
    }

    public Specification<T> whereChildBoleanFieldEquals(String childTable, String childFiledName, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(childTable).get(childFiledName), Boolean.parseBoolean(name));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public Specification<T> whereChildAssigneeContains(String childTable, String childFiledName, List<String> assignees)
            throws Exception {
        UserDetailsService userDetailsService = BeanUtil.getBean(UserDetailsService.class);
        Specification<T> finalSpec = null;
        for (String name : assignees) {
            if (name.matches("\\d*")) {
                Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                        .equal(root.get(childTable).get(childFiledName), Long.parseLong(name));
                finalSpec = specOrCondition(finalSpec, internalSpec);
            } else {
                Long userid = userDetailsService.getIdFromUsername(name.trim()).getId();
                Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                        .equal(root.get(childTable).get(childFiledName), userid);
                finalSpec = specOrCondition(finalSpec, internalSpec);
            }
        }
        return finalSpec;
    }

    // #######################################//
    // Level 2 //
    // #######################################//

    public Specification<T> whereGrandChildAssigneeContains(String childTable, String grandChildTable,
                                                            String grandChildFiledName, List<String> assignees) throws Exception {
        UserDetailsService userDetailsService = BeanUtil.getBean(UserDetailsService.class);
        Specification<T> finalSpec = null;
        for (String name : assignees) {
            if (name.matches("\\d*")) {
                Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb.equal(
                        root.get(childTable).get(grandChildTable).get(grandChildFiledName), Long.parseLong(name));
                finalSpec = specOrCondition(finalSpec, internalSpec);
            } else {
                Long userid = userDetailsService.getIdFromUsername(name.trim()).getId();
                Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                        .equal(root.get(childTable).get(grandChildTable).get(grandChildFiledName), userid);
                finalSpec = specOrCondition(finalSpec, internalSpec);
            }
        }
        return finalSpec;
    }

    public Specification<T> whereGrandChildLongFieldContains(String childTable, String grandChildTable,
                                                             String grandChildFiledName, List<String> ids) {
        Specification<T> finalSpec = null;
        for (String name : ids) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(childTable).get(grandChildTable).get(grandChildFiledName), Long.parseLong(name));
            finalSpec = specOrCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

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

    public Specification<T> whereGrandChildFieldEquals(String childTable, String grandChildTable,
                                                       String grandChildFiledName, List<String> names) {
        Specification<T> finalSpec = null;
        for (String name : names) {
            Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                    .equal(root.get(childTable).get(grandChildTable).get(grandChildFiledName), name);
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

    // #######################################//
    // Reusable Spec Setter for NULLs //
    // #######################################//

    public Specification<T> specAndCondition(Specification<T> finalSpec, Specification<T> internalSpec) {
        if (finalSpec == null)
            return internalSpec;
        else
            return finalSpec.and(internalSpec);
    }

    public Specification<T> specOrCondition(Specification<T> finalSpec, Specification<T> internalSpec) {
        if (finalSpec == null)
            return internalSpec;
        else
            return finalSpec.or(internalSpec);
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

    public static <E extends Enum<E>> E getEnum(String text, Class<E> klass) {
        return Enum.valueOf(klass, text);
    }
}