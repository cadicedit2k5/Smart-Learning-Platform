package com.smartlearning.core.document.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String title,
        String description,
        UUID uploadedBy,
        DocumentVersionResponse version,
        Instant createdAt,
        Instant updatedAt
) {
}
