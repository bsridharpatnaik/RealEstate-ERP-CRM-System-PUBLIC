package com.ec.common.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.model.AllInventoryTransactions_;
import com.ec.application.model.InventoryReport;
import com.ec.application.model.InventoryReport_;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;


public class InventoryReportSpecification
{
static SpecificationsBuilder<InventoryReport> specbldr = new SpecificationsBuilder<InventoryReport>();
	

	public static Specification<InventoryReport> getSpecification(FilterDataList filterDataList) throws ParseException {
		List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"products");
		List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"categories");
		List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"warehouses");
		List<String> month = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"month");

		Specification<InventoryReport> finalSpec = null;

		if(productNames != null && productNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldEquals(InventoryReport_.PRODUCT_NAME, productNames));

		if(categoryNames != null && categoryNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldEquals(InventoryReport_.CATEGORY_NAME, categoryNames));

		if(warehouseNames != null && warehouseNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldEquals(InventoryReport_.WAREHOUSE_NAME, warehouseNames));
		
		if(month != null && month.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldEquals(InventoryReport_.MONTH, month));

		return finalSpec;
	}
}
