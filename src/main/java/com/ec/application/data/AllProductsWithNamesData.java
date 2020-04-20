package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Product;

public class AllProductsWithNamesData 
{

	List<String> productAndCategoryNames;
	Page<Product> products;
	List<IdNameProjections> categoryNamesForDropdown;
	public List<String> getProductAndCategoryNames() {
		return productAndCategoryNames;
	}
	public void setProductAndCategoryNames(List<String> productAndCategoryNames) {
		this.productAndCategoryNames = productAndCategoryNames;
	}
	public Page<Product> getProducts() {
		return products;
	}
	public void setProducts(Page<Product> products) {
		this.products = products;
	}
	public List<IdNameProjections> getCategoryNamesForDropdown() {
		return categoryNamesForDropdown;
	}
	public void setCategoryNamesForDropdown(List<IdNameProjections> categoryNamesForDropdown) {
		this.categoryNamesForDropdown = categoryNamesForDropdown;
	}

	
	
	
}
