package com.ec.crm.Model;

import java.io.Serializable;


import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PropertyTypeEnum implements Serializable
{ 
	HOUSE_2_BHK,
	HOUSE_3_BHK,
	PLOT;
	
	public PropertyTypeEnum setFromString(String name)
	{
		return PropertyTypeEnum.valueOf(name);
	}
}
