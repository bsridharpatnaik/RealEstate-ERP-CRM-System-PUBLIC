package com.ec.common.Data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TenantInformation
{
	String tenantName;
	String tenantCode;

	public TenantInformation(String tenantName, String tenantCode)
	{
		super();
		this.tenantName = tenantName;
		this.tenantCode = tenantCode;
	}

}
