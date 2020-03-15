package com.ec.application.data;

import java.util.List;

import com.ec.application.Projections.IdNameProjections;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class NameAndProjectionDataForDropDown 
{

	List<IdNameProjections> vendor;
	List<IdNameProjections> machinery;
	List<IdNameProjections> location;
	List<IdNameProjections> unloadingArea;
	List<IdNameProjections> category;
	List<IdNameProjections> product;
	List<IdNameProjections> contractor;
	
	public List<IdNameProjections> getContractor() {
		return contractor;
	}
	public void setContractor(List<IdNameProjections> contractor) {
		this.contractor = contractor;
	}
	public List<IdNameProjections> getUnloadingArea() {
		return unloadingArea;
	}
	public void setUnloadingArea(List<IdNameProjections> unloadingArea) {
		this.unloadingArea = unloadingArea;
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
	public List<IdNameProjections> getVendor() {
		return vendor;
	}
	public void setVendor(List<IdNameProjections> vendor) {
		this.vendor = vendor;
	}
	public List<IdNameProjections> getMachinery() {
		return machinery;
	}
	public void setMachinery(List<IdNameProjections> machinery) {
		this.machinery = machinery;
	}
	public List<IdNameProjections> getLocation() {
		return location;
	}
	public void setLocation(List<IdNameProjections> location) {
		this.location = location;
	}
	
}

