package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.model.Stock;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
public class StockInformationExportDAO 
{

	Long productId;
	String inventory;
	String category;
	String totalStock;
	String warehouse;
	@JsonSerialize(using=DoubleTwoDigitDecimalSerializer.class)
	Double warehouseStock;
	String measurementUnit;
	Double reorderQuantity;
	String stockStatus;
	public StockInformationExportDAO(SingleStockInfo ssi, Stock stock) 
	{
		this.productId=ssi.getProductId();
		this.inventory=ssi.getProductName();
		this.category=ssi.getCategoryName();
		this.totalStock=ssi.getTotalQuantityInHand();
		this.warehouse=stock.getWarehouse().getWarehouseName();
		this.warehouseStock=stock.getQuantityInHand();
		this.measurementUnit=stock.getProduct().getMeasurementUnit();
		this.reorderQuantity = stock.getProduct().getReorderQuantity();
		this.stockStatus = stock.getQuantityInHand()>stock.getProduct().getReorderQuantity()?"High":"Low";
	}

	public Double getReorderQuantity() {
		return reorderQuantity;
	}

	public void setReorderQuantity(Double reorderQuantity) {
		this.reorderQuantity = reorderQuantity;
	}

	public String getStockStatus() {
		return stockStatus;
	}

	public void setStockStatus(String stockStatus) {
		this.stockStatus = stockStatus;
	}

	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public String getInventory() {
		return inventory;
	}
	public void setInventory(String inventory) {
		this.inventory = inventory;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getTotalStock() {
		return totalStock;
	}
	public void setTotalStock(String totalStock) {
		this.totalStock = totalStock;
	}
	public String getWarehouse() {
		return warehouse;
	}
	public void setWarehouse(String warehouse) {
		this.warehouse = warehouse;
	}
	public Double getWarehouseStock() {
		return warehouseStock;
	}
	public void setWarehouseStock(Double warehouseStock) {
		this.warehouseStock = warehouseStock;
	}
	public String getMeasurementUnit() {
		return measurementUnit;
	}
	public void setMeasurementUnit(String measurementUnit) {
		this.measurementUnit = measurementUnit;
	}
}
