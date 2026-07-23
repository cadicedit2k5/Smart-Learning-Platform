package com.smartlearning.storage.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.storage.config.MinioProperties;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.dto.PresignedUrlResponse;
import com.smartlearning.storage.dto.StoredFile;
import com.smartlearning.storage.error.StorageErrorCode;
import com.smartlearning.storage.service.FileStorageService;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {
    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public FileUploadResponse upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(StorageErrorCode.EMPTY_FILE);
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null
                        ? "file"
                        : file.getOriginalFilename()
        );
        String objectName = buildObjectName(folder, originalName);
        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType();

        try {
            ensureBucketExists();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.bucket())
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1L)
                        .contentType(contentType)
                        .build());
            }
            return new FileUploadResponse(
                    objectName,
                    originalName,
                    contentType,
                    file.getSize()
            );
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw storageFailure("Không thể upload file", exception);
        }
    }

    @Override
    public StoredFile download(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectName)
                            .build()
            );
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectName)
                            .build()
            );
            String contentType = stat.contentType() == null
                    ? "application/octet-stream"
                    : stat.contentType();
            return new StoredFile(
                    stream,
                    contentType,
                    stat.size(),
                    extractFileName(objectName)
            );
        } catch (ErrorResponseException exception) {
            throw mapMinioError(exception);
        } catch (Exception exception) {
            throw storageFailure("Không thể tải file", exception);
        }
    }

    @Override
    public PresignedUrlResponse createDownloadUrl(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectName)
                    .build());

            int expiry = Math.toIntExact(
                    properties.presignedUrlExpiry().toSeconds()
            );
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(properties.bucket())
                            .object(objectName)
                            .expiry(expiry)
                            .build()
            );
            return new PresignedUrlResponse(
                    url,
                    Instant.now().plusSeconds(expiry)
            );
        } catch (ErrorResponseException exception) {
            throw mapMinioError(exception);
        } catch (Exception exception) {
            throw storageFailure("Không thể tạo URL tải file", exception);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectName)
                    .build());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectName)
                    .build());
        } catch (ErrorResponseException exception) {
            throw mapMinioError(exception);
        } catch (Exception exception) {
            throw storageFailure("Không thể xóa file", exception);
        }
    }

    private synchronized void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(properties.bucket())
                        .build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
    }

    private String buildObjectName(String folder, String originalName) {
        String safeFolder = folder == null ? "files" : folder
                .replace('\\', '/')
                .replaceAll("[^a-zA-Z0-9/_-]", "-")
                .replaceAll("^/+|/+$", "");
        if (safeFolder.isBlank()) {
            safeFolder = "files";
        }
        String safeName = originalName.replaceAll(
                "[^a-zA-Z0-9._-]",
                "-"
        );
        return safeFolder + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private String extractFileName(String objectName) {
        String name = objectName.substring(objectName.lastIndexOf('/') + 1);
        return name.replaceFirst("^[0-9a-fA-F-]{36}-", "");
    }

    private ApplicationException mapMinioError(
            ErrorResponseException exception
    ) {
        String code = exception.errorResponse().code();
        if ("NoSuchKey".equals(code)
                || "NoSuchObject".equals(code)
                || "NoSuchBucket".equals(code)) {
            return new ApplicationException(StorageErrorCode.OBJECT_NOT_FOUND);
        }
        return storageFailure("MinIO trả về lỗi: " + code, exception);
    }

    private ApplicationException storageFailure(
            String message,
            Exception cause
    ) {
        return new ApplicationException(
                StorageErrorCode.OPERATION_FAILED,
                message,
                cause
        );
    }
}