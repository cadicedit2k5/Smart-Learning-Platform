package com.smartlearning.core.course.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        UUID courseId,
        UUID authorId,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
