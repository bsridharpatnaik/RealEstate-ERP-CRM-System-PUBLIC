package com.ec.common.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Data.TenantInformation;
import com.ec.common.Service.TenantService;

@RestController
@RequestMapping(value = "/tenants", produces =
{ "application/json", "text/json" })
public class TenantController
{
	@Autowired
	TenantService tenantService;

	@GetMapping
	public List<TenantInformation> fetchAllTenants()
	{

		return tenantService.fetchTenantList();
	}
}
