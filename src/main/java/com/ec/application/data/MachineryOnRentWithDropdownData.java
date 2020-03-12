package com.ec.application.data;

import java.util.ArrayList;

import org.springframework.data.domain.Page;

import com.ec.application.model.MachineryOnRent;

public class MachineryOnRentWithDropdownData 
{

	ArrayList<MORDropdownData> morDropdownData;
	Page<MachineryOnRent> machineryOnRent;
	public ArrayList<MORDropdownData> getMorDropdownData() {
		return morDropdownData;
	}
	public void setMorDropdownData(ArrayList<MORDropdownData> morDropdownData) {
		this.morDropdownData = morDropdownData;
	}
	public Page<MachineryOnRent> getMachineryOnRent() {
		return machineryOnRent;
	}
	public void setMachineryOnRent(Page<MachineryOnRent> machineryOnRent) {
		this.machineryOnRent = machineryOnRent;
	}
}
