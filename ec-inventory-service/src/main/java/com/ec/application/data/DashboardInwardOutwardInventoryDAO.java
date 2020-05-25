package com.ec.application.data;

import java.util.Date;

public class DashboardInwardOutwardInventoryDAO 
{
	Date date;
	Double quantity;
	String productName;
	String warehouseName;
	String name;
	
	public DashboardInwardOutwardInventoryDAO(Date date, String productName,Double quantity, String warehouseName,
			String name) {
		super();
		this.date = date;
		this.quantity = quantity;
		this.productName = productName;
		this.warehouseName = warehouseName;
		this.name = name;
	}
	
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public Double getQuantity() {
		return quantity;
	}
	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getWarehouseName() {
		return warehouseName;
	}
	public void setWarehouseName(String warehouseName) {
		this.warehouseName = warehouseName;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
}
