package com.ec.application.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum APICallTypeForAuthorization
{
	Create, Update, Delete, Reject;

	public APICallTypeForAuthorization setFromString(String name)
	{
		return APICallTypeForAuthorization.valueOf(name);
	}

	public static List<String> getValidModes()
	{
		List<String> validModes = new ArrayList<String>();
		EnumSet.allOf(APICallTypeForAuthorization.class).forEach(type -> validModes.add(type.toString()));
		return validModes;
	}
}
