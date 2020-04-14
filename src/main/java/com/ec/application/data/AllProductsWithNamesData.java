package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.Product;

public class AllProductsWithNamesData 
{

	List<String> productNames;
	Page<Product> products;
	public List<String> getProductNames() {
		return productNames;
	}
	public void setProductNames(List<String> productNames) {
		this.productNames = productNames;
	}
	public Page<Product> getProducts() {
		return products;
	}
	public void setProducts(Page<Product> products) {
		this.products = products;
	}
	
	
}
