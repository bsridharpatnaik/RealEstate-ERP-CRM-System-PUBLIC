package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FileMigrationService {

    Logger log = LoggerFactory.getLogger(FileMigrationService.class);

    @Autowired MinIOFileStorageService minIOFileStorageService;
    @Autowired SchemaConfig schemaConfig;
    @Autowired JdbcTemplate jdbcTemplate;

    private volatile boolean running    = false;
    private volatile boolean completed  = false;
    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicInteger migrated   = new AtomicInteger(0);
    private final AtomicInteger skipped    = new AtomicInteger(0);
    private final AtomicInteger errors     = new AtomicInteger(0);
    private volatile String currentSchema  = "";
    private volatile String startedAt      = null;
    private volatile String completedAt    = null;
    private volatile String lastError      = null;

    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("running", running);
        s.put("completed", completed);
        s.put("totalFiles", totalFiles.get());
        s.put("migrated", migrated.get());
        s.put("skipped", skipped.get());
        s.put("errors", errors.get());
        s.put("pendingInCurrentRun", totalFiles.get() - migrated.get() - skipped.get() - errors.get());
        s.put("currentSchema", currentSchema);
        s.put("startedAt", startedAt);
        if (completedAt != null) s.put("completedAt", completedAt);
        if (lastError != null) s.put("lastError", lastError);
        return s;
    }

    public Map<String, Object> startMigration() {
        if (running) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("message", "Migration already in progress");
            r.putAll(getStatus());
            return r;
        }
        running = true;
        completed = false;
        totalFiles.set(0);
        migrated.set(0);
        skipped.set(0);
        errors.set(0);
        currentSchema = "";
        startedAt     = LocalDateTime.now().toString();
        completedAt   = null;
        lastError     = null;

        new Thread(this::runMigration, "file-migration").start();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("message", "Migration started in background");
        r.put("startedAt", startedAt);
        r.put("tip", "Poll GET /master-file/admin/migrate-files/status for progress");
        return r;
    }

    private void runMigration() {
        try {
            // Set master tenant so routing datasource has a valid connection key
            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
            Set<String> masterFileIds = getPOAndIndentFileUUIDs();
            log.info("Master-schema file UUIDs (PO + Indent): {}", masterFileIds.size());

            for (String schema : schemaConfig.getSchemaList()) {
                currentSchema = schema;
                // Switch routing datasource to current schema for each iteration
                ThreadLocalStorage.setTenantName(schema);
                log.info("Scanning schema: {}", schema);

                try {
                    List<Map<String, Object>> fileRefs = jdbcTemplate.queryForList(
                        "SELECT id, fileType FROM `" + schema + "`.files WHERE data IS NOT NULL");

                    if (fileRefs.isEmpty()) {
                        log.info("No pending files in schema: {}", schema);
                        continue;
                    }

                    totalFiles.addAndGet(fileRefs.size());
                    log.info("Found {} pending files in schema: {}", fileRefs.size(), schema);

                    for (Map<String, Object> row : fileRefs) {
                        String fileId     = (String) row.get("id");
                        String fileType   = row.get("fileType") != null ? (String) row.get("fileType") : "application/octet-stream";
                        String targetBucket = masterFileIds.contains(fileId)
                                ? schemaConfig.getMasterSchema() : schema;

                        try {
                            // Re-check blob still exists (resume safety — may have been migrated already)
                            List<byte[]> dataRows = jdbcTemplate.query(
                                "SELECT data FROM `" + schema + "`.files WHERE id = ? AND data IS NOT NULL",
                                (rs, i) -> rs.getBytes("data"),
                                fileId);

                            if (dataRows.isEmpty() || dataRows.get(0) == null) {
                                skipped.incrementAndGet();
                                continue;
                            }

                            byte[] data = dataRows.get(0);

                            minIOFileStorageService.storeFile(targetBucket, fileId,
                                    new ByteArrayInputStream(data), data.length, fileType);

                            jdbcTemplate.update(
                                "UPDATE `" + schema + "`.files SET bucketName = ?, data = NULL WHERE id = ?",
                                targetBucket, fileId);

                            migrated.incrementAndGet();
                            log.info("Migrated {} → bucket:{}", fileId, targetBucket);

                        } catch (Exception e) {
                            lastError = fileId + " [" + schema + "]: " + e.getMessage();
                            log.error("Failed file {} in {}: {}", fileId, schema, e.getMessage());
                            errors.incrementAndGet();
                        }
                    }

                } catch (Exception e) {
                    log.error("Error scanning schema {}: {}", schema, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Migration thread failed: {}", e.getMessage(), e);
            lastError = e.getMessage();
        } finally {
            ThreadLocalStorage.setTenantName(null);
            running       = false;
            completed     = true;
            completedAt   = LocalDateTime.now().toString();
            currentSchema = "";
            log.info("Migration finished — migrated={}, skipped={}, errors={}",
                    migrated.get(), skipped.get(), errors.get());
        }
    }

    private Set<String> getPOAndIndentFileUUIDs() {
        String master = schemaConfig.getMasterSchema();
        Set<String> uuids = new HashSet<>();
        try {
            jdbcTemplate.queryForList(
                "SELECT fi.fileuuid FROM `" + master + "`.file_information fi " +
                "JOIN `" + master + "`.po_fileinformation pof ON pof.file_information_id = fi.id")
                .forEach(row -> uuids.add((String) row.get("fileuuid")));

            jdbcTemplate.queryForList(
                "SELECT fi.fileuuid FROM `" + master + "`.file_information fi " +
                "JOIN `" + master + "`.indent_fileinformation inf ON inf.file_information_id = fi.id")
                .forEach(row -> uuids.add((String) row.get("fileuuid")));

            log.info("Total master-schema file UUIDs: {}", uuids.size());
        } catch (Exception e) {
            log.error("Error fetching PO/Indent file UUIDs: {}", e.getMessage());
        }
        return uuids;
    }
}
