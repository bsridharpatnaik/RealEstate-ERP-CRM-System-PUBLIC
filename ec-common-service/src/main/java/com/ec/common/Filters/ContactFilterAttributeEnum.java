package com.ec.common.Filters;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ContactFilterAttributeEnum implements Serializable
{
	NAME,
	CONTACTTYPE,
	MOBILENUMBER,
	ADDRESS,
	NAMEORMOBILE;	
}
