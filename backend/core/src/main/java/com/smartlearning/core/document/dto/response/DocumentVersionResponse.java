package com.smartlearning.core.document.dto.response;

import com.smartlearning.core.document.entity.enums.DocumentProcessingStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID id,
        Integer versionNumber,
        String fileName,
        Long fileSize,
        String mimeType,
        String checksumSha256,
        DocumentProcessingStatus processingStatus,
        UUID uploadedBy,
        Instant createdAt
) {
}