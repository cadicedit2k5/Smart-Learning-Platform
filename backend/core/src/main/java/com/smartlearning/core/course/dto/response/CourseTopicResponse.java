package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CourseTopicResponse(
        UUID id,
        UUID courseId,
        UUID chapterId,
        String title,
        String description,
        Integer orderIndex,
        Integer estimatedMinutes,
        Map<String, Object> content,
        Instant createdAt,
        Instant updatedAt
) {
}
