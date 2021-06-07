package com.ec.common.Data;

public class TenantInformation
{

	String tenantName;
	String tenantCode;
	Boolean isCrm;
	Boolean isInventory;

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

	public Boolean getCrm() {
		return isCrm;
	}

	public void setCrm(Boolean crm) {
		isCrm = crm;
	}

	public Boolean getInventory() {
		return isInventory;
	}

	public void setInventory(Boolean inventory) {
		isInventory = inventory;
	}

	public void setTenantCode(String tenantCode)
	{
		this.tenantCode = tenantCode;
	}

	public TenantInformation(String tenantName, String tenantCode, Boolean isInventory, Boolean isCrm)
	{
		super();
		this.tenantName = tenantName;
		this.tenantCode = tenantCode;
		this.isCrm=isCrm;
		this.isInventory=isInventory;
	}

}
