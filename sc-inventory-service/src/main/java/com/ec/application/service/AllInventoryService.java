package com.ec.application.service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import com.ec.application.Filters.AllInventorySpecification;
import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.comparators.MonthlyReportComparator;
import com.ec.application.data.*;
import com.ec.application.repository.InventoryReportRepo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    Logger log = LoggerFactory.getLogger(AllInventoryService.class);

    public void updateClosingStock() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        jdbcTemplate.execute("CALL update_closing_stock()");
        log.info("Update closing stock completed");
    }

    public AllInventoryReturnData fetchAllInventory(FilterDataList filterDataList, Pageable pageable)
            throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        AllInventoryReturnData allInventoryReturnData = new AllInventoryReturnData();
        Specification<AllInventoryTransactions> spec = AllInventorySpecification.getSpecification(filterDataList);

        if (spec != null) {
            Page<AllInventoryTransactions> data = allInventoryRepo.findAll(spec, pageable);
            if(data.getContent().size() > 5000)
                throw new Exception("Too many records. Please apply some filters and try again.");
            allInventoryReturnData.setTransactions(data);

        } else{
            Page<AllInventoryTransactions> data2 = allInventoryRepo.findAll(pageable);
            if(data2.getContent().size() > 5000)
                throw new Exception("Too many records. Please apply some filters and try again.");
            allInventoryReturnData.setTransactions(data2);
        }
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

    /**
     * Exports inventory transactions matching the given filters as an Excel (.xlsx) file.
     * Only human-readable columns are exported — primary keys and internal IDs are excluded.
     */
    public void exportToExcel(FilterDataList filterDataList, HttpServletResponse response) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        Specification<AllInventoryTransactions> spec = AllInventorySpecification.getSpecification(filterDataList);
        Sort sort = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("sortOrder"), Sort.Order.desc("entryid"));

        List<AllInventoryTransactions> rows;
        if (spec != null) {
            rows = allInventoryRepo.findAll(spec, sort);
        } else {
            rows = allInventoryRepo.findAll(sort);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"inventory-transactions.xlsx\"");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            Sheet sheet = wb.createSheet("Inventory");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row — clean names only, no PKs
            String[] headers = {
                "Date", "Type", "Product Name", "Category",
                "Warehouse", "Contact Name", "Unit", "Quantity", "Closing Stock"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            int rowIdx = 1;
            for (AllInventoryTransactions t : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getDate() != null ? sdf.format(t.getDate()) : "");
                row.createCell(1).setCellValue(t.getType()          != null ? t.getType()          : "");
                row.createCell(2).setCellValue(t.getProductName()   != null ? t.getProductName()   : "");
                row.createCell(3).setCellValue(t.getCategoryName()  != null ? t.getCategoryName()  : "");
                row.createCell(4).setCellValue(t.getWarehouseName() != null ? t.getWarehouseName() : "");
                row.createCell(5).setCellValue(t.getName()          != null ? t.getName()          : "");
                row.createCell(6).setCellValue(t.getMeasurementUnit() != null ? t.getMeasurementUnit() : "");
                if (t.getQuantity()     != null) row.createCell(7).setCellValue(t.getQuantity());
                else                             row.createCell(7).setCellValue("");
                if (t.getClosingStock() != null) row.createCell(8).setCellValue(t.getClosingStock());
                else                             row.createCell(8).setCellValue("");
            }

            wb.write(response.getOutputStream());
            wb.dispose();
        }
    }

    public void updateAllInventoryTable() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        jdbcTemplate.execute("CALL update_all_inventory()");
        log.info("Update all_inventory completed");
    }
}
