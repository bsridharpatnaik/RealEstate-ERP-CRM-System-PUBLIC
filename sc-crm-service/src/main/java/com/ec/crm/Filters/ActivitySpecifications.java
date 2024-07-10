package com.ec.crm.Filters;

import java.util.*;

import com.ec.crm.Enums.*;
import com.ec.crm.Model.*;
import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.ReusableClasses.ReusableMethods;
import com.ec.crm.ReusableClasses.SpecificationsBuilder;

import javax.persistence.criteria.*;

public class ActivitySpecifications {
    static SpecificationsBuilder<LeadActivity> specbldr = new SpecificationsBuilder<LeadActivity>();

    public static Specification<LeadActivity> getSpecification(FilterDataList filterDataList) throws Exception {
        List<String> name = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");
        List<String> mobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "mobile");
        List<String> purpose = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "purpose");
        List<String> textSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "textSearch");
        List<String> address = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "address");
        List<String> occupation = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "occupation");
        List<String> broker = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "broker");
        List<String> source = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "source");
        List<String> propertytype = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "propertyType");
        List<String> sentiment = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "sentiment");
        List<String> assignee = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "assignee");
        List<String> createdStartDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
                "createdStartDate");
        List<String> createdEndDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "createdEndDate");
        List<String> leadStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "leadStatus");
        List<String> activityType = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "activityType");
        List<String> activityStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "activityStatus");
        List<String> activityStartDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
                "activityStartDate");
        List<String> activityEndDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
                "activityEndDate");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> stagnantStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "stagnantStatus");
        List<String> loanStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "loanStatus");
        List<String> customerStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "customerStatus");
        List<String> dealLostReason = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "dealLostReason");

        List<String> showOnlyLatest = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "showOnlyLatest");
        List<String> prospectLeads = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "prospectLead");
        List<String> showRescheduled = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
                "showRescheduled");
        Specification<LeadActivity> finalSpec = null;

        if (name != null && name.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.customerName.getName(), name));

        if (loanStatus != null && loanStatus.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.loanStatus.getName(), loanStatus));

        if (prospectLeads != null && prospectLeads.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildBoleanFieldEquals(LeadActivity_.LEAD, Lead_.IS_PROSPECT_LEAD, prospectLeads));

        if (customerStatus != null && customerStatus.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.customerStatus.getName(), customerStatus));

        if (mobile != null && mobile.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.PRIMARY_MOBILE, mobile)
                            .or(specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.SECONDARY_MOBILE, mobile)));

        if (purpose != null && purpose.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.PURPOSE, purpose));

        if (address != null && address.size() > 0) {
            Specification<LeadActivity> addressSpec = specbldr
                    .whereGrandChildFieldContains(LeadActivity_.LEAD, Lead_.ADDRESS, Address_.ADDR_LINE1, address)
                    .or(specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD, Lead_.ADDRESS, Address_.ADDR_LINE2,
                            address))
                    .or(specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD, Lead_.ADDRESS, Address_.CITY,
                            address))
                    .or(specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD, Lead_.ADDRESS, Address_.PINCODE,
                            address));
            finalSpec = specbldr.specAndCondition(finalSpec, addressSpec);

        }

        if (globalSearch != null && globalSearch.size() > 0) {
            Specification<LeadActivity> globalSearchSpec = Objects.requireNonNull(specbldr
                    .whereChildFieldContains(LeadActivity_.LEAD, Lead_.CUSTOMER_NAME, globalSearch)
                    .or(specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.PRIMARY_MOBILE, globalSearch)));
            finalSpec = specbldr.specAndCondition(finalSpec, globalSearchSpec);
        }

        if (textSearch != null && textSearch.size() > 0) {
            Specification<LeadActivity> globalSearchSpec = Objects.requireNonNull(
                    specbldr
                            .whereChildFieldContains(LeadActivity_.LEAD, Lead_.CUSTOMER_NAME, textSearch)
                            .or(specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.PRIMARY_MOBILE, textSearch))
                            .or(specbldr.whereDirectFieldContains(LeadActivity_.CLOSING_COMMENT, textSearch))
                            .or(specbldr.whereDirectFieldContains(LeadActivity_.DESCRIPTION, textSearch))
                            .or(specbldr.whereDirectFieldContains(LeadActivity_.TITLE, textSearch))
                            .or(specbldr.whereDirectFieldContains(LeadActivity_.TAGS_GROUPED, textSearch))
                            .or(specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.NOTES, textSearch))
            );
            finalSpec = specbldr.specAndCondition(finalSpec, globalSearchSpec);
        }

        if (occupation != null && occupation.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains(LeadActivity_.LEAD, Lead_.OCCUPATION, occupation));

        if (broker != null && broker.size() > 0) {
            if (ReusableMethods.isNumeric(broker.get(0)))
                finalSpec = specbldr.specAndCondition(finalSpec, specbldr
                        .whereGrandChildLongFieldContains(LeadActivity_.LEAD, Lead_.BROKER, Broker_.BROKER_ID, broker));
            else
                finalSpec = specbldr.specAndCondition(finalSpec, specbldr
                        .whereGrandChildFieldContains(LeadActivity_.LEAD, Lead_.BROKER, Broker_.BROKER_NAME, broker));
        }
        if (broker != null && broker.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD, Lead_.BROKER, Broker_.BROKER_NAME, broker)
                            .or(specbldr.whereGrandChildLongFieldContains(LeadActivity_.LEAD, Lead_.BROKER,
                                    Broker_.BROKER_ID, broker)));

        if (source != null && source.size() > 0) {
            if (ReusableMethods.isNumeric(source.get(0))) {
                finalSpec = specbldr.specAndCondition(finalSpec, specbldr
                        .whereGrandChildLongFieldContains(LeadActivity_.LEAD, Lead_.SOURCE, Source_.SOURCE_ID, source));
            } else {
                finalSpec = specbldr.specAndCondition(finalSpec, specbldr
                        .whereGrandChildFieldContains(LeadActivity_.LEAD, Lead_.SOURCE, Source_.SOURCE_NAME, source));
            }
        }
        if (propertytype != null && propertytype.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildEnumFieldEquals(LeadActivity_.LEAD,
                    Lead_.PROPERTY_TYPE, propertytype, PropertyTypeEnum.class));

        if (sentiment != null && sentiment.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildEnumFieldEquals(LeadActivity_.LEAD,
                    Lead_.SENTIMENT, sentiment, SentimentEnum.class));

        if (assignee != null && assignee.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildAssigneeContains(LeadActivity_.LEAD, Lead_.ASIGNEE_ID, assignee));

        if (createdStartDate != null && createdStartDate.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldDateGreaterThan(LeadActivity_.LEAD, Lead_.CREATED, createdStartDate));

        if (createdEndDate != null && createdEndDate.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldDateLessThan(LeadActivity_.LEAD, Lead_.CREATED, createdEndDate));

        if (leadStatus != null && leadStatus.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildEnumFieldEquals(LeadActivity_.LEAD,
                    Lead_.STATUS, leadStatus, LeadStatusEnum.class));

        if (dealLostReason != null && dealLostReason.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereEnumFieldEquals(LeadActivity_.dealLostReason.getName(),
                    dealLostReason, DealLostReasonEnum.class));

        if (activityType != null && activityType.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereEnumFieldEquals(LeadActivity_.ACTIVITY_TYPE, activityType, ActivityTypeEnum.class));

        if (activityStatus != null && activityStatus.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectBooleanFieldEquals(LeadActivity_.IS_OPEN, activityStatus));

        if (activityStartDate != null && activityStartDate.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr
                    .whereDirectFieldDateGreaterThanOrEqual(LeadActivity_.ACTIVITY_DATE_TIME, activityStartDate));

        if (activityEndDate != null && activityEndDate.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThanOrEqual(LeadActivity_.ACTIVITY_DATE_TIME, activityEndDate));

        if (showOnlyLatest != null && showOnlyLatest.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectIntFieldEquals(LeadActivity_.IS_LATEST, showOnlyLatest));

        if (showRescheduled != null && showRescheduled.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectIntFieldEquals(LeadActivity_.IS_RESCHEDULED, showRescheduled));

        if (stagnantStatus != null && stagnantStatus.size() > 0) {
            Specification<LeadActivity> internalSpec = null;
            for (String str : stagnantStatus) {
                if (str.equalsIgnoreCase("NoColour"))
                    internalSpec = specbldr.specOrCondition(internalSpec,
                            specbldr.whereChildFieldIntBetween(LeadActivity_.LEAD, Lead_.STAGNANT_DAYS_COUNT, 0, 9));
                if (str.equalsIgnoreCase("Green"))
                    internalSpec = specbldr.specOrCondition(internalSpec,
                            specbldr.whereChildFieldIntBetween(LeadActivity_.LEAD, Lead_.STAGNANT_DAYS_COUNT, 10, 19));
                if (str.equalsIgnoreCase("Orange"))
                    internalSpec = specbldr.specOrCondition(internalSpec,
                            specbldr.whereChildFieldIntBetween(LeadActivity_.LEAD, Lead_.STAGNANT_DAYS_COUNT, 20, 29));
                if (str.equalsIgnoreCase("Red"))
                    internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldIntBetween(
                            LeadActivity_.LEAD, Lead_.STAGNANT_DAYS_COUNT, 30, Integer.MAX_VALUE));
            }
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }


        return finalSpec;
    }


    public static Specification<LeadActivity> noteContentContains(String searchString) {
        return (root, query, criteriaBuilder) -> {
            Join<LeadActivity, Lead> leadJoin = root.join("lead", JoinType.INNER);
            Join<Lead, Note> noteJoin = leadJoin.join("notes", JoinType.INNER);
            return criteriaBuilder.like(noteJoin.get("content").as(String.class), "%" + searchString + "%");
        };
    }

    public static Specification<LeadActivity> hasTag(String tag) {
        return (root, query, builder) -> {
            Join<LeadActivity, String> tagsJoin = root.join("tags", JoinType.INNER);
            return builder.equal(tagsJoin, tag);
        };
    }

    public static Specification<LeadActivity> searchStringInNoteContent(String searchString) {
        return new Specification<LeadActivity>() {
            @Override
            public Predicate toPredicate(Root<LeadActivity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                Subquery<Note> noteSubquery = query.subquery(Note.class);
                Root<Note> noteRoot = noteSubquery.from(Note.class);
                noteSubquery.select(noteRoot.get("lead"))
                        .where(criteriaBuilder.like(noteRoot.get("content"), "%" + searchString + "%"));
                query.distinct(true); // Add DISTINCT to the main query
                return criteriaBuilder.in(root.get("lead")).value(noteSubquery);
            }
        };
    }
}
