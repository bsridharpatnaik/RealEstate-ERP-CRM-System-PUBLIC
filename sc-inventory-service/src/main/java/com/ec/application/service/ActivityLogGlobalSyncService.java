package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.GlobalActivityLogSpecification;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.RoleConstants;
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
import java.util.ArrayList;
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
    private final UserDetailsService userDetailsService;

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

    // Global/master-schema entities (PO, Indent, Quote Comparison, Service Order, Product) are
    // logged under the master schema, so their rows carry tenant_schema = masterschema. The user's
    // allowed-schema list only has project tenants, which would filter those rows out of the global
    // log entirely — include master so cross-tenant entities are visible.
    //
    // Master is added ONLY for admins: these endpoints aren't role-guarded at the controller (the
    // global log is admin-only by frontend menu), so a non-admin who reaches /global/list must stay
    // scoped to their own project schemas and must NOT gain cross-tenant master-entity visibility.
    private List<String> allowedSchemasForGlobalView() throws Exception {
        List<String> allowedSchemas = new ArrayList<>(userDetailsService.getCurrentUserAllowedSchemas());
        String master = schemaConfig.getMasterSchema();
        if (master != null && userDetailsService.hasRole(RoleConstants.ADMIN) && !allowedSchemas.contains(master)) {
            allowedSchemas.add(master);
        }
        return allowedSchemas;
    }

    public Page<GlobalActivityLog> getFiltered(FilterDataList filterDataList, Pageable pageable) throws Exception {
        List<String> allowedSchemas = allowedSchemasForGlobalView();
        Specification<GlobalActivityLog> spec = GlobalActivityLogSpecification.getSpecification(filterDataList, allowedSchemas);
        return spec == null
                ? globalActivityLogRepository.findAll(pageable)
                : globalActivityLogRepository.findAll(spec, pageable);
    }

    public byte[] exportExcel(FilterDataList filterDataList) throws Exception {
        List<String> allowedSchemas = allowedSchemasForGlobalView();
        Specification<GlobalActivityLog> spec = GlobalActivityLogSpecification.getSpecification(filterDataList, allowedSchemas);
        long count = spec == null ? globalActivityLogRepository.count() : globalActivityLogRepository.count(spec);
        if (count > 5000)
            throw new Exception("Too many rows to export. Please apply filters to reduce results below 5000 and try again.");
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
