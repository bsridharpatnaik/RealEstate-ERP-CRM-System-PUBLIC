package com.ec.application.data;

import org.springframework.data.domain.Page;

import com.ec.application.model.InwardInventory;
import com.ec.application.model.MachineryOnRent;

public class InwardInventoryWithDropdownValues 
{
	NameAndProjectionDataForDropDown morDropdown;
	Page<InwardInventory> inwardInventory;
	public NameAndProjectionDataForDropDown getMorDropdown() {
		return morDropdown;
	}
	public void setMorDropdown(NameAndProjectionDataForDropDown morDropdown) {
		this.morDropdown = morDropdown;
	}
	public Page<InwardInventory> getInwardInventory() {
		return inwardInventory;
	}
	public void setInwardInventory(Page<InwardInventory> inwardInventory) {
		this.inwardInventory = inwardInventory;
	}
}
