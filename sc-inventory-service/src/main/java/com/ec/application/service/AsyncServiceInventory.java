package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	Logger log = LoggerFactory.getLogger(AsyncServiceInventory.class);

	public void backFillClosingStock(String dbName) throws Exception
	{
		try
		{
			ThreadLocalStorage.setTenantName(dbName);
			log.info("Starting backfilling closing stock");
			List<AllInventoryAndInwardOutwardListProjection> list = allRepo.findClosingStockNotMatched();

			log.info("No of records to be backfilled - " + list.size());

			for (AllInventoryAndInwardOutwardListProjection iop : list)
			{
				Optional<InwardOutwardList> iolOpt = iolRepo.findById(iop.getEntryid());
				if (iolOpt.isPresent())
				{
					InwardOutwardList iol = iolOpt.get();
					iol.setClosingStock(iop.getAiClosingStock());
					iolRepo.save(iol);

				}
			}
			ThreadLocalStorage.setTenantName(null);
			log.info("Completed backfilling closing stock");
		} catch (Exception e)
		{
			throw new Exception("something went wrong!");

		}
	}

}
