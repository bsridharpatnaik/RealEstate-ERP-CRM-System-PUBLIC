
package com.ec.common.Filters;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.Product_;
import com.ec.application.model.Stock;
import com.ec.application.model.Stock_;
import com.ec.application.model.Warehouse_;

public class StockSpecification
{
	static SpecificationsBuilder<Stock> specbldr = new SpecificationsBuilder<Stock>();

	public static Specification<Stock> getSpecification(FilterDataList filterDataList)
	{
		List<String> products = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "products");
		List<String> categories = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categories");
		List<String> warehouses = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouses");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

		Specification<Stock> finalSpec = null;
		if (products != null && products.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereChildFieldContains(Stock_.PRODUCT, Product_.PRODUCT_NAME, products));
		if (categories != null && categories.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereGrandChildFieldContains(Stock_.PRODUCT,
					Product_.CATEGORY, Category_.CATEGORY_NAME, categories));
		if (warehouses != null && warehouses.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereChildFieldContains(Stock_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, warehouses));

		if (globalSearch != null && globalSearch.size() > 0)
		{
			Specification<Stock> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereChildFieldContains(Stock_.PRODUCT, Product_.PRODUCT_NAME, globalSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereChildFieldContains(Stock_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, globalSearch));
			finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
		}

		return finalSpec;
	}
}
