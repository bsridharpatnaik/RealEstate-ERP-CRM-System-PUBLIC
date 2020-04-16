package com.ec.common.Filters;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationResuableMethods;
import com.ec.application.model.Category_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;

public final class ProductSpecifications 
{
	
	public static Specification<Product> getSpecification(FilterDataList filterDataList)
	{
		String productName = fetchValueFromFilterList(filterDataList,"product");
		String categoryName = fetchValueFromFilterList(filterDataList,"category");
		Specification<Product> finalSpec = null;
		if(productName!=null)
		{
			Specification<Product> internalSpec = (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb)
            -> cb.like(root.get(Product_.PRODUCT_NAME), "%"+productName+"%");
			finalSpec = checkNull(finalSpec,internalSpec );
		}
		
		if(categoryName!=null)
		{
			Specification<Product> internalSpec = (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb)
	                -> cb.equal(root.get(Product_.CATEGORY).get(Category_.CATEGORY_NAME),categoryName);
	        finalSpec = checkNull(finalSpec,internalSpec );
		}	
		return finalSpec;
	}

	private static String fetchValueFromFilterList(FilterDataList filterDataList, String field) 
	{
		String returnValue = null;
		for(FilterAttributeData filterData : filterDataList.getFilterData())
		{
			if(filterData.getAttrName().equalsIgnoreCase(field))
				returnValue = filterData.getAttrValue().get(0);
		}
		return returnValue;
	}
	
	public static Specification<Product> checkNull(Specification<Product> finalSpec, Specification<Product> internalSpec) 
	{
		if(finalSpec == null) return internalSpec;
		else return finalSpec.and(internalSpec);
	}
}
