package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AccessCodeResponse (
    UUID id,
    UUID courseId,
    String codeHint,
    Instant expiresAt,
    Boolean active,
    Instant revokedAt,
    UUID createdBy,
    Instant createdAt
){}
