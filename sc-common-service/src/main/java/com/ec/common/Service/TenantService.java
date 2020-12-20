package com.ec.common.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.common.Data.TenantInformation;

@Service
public class TenantService
{

	@Autowired
	UserService userService;

	public List<TenantInformation> fetchTenantList()
	{
		List<TenantInformation> tenants = new ArrayList<TenantInformation>();

		TenantInformation t1 = new TenantInformation("SUNCITY NX", "suncitynx");
		TenantInformation t2 = new TenantInformation("KALPAVRISH", "kalpavrish");
		TenantInformation t3 = new TenantInformation("RIDDHI SIDDHI", "riddhisiddhi");
		TenantInformation t4 = new TenantInformation("SMART CITY", "smartcity");
		TenantInformation t5 = new TenantInformation("GALAXY HEIGHTS", "galaxyheights");
		tenants.add(t1);
		tenants.add(t2);
		tenants.add(t3);
		tenants.add(t4);
		tenants.add(t5);
		return tenants;

		// suncitynx,kalpavrish,riddhisiddhi,smartcity,galaxyheights
	}

	public List<String> getValidTenantKeys()
	{
		List<String> validTenants = new ArrayList<String>();
		validTenants.add("suncitynx");
		validTenants.add("kalpavrish");
		validTenants.add("riddhisiddhi");
		validTenants.add("smartcity");
		validTenants.add("galaxyheights");
		return validTenants;
	}

	public List<TenantInformation> getAllowedTenants() throws Exception
	{
		List<String> allowedTenantKeys = userService.findTenantsForUser();
		List<TenantInformation> allTenants = fetchTenantList();
		List<TenantInformation> allowedTenants = new ArrayList<TenantInformation>();
		for (TenantInformation ti : allTenants)
		{
			if (allowedTenantKeys.contains(ti.getTenantCode()))
			{
				allowedTenants.add(ti);
			}
		}
		return allowedTenants;
	}
}