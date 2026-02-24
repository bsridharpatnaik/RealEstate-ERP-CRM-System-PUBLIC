package com.ec.application.Filters;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.*;

import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;

public final class InwardInventorySpecification
{
	static SpecificationsBuilder<InwardInventory> specbldr = new SpecificationsBuilder<InwardInventory>();

	public static Specification<InwardInventory> getSpecification(FilterDataList filterDataList) throws ParseException {
		List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
		List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
		List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
		List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
		List<String> supplierNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "supplierNames");
		List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouseNames");
		List<String> invoiceReceived = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
				"invoiceReceived");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
		List<String> showOnlyRejected = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
				"showOnlyRejected");
		List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
		List<String> textSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "textSearch");
		Specification<InwardInventory> finalSpec = null;

		if (startDates != null && startDates.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateGreaterThan(InwardInventory_.DATE, startDates));

		if (endDates != null && endDates.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateLessThan(InwardInventory_.DATE, endDates));

		if (productNames != null && productNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereProductContains(productNames, InwardInventory_.INWARD_OUTWARD_LIST));

		if (categoryNames != null && categoryNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereCategoryContains(categoryNames, InwardInventory_.INWARD_OUTWARD_LIST));

		if (supplierNames != null && supplierNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereChildFieldContains(InwardInventory_.SUPPLIER, Supplier_.NAME, supplierNames));

		if (warehouseNames != null && warehouseNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, hasWarehouseNamesIgnoreCase(warehouseNames));

		if (invoiceReceived != null && invoiceReceived.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectBoleanFieldEquals(InwardInventory_.INVOICE_RECEIVED, invoiceReceived));

		if (textSearch != null && textSearch.size() > 0) {
			Specification<InwardInventory> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(InwardInventory_.BILL_NO,textSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(InwardInventory_.CHALLAN_NO,textSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(InwardInventory_.ADDITIONAL_INFO,textSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(InwardInventory_.PURCHASE_ORDER_NO,textSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(InwardInventory_.OUR_SLIP_NO,textSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereDirectFieldContains(InwardInventory_.VEHICLE_NO,textSearch));
			finalSpec = specbldr.specAndCondition(finalSpec,
					internalSpec);
		}

		if (globalSearch != null && globalSearch.size() > 0)
		{
			Specification<InwardInventory> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereChildFieldContains(InwardInventory_.SUPPLIER, Supplier_.NAME, globalSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereProductContains(globalSearch, InwardInventory_.INWARD_OUTWARD_LIST));
			finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
		}

		if (showOnlyRejected != null && showOnlyRejected.size() == 1)
		{
			if (showOnlyRejected.get(0).toLowerCase().equals("true"))
			{
				Specification<InwardInventory> internalSpec = (Root<InwardInventory> root, CriteriaQuery<?> query,
						CriteriaBuilder cb) -> cb.greaterThan(cb.size(root.get(InwardInventory_.REJECT_INWARD_LIST)),
								0);
				// cb.like(root.get(childTable).get(childFiledName), "%" + name + "%");
				finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
			}
		}
		return finalSpec;
	}

	public static Specification<InwardInventory> hasWarehouseNamesIgnoreCase(List<String> warehouseNames) {
		return (root, query, cb) -> {

			if (warehouseNames == null || warehouseNames.isEmpty()) {
				return null;
			}

			query.distinct(true);

			Join<InwardInventory, InwardOutwardList> inwardOutwardJoin =
					root.join(InwardInventory_.inwardOutwardList, JoinType.LEFT);

			Join<InwardOutwardList, Warehouse> warehouseJoin =
					inwardOutwardJoin.join(InwardOutwardList_.warehouse, JoinType.LEFT);

			List<String> lowerCaseNames = warehouseNames.stream()
					.map(String::toLowerCase)
					.collect(Collectors.toList());

			return cb.lower(warehouseJoin.get(Warehouse_.warehouseName))
					.in(lowerCaseNames);
		};
	}

}
