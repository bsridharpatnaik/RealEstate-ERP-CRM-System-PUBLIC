package com.ec.common.Filters;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.model.Category_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;

import lombok.NonNull;

public final class ProductSpecifications 
{

	public static Specification<Product> whereProductNameContains(@NonNull String productName) 
	{
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(Product_.PRODUCT_NAME), "%"+productName+"%");
    }
	public static Specification<Product> whereCategoryNameEquals(@NonNull String categoryName) 
	{
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.equal(root.get(Product_.CATEGORY).get(Category_.CATEGORY_NAME),categoryName);
    }
	
}
