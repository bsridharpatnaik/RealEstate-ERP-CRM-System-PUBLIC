package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.DBFile;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.DBFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@Service
@Transactional
public class DBFileStorageService {

    @Autowired
    private DBFileRepository dbFileRepository;

    @Autowired
    private MinIOFileStorageService minIOFileStorageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaConfig schemaConfig;

    Logger log = LoggerFactory.getLogger(DBFileStorageService.class);

    public DBFile storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (fileName.contains("..")) {
                throw new Exception("Invalid filename");
            }
            String bucket = ThreadLocalStorage.getTenantName();
            DBFile dbFile = new DBFile(fileName, file.getContentType(), bucket);
            dbFileRepository.save(dbFile);

            byte[] bytes = file.getBytes();
            minIOFileStorageService.storeFile(bucket, dbFile.getId(),
                    new ByteArrayInputStream(bytes), bytes.length, file.getContentType());

            return dbFile;
        } catch (Exception ex) {
            log.error("Error storing file: {}", ex.getMessage());
        }
        return null;
    }

    public DBFile getFile(String fileId) throws Exception {
        // Try current schema first
        DBFile file = dbFileRepository.findById(fileId).orElse(null);
        if (file != null) return file;

        // Fallback: search masterschema (PO/Indent files are stored there)
        String master = schemaConfig.getMasterSchema();
        List<DBFile> masterFiles = jdbcTemplate.query(
            "SELECT id, fileName, fileType, bucketName FROM `" + master + "`.files WHERE id = ?",
            (rs, i) -> {
                DBFile f = new DBFile();
                f.setId(rs.getString("id"));
                f.setFileName(rs.getString("fileName"));
                f.setFileType(rs.getString("fileType"));
                f.setBucketName(rs.getString("bucketName"));
                return f;
            }, fileId);

        if (!masterFiles.isEmpty()) return masterFiles.get(0);

        throw new Exception("File not found with id " + fileId);
    }

    public InputStream getFileStream(String fileId) throws Exception {
        DBFile dbFile = getFile(fileId);

        // Post-migration: serve from MinIO
        if (dbFile.getBucketName() != null) {
            return minIOFileStorageService.getFileStream(dbFile.getBucketName(), fileId);
        }

        // Pre-migration fallback: bucketName not set yet, read blob from DB
        log.warn("File {} has no bucketName — serving from DB blob (not yet migrated)", fileId);
        byte[] data = getBlobFromDB(fileId);
        if (data != null) return new ByteArrayInputStream(data);

        throw new Exception("File data not available for id " + fileId);
    }

    public byte[] getFileBytes(String fileId) throws Exception {
        DBFile dbFile = getFile(fileId);

        // Post-migration: serve from MinIO
        if (dbFile.getBucketName() != null) {
            return minIOFileStorageService.getFileBytes(dbFile.getBucketName(), fileId);
        }

        // Pre-migration fallback
        log.warn("File {} has no bucketName — serving from DB blob (not yet migrated)", fileId);
        byte[] data = getBlobFromDB(fileId);
        if (data != null) return data;

        throw new Exception("File data not available for id " + fileId);
    }

    // Searches current schema then masterschema for the raw blob
    private byte[] getBlobFromDB(String fileId) {
        String master = schemaConfig.getMasterSchema();
        String currentSchema = ThreadLocalStorage.getTenantName();

        // Try current schema
        if (currentSchema != null) {
            List<byte[]> rows = jdbcTemplate.query(
                "SELECT data FROM `" + currentSchema + "`.files WHERE id = ? AND data IS NOT NULL",
                (rs, i) -> rs.getBytes("data"), fileId);
            if (!rows.isEmpty()) return rows.get(0);
        }

        // Try masterschema
        List<byte[]> masterRows = jdbcTemplate.query(
            "SELECT data FROM `" + master + "`.files WHERE id = ? AND data IS NOT NULL",
            (rs, i) -> rs.getBytes("data"), fileId);
        if (!masterRows.isEmpty()) return masterRows.get(0);

        return null;
    }
}
