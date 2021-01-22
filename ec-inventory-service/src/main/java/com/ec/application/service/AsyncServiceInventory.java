package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.AllInventoryAndInwardOutwardListProjection;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.application.repository.InwardOutwardListRepo;

@Service
public class AsyncServiceInventory
{
	@Autowired
	AllInventoryRepo allRepo;

	@Autowired
	InwardOutwardListRepo iolRepo;

	public void sample() throws Exception
	{
		try
		{
			System.out.println("Before");
			List<AllInventoryAndInwardOutwardListProjection> list = allRepo.findClosingStockNotMatched();

			System.out.println("After - " + list.size());
			for (AllInventoryAndInwardOutwardListProjection iop : list)
			{
				System.out.println("##### - " + iop.getEntryid() + " AI CS -  " + iop.getAiClosingStock() + "IOL CS - "
						+ iop.getIolClosingStock());

				Optional<InwardOutwardList> iolOpt = iolRepo.findById(iop.getEntryid());
				if (iolOpt.isPresent())
				{
					InwardOutwardList iol = iolOpt.get();
					iol.setClosingStock(iop.getAiClosingStock());
					iolRepo.save(iol);

				}
			}
			System.out.println("Completed backfilling closing stock");
		} catch (Exception e)
		{
			throw new Exception("something went wrong!");

		}
	}

}
