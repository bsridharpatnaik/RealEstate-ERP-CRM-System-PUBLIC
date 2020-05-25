package com.ec.application.data;

import org.springframework.data.domain.Page;

import com.ec.application.model.LostDamagedInventory;


public class LostDamagedReturnData 
{
	Page<LostDamagedInventory> lostDamagedInventories;
	NameAndProjectionDataForDropDown ldDropdown;
	public Page<LostDamagedInventory> getLostDamagedInventories() {
		return lostDamagedInventories;
	}
	public void setLostDamagedInventories(Page<LostDamagedInventory> lostDamagedInventories) {
		this.lostDamagedInventories = lostDamagedInventories;
	}
	public NameAndProjectionDataForDropDown getLdDropdown() {
		return ldDropdown;
	}
	public void setLdDropdown(NameAndProjectionDataForDropDown ldDropdown) {
		this.ldDropdown = ldDropdown;
	}
}
