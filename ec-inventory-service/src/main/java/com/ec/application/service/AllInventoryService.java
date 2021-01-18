package com.ec.application.service;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.config.AsyncService;
import com.ec.application.data.AllInventoryReturnData;
import com.ec.application.data.DashboardInwardOutwardInventoryDAO;
import com.ec.application.data.DashboardMachineOnRentDAO;
import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.application.repository.MachineryOnRentRepo;
import com.ec.common.Filters.AllInventorySpecification;
import com.ec.common.Filters.FilterDataList;

@Service
public class AllInventoryService
{
	@Autowired
	AllInventoryRepo allInventoryRepo;

	@Autowired
	AsyncServiceInventory asyncServiceInventory;

	@Autowired
	PopulateDropdownService populateDropdownService;

	@Autowired
	MachineryOnRentRepo machineryOnRentRepo;

	@Autowired
	private AsyncService asyncService;

	public AllInventoryReturnData fetchAllInventory(FilterDataList filterDataList, Pageable pageable)
			throws ParseException
	{
		AllInventoryReturnData allInventoryReturnData = new AllInventoryReturnData();
		Specification<AllInventoryTransactions> spec = AllInventorySpecification.getSpecification(filterDataList);

		if (spec != null)
		{
			Page<AllInventoryTransactions> data = allInventoryRepo.findAll(spec, pageable);
			allInventoryReturnData.setTransactions(data);

		} else
			allInventoryReturnData.setTransactions(allInventoryRepo.findAll(pageable));
		allInventoryReturnData.setLdDropdown(populateDropdownService.fetchData("allinventory"));
		asyncService.run(() ->
		{
			for (int i = 0; i < 10; i++)
			{
				try
				{
					Thread.sleep(1000);
					asyncServiceInventory.sample();
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
		return allInventoryReturnData;
	}

	/*
	 * public List<DashboardInwardOutwardInventoryDAO> fetchOutwardForDashboard() {
	 * List<DashboardInwardOutwardInventoryDAO> outwardList = new
	 * ArrayList<DashboardInwardOutwardInventoryDAO>(); outwardList =
	 * allInventoryRepo.findForDashboard(); return outwardList; }
	 */

	public List<DashboardInwardOutwardInventoryDAO> fetchInventoryForDashboard(String type)
	{
		Pageable pageable = PageRequest.of(0, 5, Sort.by("created").descending());
		return allInventoryRepo.findForDashboard(type, pageable);
	}

	public List<DashboardMachineOnRentDAO> fetchMachineryOnRent()
	{
		Pageable pageable = PageRequest.of(0, 5, Sort.by("created").descending());
		return machineryOnRentRepo.findForDashboard(pageable);
	}

	public AllInventoryTransactions getRecordByEntryId(Long value)
	{
		List<AllInventoryTransactions> list = allInventoryRepo.findByEntryId(value);
		if (list.size() != 1)
			return null;
		else
			return list.get(0);
	}
}
