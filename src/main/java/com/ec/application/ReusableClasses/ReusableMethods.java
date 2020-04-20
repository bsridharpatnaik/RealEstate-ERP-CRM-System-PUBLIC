package com.ec.application.ReusableClasses;

import java.util.List;

public final class ReusableMethods 
{

	public static List<String> removeNullsFromStringList(List<String> itemList)
	{
		while(itemList.remove(null)) {}
		return itemList;
	}
}
