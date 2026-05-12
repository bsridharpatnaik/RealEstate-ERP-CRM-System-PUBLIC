package com.ec.application.service;

import com.ec.application.Filters.ActivityLogSpecification;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.model.ActivityLog;
import com.ec.application.repository.ActivityLogRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @Autowired
    private ActivityLogRepository activityLogRepository;

    /**
     * Record an activity asynchronously. Tenant context is propagated via TenantAwareTaskDecorator.
     * performedBy must be resolved on the calling thread before this method is invoked.
     * Never throws — failures are swallowed so the main operation is never affected.
     */
    @Async
    public void record(String action, String entityType, String entityId,
                       String description, String performedBy) {
        try {
            ActivityLog entry = new ActivityLog();
            entry.setActivityTime(new Date());
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDescription(description);
            entry.setPerformedBy(performedBy != null ? performedBy : "System");
            activityLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save activity log [{} {} {}]: {}", action, entityType, entityId, e.getMessage());
        }
    }

    public Page<ActivityLog> getFiltered(FilterDataList filterDataList, Pageable pageable) throws Exception {
        Specification<ActivityLog> spec = ActivityLogSpecification.getSpecification(filterDataList);
        if (spec == null) {
            return activityLogRepository.findAll(pageable);
        }
        return activityLogRepository.findAll(spec, pageable);
    }

    public byte[] exportExcel(FilterDataList filterDataList) throws Exception {
        Specification<ActivityLog> spec = ActivityLogSpecification.getSpecification(filterDataList);
        List<ActivityLog> rows = (spec == null)
                ? activityLogRepository.findAll()
                : activityLogRepository.findAll(spec);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Activity Log");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"Time", "Action", "Entity Type", "Entity ID", "Description", "Performed By"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (ActivityLog r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getActivityTime() != null ? DATE_FMT.format(r.getActivityTime()) : "");
                row.createCell(1).setCellValue(r.getAction() != null ? r.getAction() : "");
                row.createCell(2).setCellValue(r.getEntityType() != null ? r.getEntityType() : "");
                row.createCell(3).setCellValue(r.getEntityId() != null ? r.getEntityId() : "");
                row.createCell(4).setCellValue(r.getDescription() != null ? r.getDescription() : "");
                row.createCell(5).setCellValue(r.getPerformedBy() != null ? r.getPerformedBy() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    public int purgeOlderThan(Date cutoff) {
        return activityLogRepository.deleteByActivityTimeBefore(cutoff);
    }
}
