package com.smartlearning.core.course.dto.response;

import com.smartlearning.core.course.entity.enums.CourseContentStatus;

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
        CourseContentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
