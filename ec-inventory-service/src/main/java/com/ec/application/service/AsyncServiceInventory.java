package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.repository.AllInventoryRepo;

@Service
public class AsyncServiceInventory
{
	@Autowired
	AllInventoryRepo allRepo;

	public void sample()
	{
		System.out.println("#################" + allRepo.count());
	}

}
