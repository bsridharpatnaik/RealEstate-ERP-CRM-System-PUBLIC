package com.ec.application.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.comparators.MonthlyReportComparator;
import com.ec.application.data.*;
import com.ec.application.model.InventoryReport;
import com.ec.application.model.InwardInventory;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InventoryReportRepo;
import com.ec.common.Filters.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.application.repository.MachineryOnRentRepo;

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

    public List<InventoryReportByDate> getInventoryReport(FilterDataList filterDataList) throws Exception {
        FilterAttrValueListForAllInventory fiList = new FilterAttrValueListForAllInventory();
        getValueFromPayload(fiList, filterDataList);

        if (fiList.getStartDate() == null || fiList.getEndDate() == null)
            throw new Exception("Start Date or End Date cannot be Empty");

        List<InventoryReportByDate> allData = allInventoryRepo.getFilteredTransactionReport(fiList.getStartDate(), fiList.getEndDate());
        List<InventoryReportByDate> returnData = filterData(allData, fiList);
        Collections.sort(returnData,new MonthlyReportComparator());
        return returnData;
    }

    private List<InventoryReportByDate> filterData(List<InventoryReportByDate> allData, FilterAttrValueListForAllInventory fiList) {

        if (fiList.getProductNames() != null && fiList.getProductNames().size() > 0) {
            allData = allData.stream().filter(e -> fiList.getProductNames()
                    .contains(e.getProduct_name())).collect(Collectors.toList());
        }
        if (fiList.getCategoryNames() != null && fiList.getCategoryNames().size() > 0) {
            allData = allData.stream().filter(e -> fiList.getCategoryNames()
                    .contains(e.getCategory_name())).collect(Collectors.toList());
        }
        if (fiList.getWarehouseNames() != null && fiList.getWarehouseNames().size()>0) {
            allData = allData.stream().filter(e -> fiList.getWarehouseNames()
                    .contains(e.getWarehousename())).collect(Collectors.toList());
        }
        return allData;
    }

    private FilterAttrValueListForAllInventory getValueFromPayload(FilterAttrValueListForAllInventory returnData,
                                                                   FilterDataList filterDataList) throws Exception {
        for (FilterAttributeData fData : filterDataList.getFilterData()) {
            if (fData.getAttrName().equalsIgnoreCase("startDate")) {
                try {
                    returnData.setStartDate(new SimpleDateFormat("dd-MM-yyyy").parse(fData.getAttrValue().get(0)));
                } catch (Exception e) {
                    throw new Exception("Unable to parse value for key startDate");
                }
            }
            if (fData.getAttrName().equalsIgnoreCase("EndDate")) {
                try {
                    returnData.setEndDate(new SimpleDateFormat("dd-MM-yyyy").parse(fData.getAttrValue().get(0)));
                } catch (Exception e) {
                    throw new Exception("Unable to parse value for key EndDate");
                }
            }
            if (fData.getAttrName().equalsIgnoreCase("products")) {
                try {
                    returnData.setProductNames(new HashSet<String>(fData.getAttrValue()));
                } catch (Exception e) {
                    throw new Exception("Unable to parse value for key Products");
                }
            }
            if (fData.getAttrName().equalsIgnoreCase("categories")) {
                try {
                    returnData.setCategoryNames(new HashSet<String>(fData.getAttrValue()));
                } catch (Exception e) {
                    throw new Exception("Unable to parse value for key categories");
                }
            }
            if (fData.getAttrName().equalsIgnoreCase("warehouses")) {
                try {
                    returnData.setWarehouseNames(new HashSet<String>(fData.getAttrValue()));
                } catch (Exception e) {
                    throw new Exception("Unable to parse value for key warehouses");
                }
            }
        }
        return returnData;
    }
}
