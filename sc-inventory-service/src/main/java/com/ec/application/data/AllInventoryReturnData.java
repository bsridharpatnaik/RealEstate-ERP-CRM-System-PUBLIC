package com.ec.application.data;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.application.model.AllInventoryTransactions;

public class AllInventoryReturnData  implements Serializable
{

	Page<AllInventoryTransactions> transactions;
	NameAndProjectionDataForDropDown ldDropdown;
	
	public Page<AllInventoryTransactions> getTransactions() {
		return transactions;
	}
	public void setTransactions(Page<AllInventoryTransactions> data) {
		this.transactions = data;
	}
	public NameAndProjectionDataForDropDown getLdDropdown() {
		return ldDropdown;
	}
	public void setLdDropdown(NameAndProjectionDataForDropDown ldDropdown) {
		this.ldDropdown = ldDropdown;
	}
}
