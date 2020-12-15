package com.ec.application.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantsController
{
	@Value("${schemas.list}")
	private String schemasList;

	@GetMapping("/tenants")
	public List<String> getTenants()
	{
		List<String> list = new ArrayList<>();
		if (StringUtils.isBlank(schemasList))
		{
			return java.util.Collections.emptyList();
		} else
		{
			String[] tenants = schemasList.split(",");
			for (String str : tenants)
			{
				list.add(str);
			}
			return list;
		}
	}
}
