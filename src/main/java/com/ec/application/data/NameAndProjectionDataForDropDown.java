package com.ec.application.data;

import java.util.List;

import com.ec.application.Projections.IdNameProjections;

public class NameAndProjectionDataForDropDown 
{

	List<IdNameProjections> vendor;
	List<IdNameProjections> machinery;
	List<IdNameProjections> location;
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

