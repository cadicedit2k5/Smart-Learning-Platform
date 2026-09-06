package com.smartlearning.storage.dto;

public record FileUploadResponse(
        String objectName,
        String originalFileName,
        String contentType,
        long size
) {
}
