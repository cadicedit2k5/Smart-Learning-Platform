package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AccessCodeCreatedResponse(
        UUID id,
        UUID courseId,
        String code,
        Integer maxUses,
        Integer usedCount,
        Instant expiresAt,
        Boolean active,
        Instant createdAt
) {
}