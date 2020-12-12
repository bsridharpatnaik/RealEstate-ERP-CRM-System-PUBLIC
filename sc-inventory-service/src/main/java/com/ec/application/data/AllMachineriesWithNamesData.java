package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.Machinery;

public class AllMachineriesWithNamesData 
{
	List<String> machineryNames;
	Page<Machinery> machineries;
	public List<String> getMachineryNames() {
		return machineryNames;
	}
	public void setMachineryNames(List<String> machineryNames) {
		this.machineryNames = machineryNames;
	}
	public Page<Machinery> getMachineries() {
		return machineries;
	}
	public void setMachineries(Page<Machinery> machineries) {
		this.machineries = machineries;
	}
	
	
}
