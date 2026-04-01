package com.ec.application.controller;

import com.ec.application.aspects.CheckAuthority;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.FileInformation;
import com.ec.application.service.FileHandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * File upload/download endpoint that always operates in the master schema.
 * Used for attachments that belong to master-schema entities (e.g. Purchase Order
 * line-item sample images).
 *
 * The "/master-file" path is excluded from tenant-id validation in
 * TenantNameInterceptor — the interceptor automatically sets the master schema,
 * so no tenant-id header is required by the caller.
 */
@RestController
@RequestMapping("/master-file")
@UseDefaultTenant
public class MasterFileHandlingController {

    @Autowired
    FileHandlingService fileHandlingService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @CheckAuthority
    public FileInformation uploadDoc(@RequestParam("file") MultipartFile file) throws Exception {
        return fileHandlingService.uploadDoc(file);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) throws Exception {
        return fileHandlingService.downloadFile(fileId);
    }
}
