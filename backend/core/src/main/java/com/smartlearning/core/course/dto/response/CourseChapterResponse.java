package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CourseChapterResponse(
        UUID id,
        UUID courseId,
        String title,
        String description,
        String learningObjectives,
        Integer orderIndex,
        Instant createdAt,
        Instant updatedAt
) {
}
