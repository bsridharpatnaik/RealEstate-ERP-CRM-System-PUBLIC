package com.ec.application.data;

import org.springframework.data.domain.Page;

import com.ec.application.model.OutwardInventory;

public class ReturnOutwardInventoryData 
{
	Page<OutwardInventory> outwardInventory;
	NameAndProjectionDataForDropDown iiDropdown;
	public Page<OutwardInventory> getOutwardInventory() {
		return outwardInventory;
	}
	public void setOutwardInventory(Page<OutwardInventory> outwardInventory) {
		this.outwardInventory = outwardInventory;
	}
	public NameAndProjectionDataForDropDown getIiDropdown() {
		return iiDropdown;
	}
	public void setIiDropdown(NameAndProjectionDataForDropDown iiDropdown) {
		this.iiDropdown = iiDropdown;
	}
}
