package com.ec.application.data;

import org.springframework.data.domain.Page;

import com.ec.application.model.OutwardInventory;

public class OutwardInventoryWithDropdownValues 
{
	NameAndProjectionDataForDropDown morDropdown;
	Page<OutwardInventory> outwardInventory;
	public NameAndProjectionDataForDropDown getMorDropdown() {
		return morDropdown;
	}
	public void setMorDropdown(NameAndProjectionDataForDropDown morDropdown) {
		this.morDropdown = morDropdown;
	}
	public Page<OutwardInventory> getOutwardInventory() {
		return outwardInventory;
	}
	public void setOutwardInventory(Page<OutwardInventory> outwardInventory) {
		this.outwardInventory = outwardInventory;
	}
	
}
