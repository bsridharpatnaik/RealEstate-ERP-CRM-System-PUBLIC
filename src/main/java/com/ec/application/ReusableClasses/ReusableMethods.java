package com.ec.application.ReusableClasses;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.ec.application.data.ProductWithQuantity;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.Product;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.ProductRepo;

public final class ReusableMethods 
{
	
	public static List<String> removeNullsFromStringList(List<String> itemList)
	{
		while(itemList.remove(null)) {}
		return itemList;
	}
}
