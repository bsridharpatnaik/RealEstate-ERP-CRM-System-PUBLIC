package com.ec.application.service;

import io.minio.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MinIOFileStorageService {

    @Autowired
    private MinioClient minioClient;

    Logger log = LoggerFactory.getLogger(MinIOFileStorageService.class);

    public void storeFile(String bucket, String objectId, InputStream data, long size, String contentType) throws Exception {
        ensureBucketExists(bucket);
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectId)
                .stream(data, size, -1)
                .contentType(contentType)
                .build());
    }

    public InputStream getFileStream(String bucket, String objectId) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectId)
                .build());
    }

    public byte[] getFileBytes(String bucket, String objectId) throws Exception {
        try (InputStream stream = getFileStream(bucket, objectId)) {
            return IOUtils.toByteArray(stream);
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket: {}", bucket);
        }
    }
}
