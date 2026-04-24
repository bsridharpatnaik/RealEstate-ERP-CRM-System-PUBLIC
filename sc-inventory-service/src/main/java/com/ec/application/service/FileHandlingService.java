package com.ec.application.service;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.ec.application.model.DBFile;
import com.ec.application.model.FileInformation;

@Service
@Transactional
public class FileHandlingService {
    @Autowired
    DBFileStorageService dbFileStorageService;

    Logger log = LoggerFactory.getLogger(FileHandlingService.class);

    public FileInformation uploadDoc(MultipartFile file) throws Exception {
        try {
            if (file == null || file.isEmpty()) {
                throw new Exception("File is empty or invalid");
            }

            String fileName = file.getOriginalFilename();
            if (!isValidFileName(fileName)) {
                throw new Exception("Invalid filename. Please use only letters, numbers, spaces, and basic punctuation (. - _)");
            }

            FileInformation fileUploadSuccessData = new FileInformation();
            DBFile dbFile = dbFileStorageService.storeFile(file);
            fileUploadSuccessData.setFileUUId(dbFile.getId());
            fileUploadSuccessData.setFileName(dbFile.getFileName());
            return fileUploadSuccessData;

        } catch (MaxUploadSizeExceededException e) {
            log.error("File size exceeded", e);
            throw new Exception("File size too large. Max allowed size - 15 MB");
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("1366")) {
                throw new Exception("Invalid filename. Please use only letters, numbers, spaces, and basic punctuation (. - _)");
            }
            throw new Exception(e.getMessage());
        }
    }

    public ResponseEntity<Resource> downloadFile(String fileId) throws Exception {
        try {
            DBFile dbFile = dbFileStorageService.getFile(fileId);
            String rawName = dbFile.getFileName() != null ? dbFile.getFileName() : "download";
            String encodedName = URLEncoder.encode(rawName, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");

            InputStream stream = dbFileStorageService.getFileStream(fileId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(dbFile.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename*=UTF-8''" + encodedName)
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            log.error("Error downloading file", e);
            throw new Exception("Error downloading file");
        }
    }

    private boolean isValidFileName(String fileName) {
        if (fileName == null) {
            return false;
        }

        boolean containsNonAscii = !Normalizer.normalize(fileName, Normalizer.Form.NFD)
                .matches("\\A\\p{ASCII}*\\z");

        boolean containsSpecialChars = !fileName.matches("[a-zA-Z0-9\\s._-]+");

        if (containsNonAscii || containsSpecialChars) {
            log.debug("Invalid filename detected: {}", fileName);
            return false;
        }

        return true;
    }
}
