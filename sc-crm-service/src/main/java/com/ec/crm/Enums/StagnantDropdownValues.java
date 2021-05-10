package com.ec.crm.Enums;

import java.util.HashMap;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class StagnantDropdownValues
{
	public static HashMap<String, String> getKeyValue()
	{
		HashMap<String, String> keyValues = new HashMap<String, String>();
		keyValues.put("NoColour", "No Colour (0 - 10 Days)");
		keyValues.put("Green", "Green (10 - 20 Days)");
		keyValues.put("Orange", "Orange (20 - 30 Days)");
		keyValues.put("Red", "Red (> 30 Days)");
		return keyValues;
	}
}
