package com.ec.application.service;

import com.ec.application.model.DBFile;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.DBFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@Transactional
public class DBFileStorageService {

    @Autowired
    private DBFileRepository dbFileRepository;

    @Autowired
    private MinIOFileStorageService minIOFileStorageService;

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
        return dbFileRepository.findById(fileId)
                .orElseThrow(() -> new Exception("File not found with id " + fileId));
    }

    public InputStream getFileStream(String fileId) throws Exception {
        DBFile dbFile = getFile(fileId);
        return minIOFileStorageService.getFileStream(dbFile.getBucketName(), fileId);
    }

    public byte[] getFileBytes(String fileId) throws Exception {
        DBFile dbFile = getFile(fileId);
        return minIOFileStorageService.getFileBytes(dbFile.getBucketName(), fileId);
    }
}
