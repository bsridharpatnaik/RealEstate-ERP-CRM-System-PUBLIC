package com.ec.application.ReusableClasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ReusableMethods 
{
	
	public static List<String> removeNullsFromStringList(List<String> itemList)
	{
		while(itemList.remove(null)) {}
		return itemList;
	}	
	
	public static <T> List<T> convertSetToList(Set<T> set) 
    { 
        // create an empty list 
        List<T> list = new ArrayList<>(); 
  
        // push each element in the set into the list 
        for (T t : set) 
            list.add(t); 
  
        // return the list 
        return list; 
    }
}