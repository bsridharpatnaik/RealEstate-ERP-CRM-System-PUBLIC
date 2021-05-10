package com.ec.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.application.repository.InwardOutwardListRepo;

@Service
public class InwardOutwardListService
{

	@Autowired
	InwardOutwardListRepo iolRepo;

	@Autowired
	AllInventoryRepo aiRepo;

	public void correctClosingStock(Long startEntryId, Long endEntryId)
	{
		List<InwardOutwardList> recordsBetweenIds = iolRepo.getRecordsBetweenEntryIds(startEntryId, endEntryId);
		long i = 0;
		long j = 0;
		for (InwardOutwardList iol : recordsBetweenIds)
		{

			List<AllInventoryTransactions> trans = aiRepo.findByEntryId(iol.getEntryid());
			if (trans.size() == 1)
			{
				System.out.println("Processing - " + iol.getEntryid());
				if (!iol.getClosingStock().equals(trans.get(0).getClosingStock()))
				{
					System.out.println("Old Closing Stock - " + iol.getClosingStock() + ". New closing stock - "
							+ trans.get(0).getClosingStock());
					iol.setClosingStock(trans.get(0).getClosingStock());

					i++;
				} else
				{
					j++;
				}

			} else
			{
				// ignore else. Should never happen
			}
			iolRepo.save(iol);
		}

		System.out.println("Changed for - " + i);
		System.out.println("Unchanged for " + j);
	}
}
