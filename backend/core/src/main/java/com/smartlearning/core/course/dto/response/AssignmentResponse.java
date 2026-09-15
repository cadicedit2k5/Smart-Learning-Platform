package com.smartlearning.core.course.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID courseId,
        String title,
        String description,
        Instant dueAt,
        BigDecimal maxScore,
        UUID createdBy,
        boolean expired,
        Instant createdAt,
        Instant updatedAt
) {
}