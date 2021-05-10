package com.ec.common.Filters;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum UserFilterAttributeEnum implements Serializable
{
	USERNAME;	
}
