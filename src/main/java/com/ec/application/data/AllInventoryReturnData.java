package com.ec.application.data;

import org.springframework.data.domain.Page;

import com.ec.application.model.AllInventoryTransactions;

public class AllInventoryReturnData 
{

	Page<AllInventoryTransactions> transactions;
	NameAndProjectionDataForDropDown ldDropdown;
	public Page<AllInventoryTransactions> getTransactions() {
		return transactions;
	}
	public void setTransactions(Page<AllInventoryTransactions> transactions) {
		this.transactions = transactions;
	}
	public NameAndProjectionDataForDropDown getLdDropdown() {
		return ldDropdown;
	}
	public void setLdDropdown(NameAndProjectionDataForDropDown ldDropdown) {
		this.ldDropdown = ldDropdown;
	}
}
