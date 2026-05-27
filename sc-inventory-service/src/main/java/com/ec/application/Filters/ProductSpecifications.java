package com.ec.application.Filters;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.constants.BatchMode;
import com.ec.application.model.Category_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;

public final class ProductSpecifications
{
	static SpecificationsBuilder<Product> specbldr = new SpecificationsBuilder<Product>();

	public static Specification<Product> getSpecification(FilterDataList filterDataList)
	{
		List<String> names           = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");
		List<String> categoryNames   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
		List<String> isManagedList   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "isManagedInventory");
		List<String> batchModeList   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "batchModes");
		List<String> productCodes    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");

		Specification<Product> finalSpec = null;

		// Global text search (existing behaviour — OR across name + category)
		if (names != null && !names.isEmpty()) {
			Specification<Product> nameSpec = null;
			nameSpec = specbldr.specOrCondition(nameSpec, specbldr.whereDirectFieldContains(Product_.PRODUCT_NAME, names));
			nameSpec = specbldr.specOrCondition(nameSpec,
					specbldr.whereChildFieldContains(Product_.CATEGORY, Category_.CATEGORY_NAME, names));
			finalSpec = specbldr.specAndCondition(finalSpec, nameSpec);
		}

		// Category filter
		if (categoryNames != null && !categoryNames.isEmpty()) {
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereChildFieldEquals(Product_.CATEGORY, Category_.CATEGORY_NAME, categoryNames));
		}

		// Managed inventory filter (values: "true" / "false")
		if (isManagedList != null && !isManagedList.isEmpty()) {
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectBoleanFieldEquals(Product_.IS_MANAGED_INVENTORY, isManagedList));
		}

		// Batch mode filter (enum values: NONE, BATCH_ONLY, BATCH_WITH_EXPIRY)
		if (batchModeList != null && !batchModeList.isEmpty()) {
			List<BatchMode> modes = batchModeList.stream()
					.map(s -> { try { return BatchMode.valueOf(s); } catch (Exception e) { return null; } })
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			if (!modes.isEmpty()) {
				finalSpec = specbldr.specAndCondition(finalSpec,
						(root, query, cb) -> root.get(Product_.BATCH_MODE).in(modes));
			}
		}

		// Product code filter
		if (productCodes != null && !productCodes.isEmpty()) {
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldEquals(Product_.PRODUCT_CODE, productCodes));
		}

		return finalSpec;
	}
}
