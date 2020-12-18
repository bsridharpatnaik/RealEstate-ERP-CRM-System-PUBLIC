package com.ec.common.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ec.common.Data.TenantInformation;

@Service
public class TenantService
{
	public List<TenantInformation> fetchTenantList()
	{
		List<TenantInformation> tenants = new ArrayList<TenantInformation>();

		TenantInformation t1 = new TenantInformation("MAHAVIR SUNCITY NX", "suncitynx");
		TenantInformation t2 = new TenantInformation("MAHAVIR KALPAVRISH", "kalpavrish");
		TenantInformation t3 = new TenantInformation("RIDDHI SIDDHI L AND M SERIES", "riddhisiddhi");
		TenantInformation t4 = new TenantInformation("MAHAVIR SMART CITY", "smartcity");
		TenantInformation t5 = new TenantInformation("GALAXY HEIGHTS", "galaxyheights");
		tenants.add(t1);
		tenants.add(t2);
		tenants.add(t3);
		tenants.add(t4);
		tenants.add(t5);
		return tenants;
	}
}