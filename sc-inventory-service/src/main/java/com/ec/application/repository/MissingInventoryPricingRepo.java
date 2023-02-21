package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.DashboardInwardOutwardInventoryDAO;
import com.ec.application.data.InventoryReportByDate;
import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.model.MissingInventoryPricing;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public interface MissingInventoryPricingRepo extends BaseRepository<MissingInventoryPricing, String> {
}