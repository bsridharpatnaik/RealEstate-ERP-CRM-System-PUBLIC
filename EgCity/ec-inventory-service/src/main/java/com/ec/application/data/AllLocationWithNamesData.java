package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.UsageLocation;

public class AllLocationWithNamesData 
{
	List<String> names;
	Page<UsageLocation> locations;
	public List<String> getNames() {
		return names;
	}
	public void setNames(List<String> names) {
		this.names = names;
	}
	public Page<UsageLocation> getLocations() {
		return locations;
	}
	public void setLocations(Page<UsageLocation> locations) {
		this.locations = locations;
	}
}
