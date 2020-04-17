package com.ec.common.Filters;

import java.util.List;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;

public final class ProductSpecifications 
{
	static SpecificationsBuilder<Product> specbldr = new SpecificationsBuilder<Product>();
	
	public static Specification<Product> getSpecification(FilterDataList filterDataList)
	{
		List<String> productNames = specbldr.fetchValueFromFilterList(filterDataList,"product");
		List<String> categoryNames = specbldr.fetchValueFromFilterList(filterDataList,"category");
		Specification<Product> finalSpec = null;
		if(productNames != null && productNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(Product_.PRODUCT_NAME, productNames));
		
		if(categoryNames != null && categoryNames.size()>0)
		{
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereChildFieldContains(Product_.CATEGORY,Category_.CATEGORY_NAME, categoryNames));
		}	
		return finalSpec;	
	}
}
