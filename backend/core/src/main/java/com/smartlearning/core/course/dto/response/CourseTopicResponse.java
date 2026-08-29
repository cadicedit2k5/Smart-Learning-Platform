package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CourseTopicResponse(
        UUID id,
        UUID courseId,
        UUID chapterId,
        String title,
        String description,
        Integer orderIndex,
        Integer estimatedMinutes,
        Instant createdAt,
        Instant updatedAt
) {
}
