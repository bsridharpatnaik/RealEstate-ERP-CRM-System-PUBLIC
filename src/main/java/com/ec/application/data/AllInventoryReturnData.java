package com.ec.application.data;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.AllInventoryTransactions;

public class AllInventoryReturnData  implements Serializable
{

	List<AllInventoryTransactions> transactions;
	NameAndProjectionDataForDropDown ldDropdown;
	
	public List<AllInventoryTransactions> getTransactions() {
		return transactions;
	}
	public void setTransactions(List<AllInventoryTransactions> data) {
		this.transactions = data;
	}
	public NameAndProjectionDataForDropDown getLdDropdown() {
		return ldDropdown;
	}
	public void setLdDropdown(NameAndProjectionDataForDropDown ldDropdown) {
		this.ldDropdown = ldDropdown;
	}
}
