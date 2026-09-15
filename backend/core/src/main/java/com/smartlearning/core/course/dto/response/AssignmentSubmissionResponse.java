package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.AssignmentSubmissionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssignmentSubmissionResponse(
        UUID id,
        UUID assignmentId,
        UUID studentId,
        String content,
        AssignmentSubmissionStatus status,
        Instant submittedAt,
        boolean late,

        String originalFileName,
        String attachmentUrl,

        BigDecimal score,
        String feedback,
        Instant gradedAt,
        UUID gradedBy
) {
}
