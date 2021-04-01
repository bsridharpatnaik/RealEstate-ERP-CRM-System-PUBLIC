package com.ec.common.Service;

import com.ec.common.Data.TenantInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TenantService
{

	@Autowired
	UserService userService;

	@Value("${spring.profiles.active}")
	private String profile;

	public List<TenantInformation> fetchTenantList()
	{
		List<TenantInformation> tenants = new ArrayList<TenantInformation>();

		if(profile.contains("ec-"))
		{
			TenantInformation t1 = new TenantInformation("Evergreen City", "egcity");
		}
		else if(profile.contains("sc-")) {
			TenantInformation t1 = new TenantInformation("SUNCITY NX", "suncitynx");
			TenantInformation t2 = new TenantInformation("KALPAVRISH", "kalpavrish");
			TenantInformation t3 = new TenantInformation("RIDDHI SIDDHI", "riddhisiddhi");
			TenantInformation t4 = new TenantInformation("SMART CITY", "smartcity");
			TenantInformation t5 = new TenantInformation("BUSINESS PARK", "businesspark");
			tenants.add(t1);
			tenants.add(t2);
			tenants.add(t3);
			tenants.add(t4);
			tenants.add(t5);
		}
		return tenants;

		// suncitynx,kalpavrish,riddhisiddhi,smartcity,galaxyheights
	}

	public List<String> getValidTenantKeys()
	{
		List<String> validTenants = new ArrayList<String>();
		if(profile.contains("sc-")) {
			validTenants.add("suncitynx");
			validTenants.add("kalpavrish");
			validTenants.add("riddhisiddhi");
			validTenants.add("smartcity");
			validTenants.add("businesspark");
		}
		else if(profile.contains("ec-"))
			validTenants.add("egcity");
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