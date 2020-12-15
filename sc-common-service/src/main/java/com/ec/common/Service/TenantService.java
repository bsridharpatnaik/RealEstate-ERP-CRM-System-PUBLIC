package com.ec.common.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ec.common.Data.TenantInformation;

@Service
public class TenantService
{
	public List<TenantInformation> getTenantInformation()
	{
		List<TenantInformation> tenantInformation = new ArrayList<TenantInformation>();
		TenantInformation ti1 = new TenantInformation("MAHAVIR SUNCITY NX", "suncitynx");
		TenantInformation ti2 = new TenantInformation("MAHAVIR KALPAVRISH", "kalpavrish");
		TenantInformation ti3 = new TenantInformation("RIDDHI SIDDHI L AND M SERIES", "riddhisiddhi");
		TenantInformation ti4 = new TenantInformation("MAHAVIR SMART CITY", "smartcity");
		TenantInformation ti5 = new TenantInformation("GALAXY HEIGHTS", "galaxyheights");
		tenantInformation.add(ti1);
		tenantInformation.add(ti2);
		tenantInformation.add(ti3);
		tenantInformation.add(ti4);
		tenantInformation.add(ti5);
		return tenantInformation;
	}

}
