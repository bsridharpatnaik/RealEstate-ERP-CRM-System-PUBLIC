package com.ec.application.service;

import com.ec.application.Filters.BOQHistorySpecifications;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.model.BOQHistory;
import com.ec.application.model.BOQInventoryMapping;
import com.ec.application.model.BOQUpload;
import com.ec.application.repository.BOQHistoryRepository;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class BOQHistoryService {

    @Autowired
    private BOQHistoryRepository boqHistoryRepository;

    Logger log = LoggerFactory.getLogger(BOQHistoryService.class);

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    /**
     * Record a BOQ change from Excel upload path.
     */
    public void record(String changeType, String changedBy, BOQUpload boqUpload,
                       Double oldQuantity, Double newQuantity, String remark) {
        try {
            BOQHistory h = new BOQHistory();
            h.setChangeDateTime(new Date());
            h.setChangeType(changeType);
            h.setChangedBy(changedBy);
            h.setBuildingType(boqUpload.getBuildingType());
            h.setUsageLocation(boqUpload.getUsageLocation());
            h.setProduct(boqUpload.getProduct());
            h.setFinalLocation(boqUpload.getLocation());
            h.setOldQuantity(oldQuantity);
            h.setNewQuantity(newQuantity);
            h.setRemark(remark != null && !remark.trim().isEmpty() ? remark : null);
            boqHistoryRepository.save(h);
        } catch (Exception e) {
            log.error("Failed to save BOQ history record: " + e.getMessage());
        }
    }

    /**
     * Record a BOQ change from the popup add/edit path (BOQInventoryMapping).
     */
    public void recordFromMapping(String changeType, String changedBy, BOQInventoryMapping mapping,
                                  Double oldQuantity, Double newQuantity, String remark) {
        try {
            BOQHistory h = new BOQHistory();
            h.setChangeDateTime(new Date());
            h.setChangeType(changeType);
            h.setChangedBy(changedBy);
            h.setBuildingType(mapping.getBuildingType());
            h.setUsageLocation(mapping.getLocation());
            h.setProduct(mapping.getProduct());
            h.setFinalLocation(null);
            h.setOldQuantity(oldQuantity);
            h.setNewQuantity(newQuantity);
            h.setRemark(remark != null && !remark.trim().isEmpty() ? remark : null);
            boqHistoryRepository.save(h);
        } catch (Exception e) {
            log.error("Failed to save BOQ history record (mapping): " + e.getMessage());
        }
    }

    public Page<BOQHistory> getFiltered(FilterDataList filterDataList, Pageable pageable) throws Exception {
        Specification<BOQHistory> spec = BOQHistorySpecifications.getSpecification(filterDataList);
        if (spec == null) {
            return boqHistoryRepository.findAll(pageable);
        }
        return boqHistoryRepository.findAll(spec, pageable);
    }

    public byte[] exportExcel(FilterDataList filterDataList) throws Exception {
        Specification<BOQHistory> spec = BOQHistorySpecifications.getSpecification(filterDataList);
        long count = spec == null ? boqHistoryRepository.count() : boqHistoryRepository.count(spec);
        if (count > 5000)
            throw new Exception("Too many rows to export. Please apply filters to reduce results below 5000 and try again.");
        List<BOQHistory> rows = (spec == null)
                ? boqHistoryRepository.findAll()
                : boqHistoryRepository.findAll(spec);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("BOQ History");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {
                "Change DateTime", "Structure Type", "Structure",
                "Product", "Category", "Work Area",
                "Old Qty", "New Qty", "Changed By", "Change Type", "Remark"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (BOQHistory h : rows) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(h.getChangeDateTime() != null ? DATE_FMT.format(h.getChangeDateTime()) : "");
                r.createCell(1).setCellValue(h.getBuildingType() != null ? h.getBuildingType().getTypeName() : "");
                r.createCell(2).setCellValue(h.getUsageLocation() != null ? h.getUsageLocation().getLocationName() : "");
                r.createCell(3).setCellValue(h.getProduct() != null ? h.getProduct().getProductName() : "");
                r.createCell(4).setCellValue(h.getProduct() != null && h.getProduct().getCategory() != null
                        ? h.getProduct().getCategory().getCategoryName() : "");
                r.createCell(5).setCellValue(h.getFinalLocation() != null ? h.getFinalLocation().getUsageAreaName() : "");
                r.createCell(6).setCellValue(h.getOldQuantity() != null ? h.getOldQuantity() : 0);
                r.createCell(7).setCellValue(h.getNewQuantity() != null ? h.getNewQuantity() : 0);
                r.createCell(8).setCellValue(h.getChangedBy() != null ? h.getChangedBy() : "");
                r.createCell(9).setCellValue(h.getChangeType() != null ? h.getChangeType() : "");
                r.createCell(10).setCellValue(h.getRemark() != null ? h.getRemark() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
