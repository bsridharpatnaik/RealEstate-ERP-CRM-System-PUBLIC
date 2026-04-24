package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class FileMigrationService {

    Logger log = LoggerFactory.getLogger(FileMigrationService.class);

    @Autowired
    MinIOFileStorageService minIOFileStorageService;

    @Autowired
    SchemaConfig schemaConfig;

    @PersistenceContext
    EntityManager entityManager;

    private Set<String> getPOAndIndentFileUUIDs() {
        String master = schemaConfig.getMasterSchema();
        Set<String> uuids = new HashSet<>();
        try {
            List<?> poFiles = entityManager.createNativeQuery(
                "SELECT file_uuid_id FROM " + master + ".po_fileinformation").getResultList();
            List<?> indentFiles = entityManager.createNativeQuery(
                "SELECT file_uuid_id FROM " + master + ".indent_fileinformation").getResultList();
            for (Object o : poFiles) uuids.add((String) o);
            for (Object o : indentFiles) uuids.add((String) o);
        } catch (Exception e) {
            log.error("Error fetching PO/Indent file UUIDs: {}", e.getMessage());
        }
        return uuids;
    }

    @Transactional
    public Map<String, Object> migrateAll() {
        Set<String> masterFileIds = getPOAndIndentFileUUIDs();
        log.info("Found {} file UUIDs linked to POs/Indents — will migrate to masterschema bucket", masterFileIds.size());

        int migrated = 0, skipped = 0, errors = 0;

        for (String schema : schemaConfig.getSchemaList()) {
            log.info("Processing schema: {}", schema);
            try {
                List<Object[]> rows = entityManager.createNativeQuery(
                    "SELECT id, file_name, file_type, data FROM " + schema + ".files WHERE data IS NOT NULL")
                    .getResultList();

                for (Object[] row : rows) {
                    String fileId   = (String) row[0];
                    String fileType = row[2] != null ? (String) row[2] : "application/octet-stream";
                    byte[] data     = (byte[]) row[3];

                    if (data == null || data.length == 0) {
                        skipped++;
                        continue;
                    }

                    String targetBucket = masterFileIds.contains(fileId)
                            ? schemaConfig.getMasterSchema()
                            : schema;

                    try {
                        minIOFileStorageService.storeFile(targetBucket, fileId,
                                new ByteArrayInputStream(data), data.length, fileType);

                        entityManager.createNativeQuery(
                            "UPDATE " + schema + ".files SET bucket_name = ?1, data = NULL WHERE id = ?2")
                            .setParameter(1, targetBucket)
                            .setParameter(2, fileId)
                            .executeUpdate();

                        log.info("Migrated file {} from schema {} to bucket {}", fileId, schema, targetBucket);
                        migrated++;
                    } catch (Exception e) {
                        log.error("Failed to migrate file {} from schema {}: {}", fileId, schema, e.getMessage());
                        errors++;
                    }
                }
            } catch (Exception e) {
                log.error("Error processing schema {}: {}", schema, e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("migrated", migrated);
        result.put("skipped", skipped);
        result.put("errors", errors);
        log.info("Migration complete — migrated={}, skipped={}, errors={}", migrated, skipped, errors);
        return result;
    }
}
