package com.smartlearning.core.document.dto.response;

import com.smartlearning.core.document.entity.enums.DocumentLifecycleStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID courseId,
        UUID chapterId,
        UUID topicId,
        String title,
        String description,
        DocumentLifecycleStatus lifecycleStatus,
        UUID uploadedBy,
        DocumentVersionResponse version,
        Instant createdAt,
        Instant updatedAt
) {
}