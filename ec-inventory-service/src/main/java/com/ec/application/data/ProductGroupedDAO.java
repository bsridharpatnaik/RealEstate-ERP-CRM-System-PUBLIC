package com.ec.application.data;

public class ProductGroupedDAO 
{
	String productname;
	Double quantity;
	public ProductGroupedDAO(String productname, Long quantity) {
		super();
		this.productname = productname;
		this.quantity = Double.valueOf(quantity);
	}
	public ProductGroupedDAO(String productname, Double quantity) {
		super();
		this.productname = productname;
		this.quantity = Double.valueOf(quantity);
	}
	
	public String getProductname() {
		return productname;
	}
	public void setProductname(String productname) {
		this.productname = productname;
	}
	public Double getQuantity() {
		return quantity;
	}
	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}
}
