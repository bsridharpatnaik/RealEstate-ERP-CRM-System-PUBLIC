package com.ec.application.data;

import java.util.List;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class NameAndProjectionDataForDropDown 
{

	List<IdNameProjections> machinery;
	List<IdNameProjections> usagelocation;
	List<IdNameProjections> warehouse;
	List<IdNameProjections> category;
	List<IdNameProjections> product;
	List<IdNameProjections> contractor;
	List<IdNameProjections> supplier;
	
	public List<IdNameProjections> getMachinery() {
		return machinery;
	}
	public void setMachinery(List<IdNameProjections> machinery) {
		this.machinery = machinery;
	}
	public List<IdNameProjections> getUsagelocation() {
		return usagelocation;
	}
	public void setUsagelocation(List<IdNameProjections> usagelocation) {
		this.usagelocation = usagelocation;
	}
	public List<IdNameProjections> getWarehouse() {
		return warehouse;
	}
	public void setWarehouse(List<IdNameProjections> warehouse) {
		this.warehouse = warehouse;
	}
	public List<IdNameProjections> getCategory() {
		return category;
	}
	public void setCategory(List<IdNameProjections> category) {
		this.category = category;
	}
	public List<IdNameProjections> getProduct() {
		return product;
	}
	public void setProduct(List<IdNameProjections> product) {
		this.product = product;
	}
	public List<IdNameProjections> getContractor() {
		return contractor;
	}
	public void setContractor(List<IdNameProjections> contractor) {
		this.contractor = contractor;
	}
	public List<IdNameProjections> getSupplier() {
		return supplier;
	}
	public void setSupplier(List<IdNameProjections> supplier) {
		this.supplier = supplier;
	}
	
	
}

