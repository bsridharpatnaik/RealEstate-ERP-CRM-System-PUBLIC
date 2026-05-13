package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.GlobalActivityLogSpecification;
import com.ec.application.config.SchemaConfig;
import com.ec.application.model.ActivityLog;
import com.ec.application.model.GlobalActivityLog;
import com.ec.application.repository.ActivityLogRepository;
import com.ec.application.repository.GlobalActivityLogRepository;
import com.ec.application.multitenant.ThreadLocalStorage;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogGlobalSyncService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogGlobalSyncService.class);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private final ActivityLogRepository activityLogRepository;
    private final GlobalActivityLogRepository globalActivityLogRepository;
    private final SchemaConfig schemaConfig;

    public void syncSingleTenant(String tenantSchema) {
        String masterSchema = schemaConfig.getMasterSchema();
        log.info("Starting activity log sync for tenant: {}", tenantSchema);

        // Step 1: get last synced ID from master
        ThreadLocalStorage.setTenantName(masterSchema);
        Long lastSyncedId = globalActivityLogRepository.findMaxTenantActivityLogIdBySchema(tenantSchema);
        if (lastSyncedId == null) lastSyncedId = 0L;
        log.info("Last synced activity_log id for {}: {}", tenantSchema, lastSyncedId);

        // Step 2: fetch new records from tenant
        ThreadLocalStorage.setTenantName(tenantSchema);
        List<ActivityLog> newLogs = activityLogRepository.findByIdGreaterThanOrderByIdAsc(lastSyncedId);

        if (newLogs.isEmpty()) {
            log.info("No new activity log entries for tenant: {}", tenantSchema);
            return;
        }

        // Step 3: insert into master
        Date syncedAt = new Date();
        ThreadLocalStorage.setTenantName(masterSchema);
        for (ActivityLog entry : newLogs) {
            GlobalActivityLog global = new GlobalActivityLog();
            global.setTenantSchema(tenantSchema);
            global.setTenantActivityLogId(entry.getId());
            global.setActivityTime(entry.getActivityTime());
            global.setAction(entry.getAction());
            global.setEntityType(entry.getEntityType());
            global.setEntityId(entry.getEntityId());
            global.setDescription(entry.getDescription());
            global.setPerformedBy(entry.getPerformedBy());
            global.setSyncedAt(syncedAt);
            globalActivityLogRepository.save(global);
        }
        log.info("Synced {} activity log entries for tenant: {}", newLogs.size(), tenantSchema);
    }

    public Page<GlobalActivityLog> getFiltered(FilterDataList filterDataList, Pageable pageable) throws Exception {
        Specification<GlobalActivityLog> spec = GlobalActivityLogSpecification.getSpecification(filterDataList);
        return spec == null
                ? globalActivityLogRepository.findAll(pageable)
                : globalActivityLogRepository.findAll(spec, pageable);
    }

    public byte[] exportExcel(FilterDataList filterDataList) throws Exception {
        Specification<GlobalActivityLog> spec = GlobalActivityLogSpecification.getSpecification(filterDataList);
        List<GlobalActivityLog> rows = spec == null
                ? globalActivityLogRepository.findAll()
                : globalActivityLogRepository.findAll(spec);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Global Activity Log");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"Time", "Project", "Action", "Entity Type", "Entity ID", "Description", "Performed By"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (GlobalActivityLog r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getActivityTime() != null ? DATE_FMT.format(r.getActivityTime()) : "");
                row.createCell(1).setCellValue(r.getTenantSchema() != null ? r.getTenantSchema() : "");
                row.createCell(2).setCellValue(r.getAction() != null ? r.getAction() : "");
                row.createCell(3).setCellValue(r.getEntityType() != null ? r.getEntityType() : "");
                row.createCell(4).setCellValue(r.getEntityId() != null ? r.getEntityId() : "");
                row.createCell(5).setCellValue(r.getDescription() != null ? r.getDescription() : "");
                row.createCell(6).setCellValue(r.getPerformedBy() != null ? r.getPerformedBy() : "");
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
