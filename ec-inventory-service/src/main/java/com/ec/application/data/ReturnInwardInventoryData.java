package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.InwardInventory;

public class ReturnInwardInventoryData 
{
	NameAndProjectionDataForDropDown iiDropdown;
	Page<InwardInventory> inwardInventory;
	List<ProductGroupedDAO> totals;
	
	public List<ProductGroupedDAO> getTotals() {
		return totals;
	}
	public void setTotals(List<ProductGroupedDAO> totals) {
		this.totals = totals;
	}
	public NameAndProjectionDataForDropDown getIiDropdown() {
		return iiDropdown;
	}
	public void setIiDropdown(NameAndProjectionDataForDropDown iiDropdown) {
		this.iiDropdown = iiDropdown;
	}
	public Page<InwardInventory> getInwardInventory() {
		return inwardInventory;
	}
	public void setInwardInventory(Page<InwardInventory> inwardInventory) {
		this.inwardInventory = inwardInventory;
	}
}
