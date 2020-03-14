package com.ec.application.model;

import java.io.Serializable;

import com.ec.application.model.BasicEntities.Product;
import com.ec.application.model.Stock.Warehouse;

public class StockID implements Serializable 
{
	private Product product;
	private Warehouse warehouse ;
	public StockID(Product product, Warehouse warehouse) {
		super();
		this.product = product;
		this.warehouse = warehouse;
	}
	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	}
	public Warehouse getWarehouse() {
		return warehouse;
	}
	public void setWarehouse(Warehouse warehouse) {
		this.warehouse = warehouse;
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StockID that = (StockID) o;

        if (!product.equals(that.product)) return false;
        return warehouse.equals(that.warehouse);
    }

    @Override
    public int hashCode() {
        int result = product.hashCode();
        result = 31 * result + warehouse.hashCode();
        return result;
    }
}
