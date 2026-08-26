package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.CourseContentStatus;

import java.time.Instant;
import java.util.UUID;

public record CourseChapterResponse(
        UUID id,
        UUID courseId,
        String title,
        String description,
        String learningObjectives,
        Integer orderIndex,
        CourseContentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
