package com.ec.common.Data;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CustomerTypeEnum implements Serializable{
	
	CUSTOMER,
	SUPPLIER,
	CONTRACTOR;
	
	public CustomerTypeEnum setFromString(String name)
	{
		return CustomerTypeEnum.valueOf(name);
	}
}