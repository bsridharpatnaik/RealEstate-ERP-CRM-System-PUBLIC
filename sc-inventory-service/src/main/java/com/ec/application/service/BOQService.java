package com.ec.application.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.ec.application.ReusableClasses.IdNameProjections;

import javax.transaction.Transactional;

import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.BOQUploadConstant;
import com.ec.application.Filters.BOQStatusFilterDataList;
import com.ec.application.Filters.FilterAttributeData;

@Service
@Transactional
public class BOQService {

    @Autowired
    private UsageAreaRepo locationRepository;

    @Autowired
    private ProductRepo productRepository;

    @Autowired
    private BuildingTypeRepo buildingTypeRepository;

    @Autowired
    private BOQUploadRepository bOQUploadRepository;

    @Autowired
    private LocationRepo usageLocationRepository;

    @Autowired
    private InwardOutwardListRepo inwardOutwardListRepo;

    @Autowired
    private CategoryRepo categoryRepository;

    @Autowired
    private BOQHistoryService boqHistoryService;

    @Autowired
    private UserDetailsService userDetailsService;

    Logger log = LoggerFactory.getLogger(BOQService.class);

    @Value("${boq.enforcement.block:true}")
    private boolean boqEnforcementBlock;

    public List<BOQUploadValidationResponse> boqUpload(BOQDto boqDto) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse bOQUploadValidationResponse = new BOQUploadValidationResponse();
        List<BOQUploadValidationResponse> listBOQUploadResponse = validateUploadedBOQ(boqDto);
        try {
            if (listBOQUploadResponse.isEmpty()) {
                boqDto.getUpload().forEach(upload -> {
                    Product product = productRepository.findByProductName(upload.getInventory());
                    UsageArea location = locationRepository.findByUsageAreaName(upload.getLocation());
                    BOQUpload boqUpload = bOQUploadRepository.findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(upload.getBuildingUnit(), location.getUsageAreaId(), product.getProductId());
                    bOQDetailsModification(upload, product, location, boqUpload);
                });
                bOQUploadValidationResponse.setMessage("Successfully done");
                listBOQUploadResponse.add(bOQUploadValidationResponse);
                return listBOQUploadResponse;
            } else {
                return listBOQUploadResponse;
            }
        } catch (Exception e) {
            bOQUploadValidationResponse.setMessage("Error while uploading the boq details");
            listBOQUploadResponse.add(bOQUploadValidationResponse);
        }
        return new ArrayList<BOQUploadValidationResponse>();
    }


    private void bOQDetailsModification(BOQUploadDto upload, Product product, UsageArea location, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION)) {
            addBoqRecords(upload, product, location, boqUpload);
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE)) {
            updateBoqQuantity(upload, boqUpload);
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)) {
            deleteBoqQuantity(upload, boqUpload);
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPSERT)) {
            upsertBoqRecord(upload, product, location, boqUpload);
        }
    }


    private String resolveCurrentUser() {
        try {
            return userDetailsService.getCurrentUser().getUsername();
        } catch (Exception e) {
            return "System";
        }
    }

    private void addBoqRecords(BOQUploadDto upload, Product product, UsageArea location, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (boqUpload == null) {
            BOQUpload saved = saveAndReturn(upload.getBuildingType(), upload.getBuildingUnit(),
                    location.getUsageAreaId(), product.getProductId(), upload.getQuantity(),
                    upload.getSno(), upload.getChanges());
            if (saved != null) {
                boqHistoryService.record("Added", resolveCurrentUser(), saved, null,
                        Double.parseDouble(upload.getQuantity()));
            }
        }
    }


    private void deleteBoqQuantity(BOQUploadDto upload, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (boqUpload != null) {
            double oldQty = boqUpload.getQuantity();
            boqUpload.setQuantity(0);
            boqUpload.setChanges(upload.getChanges());
            bOQUploadRepository.softDelete(boqUpload);
            boqHistoryService.record("Deleted", resolveCurrentUser(), boqUpload, oldQty, 0.0);
        }
    }


    private void updateBoqQuantity(BOQUploadDto upload, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (boqUpload != null) {
            double oldQty = boqUpload.getQuantity();
            double newQty = Double.parseDouble(upload.getQuantity());
            boqUpload.setChanges(upload.getChanges());
            boqUpload.setQuantity(newQty);
            bOQUploadRepository.save(boqUpload);
            if (oldQty != newQty) {
                boqHistoryService.record("Updated", resolveCurrentUser(), boqUpload, oldQty, newQty);
            }
        }
    }


    private void save(long buildingTypeId, long buildingUnit, long usageAreaId, long productId, String quantity, int sNo, String changes) {
        saveAndReturn(buildingTypeId, buildingUnit, usageAreaId, productId, quantity, sNo, changes);
    }

    private BOQUpload saveAndReturn(long buildingTypeId, long buildingUnit, long usageAreaId, long productId, String quantity, int sNo, String changes) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUpload boqUpload = new BOQUpload();

        UsageArea location = locationRepository.findByUsageAreaId(usageAreaId);
        boqUpload.setLocation(location);

        BuildingType buildingType = buildingTypeRepository.findByTypeId(buildingTypeId);
        boqUpload.setBuildingType(buildingType);

        Product product = productRepository.findByProductId(productId);
        boqUpload.setProduct(product);

        double doublQquntity = Double.parseDouble(quantity);
        boqUpload.setQuantity(doublQquntity);
        boqUpload.setChanges(changes);
        boqUpload.setSno(sNo);

        boqUpload.setUsageLocation(usageLocationRepository.findByLocationId((long) buildingUnit));
        return bOQUploadRepository.save(boqUpload);
    }


    public byte[] generateSampleExcel() throws IOException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        List<Product> allProducts = productRepository.findAll()
                .stream().sorted(Comparator.comparing(Product::getProductName)).collect(Collectors.toList());

        // Group products by category for cascading dropdown
        Map<String, List<Product>> byCategory = new LinkedHashMap<>();
        for (Product p : allProducts) {
            if (p.getCategory() != null) {
                byCategory.computeIfAbsent(p.getCategory().getCategoryName(), k -> new ArrayList<>()).add(p);
            }
        }
        List<String> sortedCategoryNames = byCategory.keySet().stream().sorted().collect(Collectors.toList());
        List<String> locationNames = locationRepository.getNames();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("BOQ Template");
            Row header = sheet.createRow(0);
            // Col 0: Category  Col 1: Inventory  Col 2: Unit (VLOOKUP, read-only)
            // Col 3: Quantity  Col 4: FinalLocation  Col 5: Changes
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Inventory");
            header.createCell(2).setCellValue("Unit");
            header.createCell(3).setCellValue("Quantity");
            header.createCell(4).setCellValue("FinalLocation");
            header.createCell(5).setCellValue("Changes");

            // Hidden: Categories — col A = display name, col B = named range key for INDIRECT
            Sheet categorySheet = workbook.createSheet("Categories");
            Map<String, Integer> usedRangeNames = new HashMap<>();
            List<String> rangeKeys = new ArrayList<>();
            for (int i = 0; i < sortedCategoryNames.size(); i++) {
                String catName = sortedCategoryNames.get(i);
                String rangeKey = uniqueRangeName(toRangeName(catName), usedRangeNames);
                rangeKeys.add(rangeKey);
                Row r = categorySheet.createRow(i);
                r.createCell(0).setCellValue(catName);
                r.createCell(1).setCellValue(rangeKey);
            }
            workbook.setSheetHidden(workbook.getSheetIndex("Categories"), true);

            // Hidden: CategoryProducts — one column per category; named range per column
            Sheet catProdSheet = workbook.createSheet("CategoryProducts");
            for (int ci = 0; ci < sortedCategoryNames.size(); ci++) {
                String catName = sortedCategoryNames.get(ci);
                List<String> prods = byCategory.get(catName).stream()
                        .map(Product::getProductName).sorted().collect(Collectors.toList());
                String colLetter = CellReference.convertNumToColString(ci);
                for (int ri = 0; ri < prods.size(); ri++) {
                    Row row = catProdSheet.getRow(ri);
                    if (row == null) row = catProdSheet.createRow(ri);
                    row.createCell(ci).setCellValue(prods.get(ri));
                }
                Name nr = workbook.createName();
                nr.setNameName(rangeKeys.get(ci));
                nr.setRefersToFormula("CategoryProducts!$" + colLetter + "$1:$" + colLetter + "$" + prods.size());
            }
            workbook.setSheetHidden(workbook.getSheetIndex("CategoryProducts"), true);

            // Hidden: ProductUnits — col A = product name, col B = unit (for VLOOKUP in Unit column)
            Sheet puSheet = workbook.createSheet("ProductUnits");
            for (int i = 0; i < allProducts.size(); i++) {
                Row r = puSheet.createRow(i);
                r.createCell(0).setCellValue(allProducts.get(i).getProductName());
                String mu = allProducts.get(i).getMeasurementUnit();
                r.createCell(1).setCellValue(mu != null ? mu : "");
            }
            workbook.setSheetHidden(workbook.getSheetIndex("ProductUnits"), true);

            // Named range covering all products — used by Inventory dropdown when no category is selected
            int puSize = allProducts.size();
            Name allProductsRange = workbook.createName();
            allProductsRange.setNameName("AllProducts");
            allProductsRange.setRefersToFormula("ProductUnits!$A$1:$A$" + puSize);

            // Hidden: Locations
            Sheet locationSheet = workbook.createSheet("Locations");
            for (int i = 0; i < locationNames.size(); i++) {
                locationSheet.createRow(i).createCell(0).setCellValue(locationNames.get(i));
            }
            workbook.setSheetHidden(workbook.getSheetIndex("Locations"), true);

            // Unit VLOOKUP formula for data rows (auto-populates when Inventory is selected)
            for (int r = 1; r <= 1000; r++) {
                Row dataRow = sheet.createRow(r);
                dataRow.createCell(2).setCellFormula(
                    "IFERROR(VLOOKUP(B" + (r + 1) + ",ProductUnits!$A$1:$B$" + puSize + ",2,FALSE),\"\")");
            }

            DataValidationHelper dvHelper = sheet.getDataValidationHelper();

            // Category dropdown (col 0)
            DataValidation catv = dvHelper.createValidation(
                    dvHelper.createFormulaListConstraint("Categories!$A$1:$A$" + sortedCategoryNames.size()),
                    new CellRangeAddressList(1, 1000, 0, 0));
            catv.setShowErrorBox(true);
            sheet.addValidationData(catv);

            // Inventory cascading dropdown (col 1)
            // No category selected → AllProducts (all inventory); category selected → filtered named range
            // A2 is relative — Excel adjusts per row (A3 for row 3, A4 for row 4, etc.)
            DataValidation pv = dvHelper.createValidation(
                    dvHelper.createFormulaListConstraint(
                        "INDIRECT(IF(A2=\"\",\"AllProducts\",VLOOKUP(A2,Categories!$A$1:$B$" + sortedCategoryNames.size() + ",2,0)))"),
                    new CellRangeAddressList(1, 1000, 1, 1));
            pv.setShowErrorBox(false);
            sheet.addValidationData(pv);

            // FinalLocation dropdown (col 4)
            DataValidation lv = dvHelper.createValidation(
                    dvHelper.createFormulaListConstraint("Locations!$A$1:$A$" + locationNames.size()),
                    new CellRangeAddressList(1, 1000, 4, 4));
            lv.setShowErrorBox(true);
            sheet.addValidationData(lv);

            // Changes dropdown (col 5)
            DataValidation cv = dvHelper.createValidation(
                    dvHelper.createExplicitListConstraint(new String[]{"addition", "update", "deletion"}),
                    new CellRangeAddressList(1, 1000, 5, 5));
            cv.setShowErrorBox(true);
            sheet.addValidationData(cv);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }


    public byte[] generateExistingBoqExcel(Long buildingTypeId, Long buildingUnitId) throws IOException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<BOQUpload> boqList = bOQUploadRepository
                .findByBuildingTypeTypeIdAndUsageLocationLocationId(buildingTypeId, buildingUnitId);

        List<Product> allProducts = productRepository.findAll()
                .stream().sorted(Comparator.comparing(Product::getProductName)).collect(Collectors.toList());

        Map<String, List<Product>> byCategory = new LinkedHashMap<>();
        for (Product p : allProducts) {
            if (p.getCategory() != null) {
                byCategory.computeIfAbsent(p.getCategory().getCategoryName(), k -> new ArrayList<>()).add(p);
            }
        }
        List<String> sortedCategoryNames = byCategory.keySet().stream().sorted().collect(Collectors.toList());
        List<String> locationNames = locationRepository.getNames();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Existing BOQ");
            Row header = sheet.createRow(0);
            // Col 0: Category  Col 1: Inventory  Col 2: Unit (pre-filled, reference only)
            // Col 3: Quantity  Col 4: FinalLocation
            header.createCell(0).setCellValue("Category");
            header.createCell(1).setCellValue("Inventory");
            header.createCell(2).setCellValue("Unit");
            header.createCell(3).setCellValue("Quantity");
            header.createCell(4).setCellValue("FinalLocation");

            // Hidden: Categories — col A = display name, col B = named range key
            Sheet categorySheet = workbook.createSheet("Categories");
            Map<String, Integer> usedRangeNames = new HashMap<>();
            List<String> rangeKeys = new ArrayList<>();
            for (int i = 0; i < sortedCategoryNames.size(); i++) {
                String catName = sortedCategoryNames.get(i);
                String rangeKey = uniqueRangeName(toRangeName(catName), usedRangeNames);
                rangeKeys.add(rangeKey);
                Row r = categorySheet.createRow(i);
                r.createCell(0).setCellValue(catName);
                r.createCell(1).setCellValue(rangeKey);
            }
            workbook.setSheetHidden(workbook.getSheetIndex("Categories"), true);

            // Hidden: CategoryProducts — one column per category with named ranges
            Sheet catProdSheet = workbook.createSheet("CategoryProducts");
            for (int ci = 0; ci < sortedCategoryNames.size(); ci++) {
                String catName = sortedCategoryNames.get(ci);
                List<String> prods = byCategory.get(catName).stream()
                        .map(Product::getProductName).sorted().collect(Collectors.toList());
                String colLetter = CellReference.convertNumToColString(ci);
                for (int ri = 0; ri < prods.size(); ri++) {
                    Row row = catProdSheet.getRow(ri);
                    if (row == null) row = catProdSheet.createRow(ri);
                    row.createCell(ci).setCellValue(prods.get(ri));
                }
                Name nr = workbook.createName();
                nr.setNameName(rangeKeys.get(ci));
                nr.setRefersToFormula("CategoryProducts!$" + colLetter + "$1:$" + colLetter + "$" + prods.size());
            }
            workbook.setSheetHidden(workbook.getSheetIndex("CategoryProducts"), true);

            // Hidden: ProductUnits — for VLOOKUP in Unit column
            Sheet puSheet = workbook.createSheet("ProductUnits");
            for (int i = 0; i < allProducts.size(); i++) {
                Row r = puSheet.createRow(i);
                r.createCell(0).setCellValue(allProducts.get(i).getProductName());
                String mu = allProducts.get(i).getMeasurementUnit();
                r.createCell(1).setCellValue(mu != null ? mu : "");
            }
            workbook.setSheetHidden(workbook.getSheetIndex("ProductUnits"), true);

            // Named range covering all products — used when no category selected
            int puSize = allProducts.size();
            Name allProductsRange = workbook.createName();
            allProductsRange.setNameName("AllProducts");
            allProductsRange.setRefersToFormula("ProductUnits!$A$1:$A$" + puSize);

            // Hidden: Locations
            Sheet locationSheet = workbook.createSheet("Locations");
            for (int i = 0; i < locationNames.size(); i++) {
                locationSheet.createRow(i).createCell(0).setCellValue(locationNames.get(i));
            }
            workbook.setSheetHidden(workbook.getSheetIndex("Locations"), true);

            DataValidationHelper dvHelper = sheet.getDataValidationHelper();
            int lastRow = Math.max(boqList.size(), 1000);

            // Unit VLOOKUP formula for all data rows — auto-updates when Inventory changes
            for (int r = 1; r <= lastRow; r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow == null) dataRow = sheet.createRow(r);
                dataRow.createCell(2).setCellFormula(
                    "IFERROR(VLOOKUP(B" + (r + 1) + ",ProductUnits!$A$1:$B$" + puSize + ",2,FALSE),\"\")");
            }

            // Category dropdown (col 0)
            DataValidation catv = dvHelper.createValidation(
                    dvHelper.createFormulaListConstraint("Categories!$A$1:$A$" + sortedCategoryNames.size()),
                    new CellRangeAddressList(1, lastRow, 0, 0));
            catv.setShowErrorBox(true);
            sheet.addValidationData(catv);

            // Inventory cascading dropdown (col 1)
            // No category → AllProducts; category selected → filtered named range
            DataValidation pv = dvHelper.createValidation(
                    dvHelper.createFormulaListConstraint(
                        "INDIRECT(IF(A2=\"\",\"AllProducts\",VLOOKUP(A2,Categories!$A$1:$B$" + sortedCategoryNames.size() + ",2,0)))"),
                    new CellRangeAddressList(1, lastRow, 1, 1));
            pv.setShowErrorBox(false);
            sheet.addValidationData(pv);

            // FinalLocation dropdown (col 4)
            DataValidation lv = dvHelper.createValidation(
                    dvHelper.createFormulaListConstraint("Locations!$A$1:$A$" + locationNames.size()),
                    new CellRangeAddressList(1, lastRow, 4, 4));
            lv.setShowErrorBox(true);
            sheet.addValidationData(lv);

            // Pre-fill data rows from existing BOQ (col 2/Unit is driven by formula, not hardcoded)
            int rowIdx = 1;
            for (BOQUpload b : boqList) {
                Row row = sheet.getRow(rowIdx++);
                if (row == null) row = sheet.createRow(rowIdx - 1);
                String categoryName = (b.getProduct().getCategory() != null)
                        ? b.getProduct().getCategory().getCategoryName() : "";
                row.createCell(0).setCellValue(categoryName);
                row.createCell(1).setCellValue(b.getProduct().getProductName());
                // col 2 already has VLOOKUP formula — do not overwrite
                row.createCell(3).setCellValue(b.getQuantity());
                row.createCell(4).setCellValue(b.getLocation().getUsageAreaName());
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }


    private void upsertBoqRecord(BOQUploadDto upload, Product product, UsageArea location, BOQUpload boqUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (boqUpload == null) {
            BOQUpload saved = saveAndReturn(upload.getBuildingType(), upload.getBuildingUnit(),
                    location.getUsageAreaId(), product.getProductId(), upload.getQuantity(),
                    upload.getSno(), BOQUploadConstant.UPDATE);
            if (saved != null) {
                boqHistoryService.record("Added", resolveCurrentUser(), saved, null,
                        Double.parseDouble(upload.getQuantity()));
            }
        } else {
            double oldQty = boqUpload.getQuantity();
            double newQty = Double.parseDouble(upload.getQuantity());
            boqUpload.setQuantity(newQty);
            boqUpload.setChanges(BOQUploadConstant.UPDATE);
            bOQUploadRepository.save(boqUpload);
            if (oldQty != newQty) {
                boqHistoryService.record("Updated", resolveCurrentUser(), boqUpload, oldQty, newQty);
            }
        }
    }


    /** Converts a category display name into a valid Excel named range identifier. */
    private String toRangeName(String name) {
        String result = name.replaceAll("[^a-zA-Z0-9]", "_");
        if (result.isEmpty() || Character.isDigit(result.charAt(0))) result = "Cat_" + result;
        return result;
    }

    /** Returns a unique range name, appending a suffix if the base name was already used. */
    private String uniqueRangeName(String base, Map<String, Integer> used) {
        if (!used.containsKey(base)) { used.put(base, 1); return base; }
        int n = used.get(base) + 1; used.put(base, n); return base + "_" + n;
    }


    private List<BOQUploadValidationResponse> validateUploadedBOQ(BOQDto boqDto) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<BOQUploadValidationResponse> listboqBoqUploadResponses = new ArrayList<>();
        List<BOQUpload> listOfBOQUpload = bOQUploadRepository.findAll();
        boqDto.getUpload().forEach(upload -> {
            boolean isInventoryExist = productRepository.existsByProductName(upload.getInventory());
            boolean isLocationExist = locationRepository.existsByUsageAreaName(upload.getLocation());
            try {
                double doublQuantity = Double.parseDouble(upload.getQuantity());
                boolean isValidChanges = upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION)
                        || upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE)
                        || upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)
                        || upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPSERT);
                if (!isInventoryExist || !isLocationExist || !isValidChanges) {
                    validateInventoryLocationChanges(listboqBoqUploadResponses, upload, isInventoryExist, isLocationExist);
                } else if (!upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPSERT)) {
                    validateChanges(listboqBoqUploadResponses, upload, listOfBOQUpload);
                }
            } catch (Exception e) {
                validateBOQQuantity(listboqBoqUploadResponses, upload);
            }
        });

        return listboqBoqUploadResponses;
    }


    private void validateBOQQuantity(List<BOQUploadValidationResponse> listboqBoqUploadResponses, BOQUploadDto upload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse boqUploadResponse = new BOQUploadValidationResponse();
        List<String> column = new ArrayList<String>();
        column.add(BOQUploadConstant.QUANTITY);
        boqUploadResponse.setSno(upload.getSno());
        boqUploadResponse.setColumns(column);
        listboqBoqUploadResponses.add(boqUploadResponse);
    }


    private void validateChanges(List<BOQUploadValidationResponse> listboqBoqUploadResponses, BOQUploadDto upload, List<BOQUpload> listOfBOQUpload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Product product = productRepository.findByProductName(upload.getInventory());
        UsageArea location = locationRepository.findByUsageAreaName(upload.getLocation());
        BOQUpload boqUpload = bOQUploadRepository.findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(upload.getBuildingUnit(), (long) location.getUsageAreaId(), (long) product.getProductId());
        if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION)) {
            if (boqUpload != null) {
                BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
                boqUploadResponse.setMessage("record will be overwritten");
                listboqBoqUploadResponses.add(boqUploadResponse);
            }
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE)) {
            if (boqUpload == null) {
                BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
                boqUploadResponse.setMessage("record not exist");
                listboqBoqUploadResponses.add(boqUploadResponse);
            }
        } else if (upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)) {
            if (boqUpload == null) {

                BOQUploadValidationResponse boqUploadResponse = setInventoryQuantityChangesLocation(upload);
                boqUploadResponse.setMessage("record not exist");
                listboqBoqUploadResponses.add(boqUploadResponse);
            }
        }
    }


    private BOQUploadValidationResponse setInventoryQuantityChangesLocation(BOQUploadDto upload) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse boqUploadResponse = new BOQUploadValidationResponse();
        List<String> column = new ArrayList<String>();
        column.add(BOQUploadConstant.INVENTORY);
        column.add(BOQUploadConstant.QUANTITY);
        column.add(BOQUploadConstant.CHANGES);
        column.add(BOQUploadConstant.FINAL_LOCATION);
        boqUploadResponse.setColumns(column);
        boqUploadResponse.setSno(upload.getSno());
        return boqUploadResponse;
    }


    private void validateInventoryLocationChanges(List<BOQUploadValidationResponse> listboqBoqUploadResponses, BOQUploadDto upload, boolean existInventory, boolean existLocation) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQUploadValidationResponse bOQUploadResponse = new BOQUploadValidationResponse();
        List<String> columnName = new ArrayList<String>();
        if (!existInventory)
            columnName.add(BOQUploadConstant.INVENTORY);
        if (!existLocation)
            columnName.add(BOQUploadConstant.FINAL_LOCATION);
        if (upload.getChanges().isEmpty()) {
            columnName.add(BOQUploadConstant.CHANGES);
        } else {
            boolean isValidAction = upload.getChanges().equalsIgnoreCase(BOQUploadConstant.ADDITION)
                    || upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPDATE)
                    || upload.getChanges().equalsIgnoreCase(BOQUploadConstant.DELETION)
                    || upload.getChanges().equalsIgnoreCase(BOQUploadConstant.UPSERT);
            if (!isValidAction) {
                columnName.add(BOQUploadConstant.CHANGES);
            }
        }
        bOQUploadResponse.setSno(upload.getSno());
        bOQUploadResponse.setColumns(columnName);

        listboqBoqUploadResponses.add(bOQUploadResponse);
    }


    public UsageLocationResponse getBuildingUnitByBuildingType(long buildingTypeId) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        UsageLocationResponse usageLocationResponse = new UsageLocationResponse();

        List<UsageLocationDto> listusageLocationDtos = new ArrayList<>();
        List<UsageLocation> usageLocation = usageLocationRepository.findByBuildingTypeTypeId(buildingTypeId);
        int count = 0;
        for (UsageLocation usageLocation1 : usageLocation) {
            count++;
            UsageLocationDto usageLocationDtos = new UsageLocationDto();
            usageLocationDtos.setId(usageLocation1.getLoationId());
            usageLocationDtos.setName(usageLocation1.getLocationName());
            listusageLocationDtos.add(usageLocationDtos);
        }
        usageLocationResponse.setUsageLocationCount(count);
        usageLocationResponse.setUsageLocation(listusageLocationDtos);

        return usageLocationResponse;
    }


    public BOQReportResponse getBoqReport() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        BOQReportResponse bOQReportResponse = new BOQReportResponse();
        try {
            List<Product> listOfProduct = productRepository.findAll();
            List<BOQReportDto> listBOQReportResponse = new ArrayList<>();
            List<Object> listOfBuildingTypeBuildingUnitProduct = bOQUploadRepository.findBuildigTypeIdBuildingUnitIdProductId();
            listOfBuildingTypeBuildingUnitProduct.forEach(iterateList -> {

                Object[] objArray = (Object[]) iterateList;
                BigInteger bigIntegerNumber0 = (BigInteger) objArray[0];
                BigInteger bigIntegerNumber1 = (BigInteger) objArray[1];
                BigInteger bigIntegerNumber2 = (BigInteger) objArray[2];
                Long productId = bigIntegerNumber0.longValue();
                Long buildingType = bigIntegerNumber1.longValue();
                Long buildingUnit = bigIntegerNumber2.longValue();

                BOQReportDto bOQReportDto = new BOQReportDto();
                List<Object> list = inwardOutwardListRepo.findByOutwardInventory(buildingType, buildingUnit, productId);
                if (!list.isEmpty()) {
                    getExcessQuantity(listBOQReportResponse, buildingType, buildingUnit, productId, bOQReportDto, listOfProduct, list);
                }
            });
            bOQReportResponse.setMessage("Get BOQ Report successfully");
            bOQReportResponse.setBoqreports(listBOQReportResponse);
        } catch (Exception e) {
            bOQReportResponse.setMessage("Error while retrieving the report");
        }
        return bOQReportResponse;
    }


    private void getExcessQuantity(List<BOQReportDto> listBOQReportResponse, Long buildingType, Long buildingUnit, Long productId, BOQReportDto bOQReportDto, List<Product> listOfProduct, List<Object> list) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Map<Long, String> mapProductName = listOfProduct.stream().collect(
                Collectors.toMap(Product::getProductId, Product::getProductName));
        Map<Long, String> mapMeasurementUnit = listOfProduct.stream().collect(
                Collectors.toMap(Product::getProductId, Product::getMeasurementUnit));
        long count = 0;
        double outwardQuantity = 0.0;
        for (Object cdata : list) {
            Object[] obj = (Object[]) cdata;
            if (obj[1] != null && obj[2] != null) {
                Double quantity = (Double) obj[5];
                outwardQuantity = outwardQuantity + quantity;
                count++;
            }
        }
        Double boqQuantity = bOQUploadRepository.findQuantityByProductProductId(productId, buildingType, buildingUnit);
        Double excessQuantity = outwardQuantity - boqQuantity;
        if (excessQuantity > 0) {
            Optional<BuildingType> building = buildingTypeRepository.findById(buildingType.longValue());
            Optional<UsageLocation> usageLocation = usageLocationRepository.findById(buildingUnit.longValue());
            bOQReportDto.setBoqQuantity(boqQuantity);
            bOQReportDto.setInventory(mapProductName.get(productId));
            bOQReportDto.setBoqQuantity(boqQuantity);
            bOQReportDto.setBuildingType(building.get().getTypeName());
            bOQReportDto.setBuildingUnit(usageLocation.get().getLocationName());
            bOQReportDto.setOutwardQuantity(outwardQuantity);
            bOQReportDto.setExcessQuantity(excessQuantity);
            bOQReportDto.setMeasurementUnit(mapMeasurementUnit.get(productId));
            listBOQReportResponse.add(bOQReportDto);
        }
    }


    public BOQInformation fetchBoqStatusInformationv2(BOQStatusFilterDataList filterDataList, Pageable page) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        List<Object[]> rawRows = bOQUploadRepository.fetchBOQStatusRows();

        // Extract per-field filter values from the request
        List<String> buildingTypeFilter = extractFilter(filterDataList, "buildingType");
        List<String> buildingUnitFilter  = extractFilter(filterDataList, "buildingUnit");
        List<String> productFilter       = extractFilter(filterDataList, "product");
        List<String> categoryFilter      = extractFilter(filterDataList, "category");
        List<String> statusBucketFilter  = extractFilter(filterDataList, "consumedPercentage");

        // Filter raw rows on table-level fields (before grouping)
        List<Object[]> filtered = rawRows.stream()
                .filter(r -> matchesContains((String) r[2], buildingTypeFilter))  // building_type
                .filter(r -> matchesContains((String) r[4], buildingUnitFilter))  // location_name
                .filter(r -> matchesContains((String) r[8], productFilter))       // product_name
                .filter(r -> matchesContains((String) r[9], categoryFilter))      // category_name
                .collect(Collectors.toList());

        // Group rows and compute status
        List<BOQStatusDto> allDtos = buildGroupedDtos(filtered);

        // Filter by statusBucket (applied after grouping since it is a derived field)
        if (statusBucketFilter != null && !statusBucketFilter.isEmpty()) {
            allDtos = allDtos.stream()
                    .filter(d -> statusBucketFilter.stream()
                            .anyMatch(f -> f.equalsIgnoreCase(d.getStatusBucket())))
                    .collect(Collectors.toList());
        }

        // Compute summary counts (before statusGroup quick-filter so cards always show full breakdown)
        // status = (outward-boq)/boq*100; consumed% = status+100
        // on-track: consumed < 80%  → status < -20
        // at-risk:  consumed 80-100% → -20 <= status <= 0
        // exceeded: consumed > 100%  → status > 0
        long onTrackCount  = allDtos.stream().filter(d -> d.getStatus() < -20).count();
        long atRiskCount   = allDtos.stream().filter(d -> d.getStatus() >= -20 && d.getStatus() <= 0).count();
        long exceededCount = allDtos.stream().filter(d -> d.getStatus() > 0).count();
        long totalCount    = allDtos.size();

        // Apply statusGroup quick-filter (card clicks)
        List<String> statusGroupFilter = extractFilter(filterDataList, "statusGroup");
        if (statusGroupFilter != null && !statusGroupFilter.isEmpty()) {
            allDtos = allDtos.stream()
                    .filter(d -> statusGroupFilter.stream().anyMatch(g -> {
                        double s = d.getStatus();
                        if ("onTrack".equalsIgnoreCase(g))   return s < -20;
                        if ("atRisk".equalsIgnoreCase(g))    return s >= -20 && s <= 0;
                        if ("exceeded".equalsIgnoreCase(g))  return s > 0;
                        return false;
                    }))
                    .collect(Collectors.toList());
        }

        // Sort, then paginate in Java
        sortDtos(allDtos, page.getSort());
        int total    = allDtos.size();
        int start    = (int) page.getOffset();
        int end      = Math.min(start + page.getPageSize(), total);
        List<BOQStatusDto> pageContent = start >= total ? new ArrayList<>() : allDtos.subList(start, end);

        BOQInformation result = new BOQInformation();
        result.setBoqstatusDto(new PageImpl<>(pageContent, page, total));
        result.setTotalCount(totalCount);
        result.setOnTrackCount(onTrackCount);
        result.setAtRiskCount(atRiskCount);
        result.setExceededCount(exceededCount);
        return result;
    }

    /**
     * Checks BOQ enforcement for all line items in an outward entry.
     * Rules:
     *  - If no BOQ record exists for (usageLocationId, productId) → skip (no enforcement)
     *  - If BOQ record exists and (currentOutward + newQty) / totalBOQ >= 1.0 → violation
     *  - If boqEnforcementBlock=true → throws Exception listing all violating products
     *  - If boqEnforcementBlock=false → logs warning only (frontend already warned user)
     *
     * @param usageLocationId  building unit (usageLocation) ID
     * @param items            list of (productId, newQuantity) pairs
     */
    /**
     * Enforces BOQ limits for outward inventory and also returns whether ALL products
     * in the list have a BOQ configured (totalBOQ > 0) for the given usageLocation.
     *
     * @return true if every product has BOQ > 0, false if any product has no BOQ
     */
    public boolean enforceBOQLimits(Long usageLocationId, List<com.ec.application.data.ProductWithQuantity> items) throws Exception {
        log.info("Invoked enforceBOQLimits");
        if (usageLocationId == null || items == null || items.isEmpty()) return false;

        boolean allHaveBOQ = true;
        List<String> violations = new ArrayList<>();

        for (com.ec.application.data.ProductWithQuantity item : items) {
            Long productId = item.getProductId();
            Double newQty  = item.getQuantity();

            // native aggregate query always returns exactly one row; use List to avoid Object[] cast issues
            List<Object[]> rows = bOQUploadRepository.fetchAggregatedBOQAndOutward(usageLocationId, productId);
            if (rows == null || rows.isEmpty()) { allHaveBOQ = false; continue; }
            Object[] row = rows.get(0);
            if (row == null) { allHaveBOQ = false; continue; }

            double totalBOQ     = row[0] != null ? ((Number) row[0]).doubleValue() : 0;
            double totalOutward = row[1] != null ? ((Number) row[1]).doubleValue() : 0;

            if (totalBOQ <= 0) { allHaveBOQ = false; continue; } // no BOQ configured for this product+unit

            // BOQ exists — now check consumption limits (only when enforcement is enabled)
            if (boqEnforcementBlock) {
                double afterQty        = totalOutward + newQty;
                double consumedPercent = (afterQty / totalBOQ) * 100;

                if (consumedPercent > 100) {
                    Product product = productRepository.findByProductId(productId);
                    String productName = product != null ? product.getProductName() : ("Product ID " + productId);
                    double remaining = Math.max(totalBOQ - totalOutward, 0);
                    violations.add(String.format(
                        "%s: BOQ limit is %.2f, already consumed %.2f, remaining %.2f but requested %.2f",
                        productName, totalBOQ, totalOutward, remaining, newQty
                    ));
                }
            }
        }

        if (!violations.isEmpty()) {
            throw new Exception("BOQ limit exceeded for the following items — save blocked:\n" +
                    String.join("\n", violations));
        }

        return allHaveBOQ;
    }

    public BOQDashboardResponse getBOQDashboardData() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        List<Object[]> rawRows = bOQUploadRepository.fetchBOQStatusRows();
        List<BOQStatusDto> allDtos = buildGroupedDtos(rawRows);

        // Aggregate boq/outward quantities per product across all building units
        Map<String, double[]> productAgg = new LinkedHashMap<>();
        for (BOQStatusDto dto : allDtos) {
            String key = dto.getProduct();
            productAgg.computeIfAbsent(key, k -> new double[2]);
            productAgg.get(key)[0] += dto.getBoqQuantity();
            productAgg.get(key)[1] += dto.getOutwardQuantity();
        }

        long onTrackCount = 0, atRiskCount = 0, exceededCount = 0;
        List<BOQDashboardItem> allItems = new ArrayList<>();

        for (Map.Entry<String, double[]> entry : productAgg.entrySet()) {
            double boq     = entry.getValue()[0];
            double outward = entry.getValue()[1];
            double consumed = boq > 0 ? Math.round((outward / boq * 100) * 100.0) / 100.0 : 0;
            double status   = boq > 0 ? ((outward - boq) / boq * 100) : 0;

            String bucket;
            if (status > 0)        { bucket = "exceeded"; exceededCount++; }
            else if (status >= -20) { bucket = "atRisk";   atRiskCount++; }
            else                    { bucket = "onTrack";  onTrackCount++; }

            allItems.add(new BOQDashboardItem(entry.getKey(), consumed, bucket));
        }

        // Chart items: only >= 70% consumed, sorted desc, top 15
        List<BOQDashboardItem> chartItems = allItems.stream()
                .filter(i -> i.getConsumedPercent() >= 70)
                .sorted((a, b) -> Double.compare(b.getConsumedPercent(), a.getConsumedPercent()))
                .limit(15)
                .collect(Collectors.toList());

        BOQDashboardResponse response = new BOQDashboardResponse();
        response.setItems(chartItems);
        response.setTotalCount(allItems.size());
        response.setOnTrackCount(onTrackCount);
        response.setAtRiskCount(atRiskCount);
        response.setExceededCount(exceededCount);
        return response;
    }

    public String getBoqQuantityForOutward(Long productId, Long locationId, Long finalLocationId) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<Object[]> rows = bOQUploadRepository.fetchBOQAndOutwardForProduct(locationId, productId, finalLocationId);
        if (rows.isEmpty()) return "NA";
        Object[] row      = rows.get(0);
        double boqQty     = toDouble(row[0]);
        double outwardQty = toDouble(row[1]);
        return String.valueOf(boqQty - outwardQty);
    }

    // ── grouping helper (shared by fetch and export) ─────────────────────────

    private List<BOQStatusDto> buildGroupedDtos(List<Object[]> filtered) {
        Map<String, BOQStatusDto>              dtoMap    = new LinkedHashMap<>();
        Map<String, List<BOQStatusDetailsDto>> detailMap = new LinkedHashMap<>();

        for (Object[] r : filtered) {
            Long   buildingTypeId = toLong(r[1]);
            Long   locationId     = toLong(r[3]);
            Long   productId      = toLong(r[7]);
            String groupKey       = buildingTypeId + "_" + locationId + "_" + productId;

            double boqQty     = toDouble(r[10]);
            double outwardQty = toDouble(r[11]);

            final Long fBuildingTypeId = buildingTypeId;
            final Long fLocationId     = locationId;
            BOQStatusDto dto = dtoMap.computeIfAbsent(groupKey, k -> {
                BOQStatusDto d = new BOQStatusDto();
                d.setCategory((String) r[9]);
                d.setProduct((String) r[8]);
                d.setBuildingUnit((String) r[4]);
                d.setBoqQuantity(0.0);
                d.setOutwardQuantity(0.0);
                d.setBuildingTypeId(fBuildingTypeId);
                d.setBuildingUnitId(fLocationId);
                return d;
            });
            dto.setBoqQuantity(dto.getBoqQuantity() + boqQty);
            dto.setOutwardQuantity(dto.getOutwardQuantity() + outwardQty);

            BOQStatusDetailsDto detail = new BOQStatusDetailsDto();
            detail.setFinalLocation((String) r[6]);
            detail.setBoqQuantity(boqQty);
            detail.setOutwardQuantity(outwardQty);
            detailMap.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(detail);
        }

        List<BOQStatusDto> allDtos = new ArrayList<>();
        int counter = 1;
        for (Map.Entry<String, BOQStatusDto> entry : dtoMap.entrySet()) {
            BOQStatusDto dto = entry.getValue();
            dto.setId(counter++);
            dto.setBoqDetails(detailMap.get(entry.getKey()));
            double boqQty     = dto.getBoqQuantity();
            double outwardQty = dto.getOutwardQuantity();
            double status     = boqQty > 0
                    ? Math.round(((outwardQty - boqQty) / boqQty * 100) * 100.0) / 100.0
                    : 0.0;
            dto.setStatus(status);
            dto.setStatusBucket(computeStatusBucket(status));
            allDtos.add(dto);
        }
        return allDtos;
    }

    // ── BOQ status Excel export ───────────────────────────────────────────────

    public byte[] exportBOQStatusExcel(List<String> buildingTypes, List<String> buildingUnits,
                                        List<String> products, List<String> categories,
                                        List<String> statusBuckets) throws IOException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        List<Object[]> rawRows = bOQUploadRepository.fetchBOQStatusRows();

        List<Object[]> filtered = rawRows.stream()
                .filter(r -> matchesContains((String) r[2], buildingTypes))
                .filter(r -> matchesContains((String) r[4], buildingUnits))
                .filter(r -> matchesContains((String) r[8], products))
                .filter(r -> matchesContains((String) r[9], categories))
                .collect(Collectors.toList());

        List<BOQStatusDto> dtos = buildGroupedDtos(filtered);

        if (statusBuckets != null && !statusBuckets.isEmpty()) {
            dtos = dtos.stream()
                    .filter(d -> statusBuckets.stream().anyMatch(f -> f.equalsIgnoreCase(d.getStatusBucket())))
                    .collect(Collectors.toList());
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("BOQ Status");

            String[] headers = {
                "Building Unit", "Category", "Product",
                "Total BOQ Qty", "Total Outward Qty", "Status (%)", "Status Bucket",
                "Final Location", "Final Loc BOQ Qty", "Final Loc Outward Qty"
            };
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (BOQStatusDto dto : dtos) {
                List<BOQStatusDetailsDto> details = dto.getBoqDetails();
                if (details == null || details.isEmpty()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(dto.getBuildingUnit());
                    row.createCell(1).setCellValue(dto.getCategory());
                    row.createCell(2).setCellValue(dto.getProduct());
                    row.createCell(3).setCellValue(dto.getBoqQuantity());
                    row.createCell(4).setCellValue(dto.getOutwardQuantity());
                    row.createCell(5).setCellValue(dto.getStatus());
                    row.createCell(6).setCellValue(dto.getStatusBucket());
                } else {
                    for (BOQStatusDetailsDto detail : details) {
                        Row row = sheet.createRow(rowIdx++);
                        row.createCell(0).setCellValue(dto.getBuildingUnit());
                        row.createCell(1).setCellValue(dto.getCategory());
                        row.createCell(2).setCellValue(dto.getProduct());
                        row.createCell(3).setCellValue(dto.getBoqQuantity());
                        row.createCell(4).setCellValue(dto.getOutwardQuantity());
                        row.createCell(5).setCellValue(dto.getStatus());
                        row.createCell(6).setCellValue(dto.getStatusBucket());
                        row.createCell(7).setCellValue(detail.getFinalLocation());
                        row.createCell(8).setCellValue(detail.getBoqQuantity());
                        row.createCell(9).setCellValue(detail.getOutwardQuantity());
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<String> extractFilter(BOQStatusFilterDataList filterDataList, String attrName) {
        if (filterDataList == null || filterDataList.getFilterData() == null) return null;
        return filterDataList.getFilterData().stream()
                .filter(f -> attrName.equals(f.getAttrName()))
                .findFirst()
                .map(FilterAttributeData::getAttrValue)
                .orElse(null);
    }

    private boolean matchesContains(String value, List<String> filters) {
        if (filters == null || filters.isEmpty()) return true;
        if (value == null) return false;
        return filters.stream().anyMatch(f -> value.toLowerCase().contains(f.toLowerCase()));
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigInteger) return ((BigInteger) obj).longValue();
        if (obj instanceof Long)       return (Long) obj;
        if (obj instanceof Integer)    return ((Integer) obj).longValue();
        return Long.parseLong(obj.toString());
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Double)     return (Double) obj;
        if (obj instanceof BigDecimal) return ((BigDecimal) obj).doubleValue();
        if (obj instanceof Float)      return ((Float) obj).doubleValue();
        return Double.parseDouble(obj.toString());
    }

    private String computeStatusBucket(double status) {
        if (status <= 10)  return "0-10 %";
        if (status <= 20)  return "10-20 %";
        if (status <= 30)  return "20-30 %";
        if (status <= 40)  return "30-40 %";
        if (status <= 50)  return "40-50 %";
        if (status <= 60)  return "50-60 %";
        if (status <= 70)  return "60-70 %";
        if (status <= 80)  return "70-80 %";
        if (status <= 90)  return "80-90 %";
        if (status <= 100) return "90-100 %";
        return "above 100 %";
    }

    private void sortDtos(List<BOQStatusDto> dtos, Sort sort) {
        if (sort == null || sort.isUnsorted()) return;
        for (Sort.Order order : sort) {
            Comparator<BOQStatusDto> comp;
            switch (order.getProperty()) {
                case "category":      comp = Comparator.comparing(BOQStatusDto::getCategory,      Comparator.nullsFirst(Comparator.naturalOrder())); break;
                case "buildingUnit":  comp = Comparator.comparing(BOQStatusDto::getBuildingUnit,  Comparator.nullsFirst(Comparator.naturalOrder())); break;
                case "product":       comp = Comparator.comparing(BOQStatusDto::getProduct,       Comparator.nullsFirst(Comparator.naturalOrder())); break;
                case "boqQuantity":   comp = Comparator.comparingDouble(BOQStatusDto::getBoqQuantity);     break;
                case "outwardQuantity": comp = Comparator.comparingDouble(BOQStatusDto::getOutwardQuantity); break;
                case "status":        comp = Comparator.comparing(BOQStatusDto::getStatus,        Comparator.nullsFirst(Comparator.naturalOrder())); break;
                default: continue;
            }
            if (order.isDescending()) comp = comp.reversed();
            dtos.sort(comp);
        }
    }
}