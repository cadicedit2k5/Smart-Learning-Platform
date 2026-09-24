package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.AssignmentStatus;

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
        AssignmentStatus status,
        Instant publishedAt,
        Instant closedAt,
        boolean expired,
        Instant createdAt,
        Instant updatedAt
) {
}