package com.ec.application.service;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import com.ec.application.model.InventoryReport;
import com.ec.application.model.InwardInventory;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InventoryReportRepo;
import com.ec.common.Filters.InventoryReportSpecification;
import com.ec.common.Filters.InwardInventorySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.data.AllInventoryReturnData;
import com.ec.application.data.DashboardInwardOutwardInventoryDAO;
import com.ec.application.data.DashboardMachineOnRentDAO;
import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.application.repository.MachineryOnRentRepo;
import com.ec.common.Filters.AllInventorySpecification;
import com.ec.common.Filters.FilterDataList;

@Service
@Transactional
public class AllInventoryService {
    @Autowired
    AllInventoryRepo allInventoryRepo;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @Autowired
    MachineryOnRentRepo machineryOnRentRepo;

    @Autowired
    InventoryReportRepo inventoryReportRepo;

    Logger log = LoggerFactory.getLogger(AllInventoryService.class);

    public AllInventoryReturnData fetchAllInventory(FilterDataList filterDataList, Pageable pageable)
            throws ParseException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        AllInventoryReturnData allInventoryReturnData = new AllInventoryReturnData();
        Specification<AllInventoryTransactions> spec = AllInventorySpecification.getSpecification(filterDataList);

        if (spec != null) {
            Page<AllInventoryTransactions> data = allInventoryRepo.findAll(spec, pageable);
            allInventoryReturnData.setTransactions(data);

        } else
            allInventoryReturnData.setTransactions(allInventoryRepo.findAll(pageable));
        allInventoryReturnData.setLdDropdown(populateDropdownService.fetchData("allinventory"));
        return allInventoryReturnData;
    }

    /*
     * public List<DashboardInwardOutwardInventoryDAO> fetchOutwardForDashboard() {
     * List<DashboardInwardOutwardInventoryDAO> outwardList = new
     * ArrayList<DashboardInwardOutwardInventoryDAO>(); outwardList =
     * allInventoryRepo.findForDashboard(); return outwardList; }
     */

    public List<DashboardInwardOutwardInventoryDAO> fetchInventoryForDashboard(String type) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Pageable pageable = PageRequest.of(0, 5, Sort.by("creationDate").descending());
        return allInventoryRepo.findForDashboard(type, pageable);
    }

    public List<DashboardMachineOnRentDAO> fetchMachineryOnRent() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Pageable pageable = PageRequest.of(0, 5, Sort.by("creationDate").descending());
        return machineryOnRentRepo.findForDashboard(pageable);
    }

    public AllInventoryTransactions getRecordByEntryId(Long value) {
        List<AllInventoryTransactions> list = allInventoryRepo.findByEntryId(value);
        if (list.size() != 1)
            return null;
        else
            return list.get(0);
    }

    public void refreshAllInventoryData() {

    }

    public List<InventoryReport> getInventoryReport(FilterDataList filterDataList) throws Exception {
        Specification<InventoryReport> spec = InventoryReportSpecification.getSpecification(filterDataList);

        // Feed listing
        if (spec != null) {
            if (inventoryReportRepo.count(spec) > 4000)
                throw new Exception("Too many rows to download. Apply some filters and try again.");
            return inventoryReportRepo.findAll(spec);
        }
		else {
            if (inventoryReportRepo.count() > 4000)
                throw new Exception("Too many rows to download. Apply some filters and try again.");
            return inventoryReportRepo.findAll();
        }
    }
}
