package com.ec.common.Data;

public class TenantInformation
{

	String tenantName;
	String tenantCode;

	public String getTenantName()
	{
		return tenantName;
	}

	public void setTenantName(String tenantName)
	{
		this.tenantName = tenantName;
	}

	public String getTenantCode()
	{
		return tenantCode;
	}

	public void setTenantCode(String tenantCode)
	{
		this.tenantCode = tenantCode;
	}

	public TenantInformation(String tenantName, String tenantCode)
	{
		super();
		this.tenantName = tenantName;
		this.tenantCode = tenantCode;
	}

}
