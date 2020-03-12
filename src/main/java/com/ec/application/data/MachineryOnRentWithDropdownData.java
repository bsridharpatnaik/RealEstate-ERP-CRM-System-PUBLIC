package com.ec.application.data;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.MachineryOnRent;

public class MachineryOnRentWithDropdownData 
{

	List<NameAndProjectionDataForDropDown> morDropdown;
	Page<MachineryOnRent> machineryOnRent;
	public List<NameAndProjectionDataForDropDown> getMorDropdown() {
		return morDropdown;
	}
	public void setMorDropdown(List<NameAndProjectionDataForDropDown> morDropdown) {
		this.morDropdown = morDropdown;
	}
	public Page<MachineryOnRent> getMachineryOnRent() {
		return machineryOnRent;
	}
	public void setMachineryOnRent(Page<MachineryOnRent> machineryOnRent) {
		this.machineryOnRent = machineryOnRent;
	}
	
	
}
